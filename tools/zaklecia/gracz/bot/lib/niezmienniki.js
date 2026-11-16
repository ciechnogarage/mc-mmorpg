'use strict';
const fs=require('node:fs');const {sleep}=require('./klient');
const SLOTS=[...Array(36).keys(),-106,100,101,102,103];
function slotKlienta(n){return n===-106?45:n>=100&&n<=103?108-n:n<9?36+n:n;}
function clientItem(bot,n){
  return bot.inventory.slots[slotKlienta(n)]||null;
}
function parseSlot(s){
  const id=s.match(/\bid:\s*"([^"]+)"/i)?.[1];
  const count=Number(s.match(/\bcount:\s*(\d+)/i)?.[1]);
  return id&&Number.isFinite(count)?{id,count}:null;
}
function visibleTitle(title){
  if(title==null)return '';
  if(typeof title==='string'){
    try{return visibleTitle(JSON.parse(title));}catch{return title;}
  }
  if(Array.isArray(title))return title.map(visibleTitle).join('');
  if(typeof title!=='object')return '';
  // Protocol chat components are NBT compounds: ignore formatting keys.
  if(title.type==='string')return typeof title.value==='string'?title.value:'';
  if(title.type==='list')return visibleTitle(title.value);
  if(title.type==='compound')return visibleTitle(title.value);
  return ['text','translate','extra'].map(key=>visibleTitle(title[key])).join('');
}
function closesBar(e){return e.data.action===1||(e.data.action===0&&!visibleTitle(e.data.title).trim());}
class Niezmienniki{
  constructor(o,k,w){this.o=o;this.k=k;this.w=w;this.rows=[];this.logOffset=o.logStart;this.lastBarClose=0;this.lastLogCount=0;this.fastInventory=false;this.fastStep=0;this.prioritySlots=[];}
  async inventory(requestedSlots=null){
    const bad=[],errors=[];let read=0;
    const step=++this.fastStep;
    const slots=requestedSlots||(this.fastInventory&&step%50!==0
      ? [...new Set([0,...this.prioritySlots,SLOTS[step%SLOTS.length]])]
      : SLOTS);
    for(const n of slots){
      const equipment={[-106]:'offhand',100:'feet',101:'legs',102:'chest',103:'head'};
      const query=equipment[n]?`equipment.${equipment[n]}`:`Inventory[{Slot:${n}b}]`;
      const raw=await this.o.cmd(`data get entity ${this.o.name} ${query}`);
      if(/Error executing:|Cannot perform command async|Unknown command/i.test(raw)){errors.push({slot:n,error:raw.slice(0,180)});continue;}
      const got=parseSlot(raw);
      const item=clientItem(this.k.bot,n);
      const expected=item?{id:`minecraft:${item.name}`,count:item.count}:null;
      if(got)read++;
      if(JSON.stringify(got)!==JSON.stringify(expected))bad.push({slot:n,server:got,client:expected});
    }
    return {bad,read,errors};
  }
  log(){
    const path='/clone-logs/latest.log';if(!fs.existsSync(path))return [];
    const size=fs.statSync(path).size,start=Math.min(this.logOffset,size),bytes=Math.min(size-start,2_000_000);
    const b=Buffer.alloc(bytes),fd=fs.openSync(path,'r');try{fs.readSync(fd,b,0,bytes,start);}finally{fs.closeSync(fd);}
    this.logOffset=size;
    const lines=b.toString('utf8').split('\n').filter(x=>/(?:\[(?:WARN|ERROR)\].*Zaklec|Exception.*Zaklec|pl\.mcmmorpg\.zaklecia)/i.test(x));
    this.lastLogCount=lines.length;return lines.slice(0,8);
  }
  async check(id){
    await sleep(100);
    const violations=[];const m=await this.o.mana();
    if(m===null)violations.push('mana: brak odczytu');else if(m<0)violations.push(`mana=${m}`);
    for(const [hand,item] of [['lewa',this.k.item('off')],['prawa',this.k.item()]]){
      const p=await this.o.placeholder(hand);
      if(p!=='pusta'&&item?.name!=='debug_stick')violations.push(`${hand}: placeholder=${p}, przedmiot=${item?.name||'pusty'}`);
    }
    // Otwarte GUI ma w protokole wlasny uklad slotow; bot.inventory.slots
    // nie jest wtedy porownywalne z 41 slotami encji gracza.
    const inv=this.k.bot.currentWindow?{bad:[],read:0,errors:[]}:await this.inventory();let inventoryTransient=[];
    if(inv.bad.length){
      await sleep(250);
      const confirmed=await this.inventory(inv.bad.map(x=>x.slot));
      inventoryTransient=inv.bad.filter(x=>!confirmed.bad.some(y=>y.slot===x.slot));
      inv.bad=confirmed.bad;inv.errors.push(...confirmed.errors);
    }
    if(inv.bad.length)violations.push(`Inventory ${inv.bad.length} rozbieżności: ${JSON.stringify(inv.bad.slice(0,5))}`);
    if(inv.errors.length)violations.push(`Inventory ${inv.errors.length} bledow odczytu: ${JSON.stringify(inv.errors.slice(0,2))}`);
    if(this.k.barClosedAt>this.lastBarClose&&!this.k.barOpen){
      this.lastBarClose=this.k.barClosedAt;
      const bars=this.o.events.filter(e=>e.packet==='boss_bar');
      const ids=new Set(bars.filter(e=>e.at>=this.k.barOpenedAt&&/✦|many\/s/.test(JSON.stringify(e.data.title||''))).map(e=>e.data.entityUUID));
      const removed=new Set(bars.filter(e=>e.at>=this.lastBarClose&&closesBar(e)).map(e=>e.data.entityUUID));
      const missing=[...ids].filter(x=>!removed.has(x));
      if(missing.length)violations.push(`BossBar: brak REMOVE po Q dla ${missing.join(',')}`);
    }
    const log=this.log();if(log.length)violations.push(`log Zaklec ${this.lastLogCount}: ${log.slice(0,2).join(' | ')}`);
    const row={step:id,at:new Date().toISOString(),mana:m,windowOpen:!!this.k.bot.currentWindow,
      windowType:this.k.bot.currentWindow?.type||null,screen:this.k.screen,
      inventoryRead:inv.read,inventoryDifferences:inv.bad,inventoryTransient,inventoryErrors:inv.errors,logCount:this.lastLogCount,log,violations};
    this.rows.push(row);if(violations.length){this.w.add(`INV-${id}`,false,violations.join('; '));console.log('NIEZMIENNIK '+JSON.stringify(row));}
    return row;
  }
  async cleanup(){
    const s=await this.o.cmd('minecraft:execute in minecraft:overworld positioned 0 80 0 if entity @e[type=!minecraft:player,distance=..24]');
    const count=Number(s.match(/Count:\s*(\d+)/i)?.[1]||0);
    const readOK=/Test failed|Test passed|Count:\s*\d+/i.test(s),violations=[];
    if(!readOK)violations.push(`nie mozna odczytac encji po sprzataniu: ${s.slice(0,180)}`);
    if(count)violations.push(`pozostalo ${count} encji w arenie`);
    const row={step:'cleanup',at:new Date().toISOString(),entities:readOK?count:null,raw:s,violations};
    this.rows.push(row);if(violations.length)this.w.add('INV-cleanup',false,violations.join('; '));return row;
  }
}
module.exports={Niezmienniki,parseSlot,slotKlienta,clientItem,visibleTitle,closesBar};
