'use strict';
const crypto=require('node:crypto');
const fs=require('node:fs');
const {Rcon}=require('rcon-client');
const clean=s=>String(s).replace(/§./g,'').replace(/\x1b\[[\d;]*m/g,'').trim();
class Obserwator {
  constructor(bot,rcon,name){this.bot=bot;this.rcon=rcon;this.name=name;this.events=[];this.attrs=new Map();this.items=new Map();this.start=Date.now();this.logStart=fs.existsSync('/clone-logs/latest.log')?fs.statSync('/clone-logs/latest.log').size:0;this.capture();}
  static async connect(){return Rcon.connect({host:process.env.MC_HOST||'zaklecia-gracz',port:Number(process.env.RCON_PORT||25575),password:process.env.RCON_PASSWORD||'zakgracz'});}
  capture(){
    const c=this.bot._client;
    for(const packet of ['set_slot','window_items','open_window','close_window','held_item_slot','entity_equipment','update_attributes','sound_effect','named_sound_effect','player_chat','system_chat','action_bar','boss_bar','entity_metadata','spawn_entity','entity_destroy'])c.on(packet,p=>{
      this.events.push({at:Date.now(),packet,data:p});if(this.events.length>10000)this.events.shift();
      if(packet==='set_slot')this.items.set(p.slot,p.item);
      if(packet==='window_items'&&Array.isArray(p.items))p.items.forEach((v,i)=>this.items.set(i,v));
      if(packet==='update_attributes'&&(p.entityId===this.bot.entity?.id||p.entityId===this.bot.entity?.entityId))for(const a of p.properties||p.attributes||[])this.attrs.set(a.key||a.name,{base:a.value??a.baseValue,modifiers:a.modifiers||[]});
    });
    this.bot.on('messagestr',s=>this.events.push({at:Date.now(),packet:'chat',data:s}));
  }
  components(item){
    if(!item)return {};
    // Prismarine trzyma dodatnie i usunięte komponenty osobno. Szukanie w
    // całym pakiecie błędnie uznawało unsetData(CONSUMABLE) za używalny item.
    let s='';try{s=JSON.stringify(item.components||item.componentData||[]);}catch{}
    const has=x=>s.includes(x);
    return {consumable:has('consumable'),attack_range:has('attack_range'),piercing_weapon:has('piercing_weapon'),kinetic_weapon:has('kinetic_weapon'),blocks_attacks:has('blocks_attacks'),use_duration:has('use_duration')};
  }
  attackRange(item){
    if(!item)return null;
    let s='';try{s=JSON.stringify(item.components||[]);}catch{return null;}
    if(!s.includes('attack_range'))return null;
    const n=key=>Number(s.match(new RegExp(`"${key}"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)`))?.[1]);
    const min=Number.isFinite(n('min_reach'))?n('min_reach'):n('minReach');
    const max=Number.isFinite(n('max_reach'))?n('max_reach'):n('maxReach');
    return Number.isFinite(min)&&Number.isFinite(max)?{min,max}:null;
  }
  attribute(name){for(const [k,v] of this.attrs)if(k.endsWith(name.split(':').pop())){
    if(typeof v==='number')return v;
    const base=Number(v.base);let value=base;
    for(const m of v.modifiers)if(m.operation===0||m.operation==='add_value'||m.operation==='add')value+=Number(m.amount??m.value??0);
    for(const m of v.modifiers)if(m.operation===1||m.operation==='add_multiplied_base')value+=base*Number(m.amount??m.value??0);
    for(const m of v.modifiers)if(m.operation===2||m.operation==='add_multiplied_total')value*=1+Number(m.amount??m.value??0);
    return value;
  }return null;}
  async cmd(s){return clean(await this.rcon.send(s));}
  async placeholder(hand){return this.cmd(`papi parse ${this.name} %zaklecia_${hand}%`);}
  async mana(){const s=await this.cmd(`papi parse ${this.name} %mmocore_mana%`);const n=Number(s.replace(',','.'));return Number.isFinite(n)?n:null;}
  async maxMana(){const s=await this.cmd(`papi parse ${this.name} %mmocore_max_mana%`);const n=Number(s.replace(',','.'));return Number.isFinite(n)?n:null;}
  async characterClass(){return this.cmd(`papi parse ${this.name} %mmocore_class%`);}
  async stamina(){const s=await this.cmd(`papi parse ${this.name} %mmocore_stamina%`);const n=Number(s.replace(',','.'));return Number.isFinite(n)?n:null;}
  async hp(target=this.name){const s=await this.cmd(`data get entity ${target} Health`);return Number(s.match(/(-?\d+(?:\.\d+)?)[fd]?$/)?.[1]);}
  async speed(){const s=await this.cmd(`minecraft:attribute ${this.name} minecraft:movement_speed get`);const n=Number(s.match(/(-?\d+(?:\.\d+)?)\s*$/)?.[1]);return Number.isFinite(n)?n:null;}
  async selected(){return this.cmd(`data get entity ${this.name} SelectedItem`);}
  snapshot(){return {at:Date.now(),index:this.events.length,hash:this.hash()};}
  hash(){const slots=this.bot.inventory.slots.map(x=>x?{type:x.type,count:x.count,nbt:x.nbt,components:x.components,metadata:x.metadata}:null);return crypto.createHash('sha256').update(JSON.stringify(slots,(_,v)=>typeof v==='bigint'?String(v):v)).digest('hex');}
  metrics(from){const e=this.events.slice(from.index),count=p=>e.filter(x=>x.packet===p).length;
    const first=e.find(x=>['spawn_entity','entity_metadata','sound_effect','named_sound_effect','action_bar','open_window','close_window'].includes(x.packet));
    return {latencyTicks:first?Math.round((first.at-from.at)/50):null,setSlotPerSec:Number(((count('set_slot')+count('entity_equipment'))/Math.max(.05,(Date.now()-from.at)/1000)).toFixed(1)),bossBars:count('boss_bar'),messages:count('chat')+count('player_chat')+count('system_chat')+count('action_bar'),sounds:count('sound_effect')+count('named_sound_effect'),inventoryChanged:this.hash()!==from.hash};}
  async entities(type='minecraft:husk'){return this.cmd(`execute as @e[type=${type},distance=..24] run data get entity @s UUID`);}
  logErrors(){try{
    const norm=x=>x.replace(/^\[[^\]]+\]\s*/, '');
    const base=new Set(fs.readFileSync('/baseline.log','utf8').split('\n').map(norm));
    const file='/clone-logs/latest.log',size=fs.statSync(file).size,start=Math.min(size,this.logStart),len=Math.min(size-start,5_000_000);
    const b=Buffer.alloc(len),fd=fs.openSync(file,'r');try{fs.readSync(fd,b,0,len,size-len);}finally{fs.closeSync(fd);}
    return b.toString('utf8').split('\n')
      .filter(x=>/ERROR\]|Exception|rzucanie przerwane/.test(x))
      .filter(x=>!base.has(norm(x))&&!/CHW|Chowance|Wiary|MMOProfiles/.test(x));
  }catch{return null;}}
}
module.exports={Obserwator,clean};
