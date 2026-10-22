'use strict';
const mineflayer = require('mineflayer');
const nbt = require('prismarine-nbt');
const sleep = ms => new Promise(r => setTimeout(r, ms));
const bot = mineflayer.createBot({host:'ui-klon',port:25565,username:'UiQA',version:'1.21.11',auth:'offline'});
setTimeout(() => { console.error('TIMEOUT 160s'); bot.quit(); process.exit(2); }, 160000);
const pages = [];
let dialog = null;
const fails = [];
bot._client.on('add_resource_pack', p => {
  bot._client.write('resource_pack_receive',{uuid:p.uuid,result:3});
  bot._client.write('resource_pack_receive',{uuid:p.uuid,result:0});
});
bot._client.on('show_dialog', p => {
  dialog = p.dialog && p.dialog.data ? nbt.simplify(p.dialog.data) : p.dialog;
});
bot._client.on('clear_dialog', () => { dialog = null; });
bot.on('error', e => { console.error('BOT_ERROR',e.stack||e); process.exit(2); });
bot.on('kicked', e => { console.error('KICKED',JSON.stringify(e)); process.exit(2); });
function plain(x) {
  if (x==null) return '';
  if (typeof x==='string') return x;
  if (Array.isArray(x)) return x.map(plain).join('');
  if (typeof x!=='object') return String(x);
  if (x.type!==undefined && x.value!==undefined && x.text===undefined) return plain(x.value);
  return plain(x.text??x['']??'')+plain(x.extra||[]);
}
function buttons(d) {
  return [...(d.actions||[]),d.exit_action].filter(Boolean).map(b=>({label:plain(b.label),id:b.action?.id,payload:b.action?.payload??b.action?.additions}));
}
function vi(n) { const a=[]; let v=n>>>0; for(;v>127;v>>>=7)a.push((v&127)|128);a.push(v);return Buffer.from(a); }
function ns(s) {const b=Buffer.from(s,'utf8'),l=Buffer.alloc(2);l.writeUInt16BE(b.length);return Buffer.concat([l,b]);}
function payload(v) {
  if(typeof v==='string') return [8,ns(v)];
  if(typeof v==='number'&&Number.isInteger(v)){const b=Buffer.alloc(4);b.writeInt32BE(v);return [3,b];}
  if(typeof v==='number'){const b=Buffer.alloc(8);b.writeDoubleBE(v);return [6,b];}
  if(Array.isArray(v)){const b=Buffer.alloc(4+4*v.length);b.writeInt32BE(v.length);v.forEach((x,i)=>b.writeInt32BE(x,4+4*i));return [11,b];}
  if(v&&typeof v==='object') return [10,body(v)];
  return [1,Buffer.from([0])];
}
function body(v){const a=[];for(const k of Object.keys(v)){const [t,b]=payload(v[k]);a.push(Buffer.from([t]),ns(k),b);}a.push(Buffer.from([0]));return Buffer.concat(a);}
function clickDialog(b) {
  const nb=b.payload===undefined?Buffer.from([0]):Buffer.concat([Buffer.from([10]),body(b.payload)]);
  const id=Buffer.from(b.id,'utf8');
  bot._client.writeRaw(Buffer.concat([vi(0x41),vi(id.length),id,vi(nb.length),nb]));
  dialog=null;
}
async function waitFor(fn,ms=15000){const until=Date.now()+ms;while(Date.now()<until){const x=fn();if(x)return x;await sleep(150);}return null;}
async function selectProfile(){
  if(!await waitFor(()=>bot.game?.gameMode==='spectator'||bot.game?.gameMode===3,30000)) throw Error('No spectator scene');
  const d=await waitFor(()=>{
    if(dialog&&/Twoje postacie/i.test(plain(dialog.title))) return dialog;
    const ent=Object.values(bot.entities).filter(e=>e.name==='interaction'&&e.position&&bot.entity?.position&&e.position.distanceTo(bot.entity.position)<4).sort((a,b)=>a.position.distanceTo(bot.entity.position)-b.position.distanceTo(bot.entity.position))[0];
    if(ent)bot.activateEntity(ent);
    return null;
  },30000);
  if(!d) throw Error('No profile dialog');
  const b=buttons(d).find(x=>x.id&&!/Nowa postać|Zarządzaj|Wróć|Limit postaci/i.test(x.label));
  if(!b) throw Error('No existing profile '+JSON.stringify(buttons(d)));
  clickDialog(b);
  if(!await waitFor(()=>bot.game?.gameMode!=='spectator'&&bot.game?.gameMode!==3,30000)) throw Error('Profile did not load');
  await sleep(2000);
  console.log('PROFILE',b.label);
}
async function nextWindow(old,ms=4000){return waitFor(()=>bot.currentWindow&&bot.currentWindow!==old?bot.currentWindow:null,ms);}
async function open(id){if(bot.currentWindow){bot.closeWindow(bot.currentWindow);await sleep(180);}const old=bot.currentWindow;bot.chat('/dm open '+id);return nextWindow(old);}
(async()=>{
  await waitFor(()=>bot.entity,45000);
  await selectProfile();
  const social=await open('hub_spolecznosc');
  if(!social) throw Error('No social menu');
  await bot.clickWindow(5,0,0);
  const creation=await nextWindow(social,5000);
  console.log('CREATION',!!creation,creation&&JSON.stringify(creation.title).slice(0,100));
  if(!creation) throw Error('No party creation');
  await bot.clickWindow(23,0,0);
  const view=await nextWindow(creation,5000);
  console.log('VIEW',!!view,view&&JSON.stringify(view.title).slice(0,120));
  if(view){const title=JSON.stringify(view.title);const bottom=view.slots.slice(54,90);console.log('PARTY_VIEW',title.includes('䪔'),!!(bottom[0]&&bottom[1]&&bottom[10]&&bottom[11]&&bottom[12]),!!bottom[3]);}
  const before=bot.inventory.items().map(i=>[i.slot,i.name,i.count]);
  for(const [id,from,slot,glyphs] of pages){
    const menu=await open(from);
    if(!menu){console.log('FAIL',id,'no-menu');fails.push(id+':no-menu');continue;}
    try{await bot.clickWindow(slot,0,0);}catch(e){console.log('FAIL',id,'click',e.message);fails.push(id+':click');continue;}
    const w=await nextWindow(menu,3500);
    if(!w){console.log('FAIL',id,'no-page');fails.push(id+':no-page');continue;}
    await sleep(180);
    const title=JSON.stringify(w.title);
    const glyph=glyphs.some(g=>title.includes(g));
    const bottom=w.slots.slice(54,90);
    const panel=!!(bottom[0]&&bottom[1]&&bottom[10]&&bottom[11]&&bottom[12]);
    const back=!!bottom[3];
    console.log('PAGE',id,'glyph',glyph,'panel',panel,'back',back,'title',title.slice(0,70));
    if(!glyph||!panel||!back)fails.push(id+':missing');
  }
  if(bot.currentWindow){bot.closeWindow(bot.currentWindow);await sleep(250);}
  const after=bot.inventory.items().map(i=>[i.slot,i.name,i.count]);
  console.log('INVENTORY_UNCHANGED',JSON.stringify(before)===JSON.stringify(after),JSON.stringify(before),JSON.stringify(after));
  if(JSON.stringify(before)!==JSON.stringify(after))fails.push('inventory');
  console.log('RESULT',pages.length,fails.length,JSON.stringify(fails));
  bot.quit();process.exit(0);
})().catch(e=>{console.error('ERROR',e.stack||e);bot.quit();process.exit(0);});
