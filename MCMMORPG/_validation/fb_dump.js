// Funkcja-builder GUI dumper: wchodzi w /md edit, daje feather, prawy-klik w blok areny,
// i ZRZUCA kazde otwierane okno z PRAWDZIWYMI etykietami (komponenty -> tekst).
// Opcjonalnie klika slot podany w argv[3] aby zejsc do nastepnego menu.
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const HOST='127.0.0.1',PORT=25565,RPORT=25575,RPASS=process.env.RCON_PASS,USER='Builder';
const DUNGEON=process.argv[2]||'level_1';
// argv[3] = sciezka klikniec rozdzielona przecinkami, np "13,2" -> klik 13, potem 2
const PATH=process.argv[3]!==undefined?process.argv[3].split(',').map(s=>parseInt(s.trim(),10)):[];
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function waitFor(fn,to=30000,st=250){const t=Date.now();while(Date.now()-t<to){if(await fn())return true;await sleep(st);}return false;}
function txt(c){ // wyciagnij plaski tekst z komponentu czatu / stringa / nbt (defensywnie)
  if(c==null) return '';
  if(typeof c==='string'){ const t=c.trim(); if(t[0]==='{'||t[0]==='['){ try{return txt(JSON.parse(t));}catch{} } return c; }
  if(typeof c==='number'||typeof c==='boolean') return String(c);
  if(Array.isArray(c)) return c.map(txt).join('');
  if(typeof c==='object'){
    // NBT wrapper {type, value}
    if(c.type!==undefined && c.value!==undefined && c.text===undefined && c.extra===undefined) return txt(c.value);
    let s='';
    if(c.text!==undefined) s+=txt(c.text);
    if(c.translate!==undefined && !s) s+=txt(c.translate);
    if(c.extra!==undefined) s+=txt(c.extra);
    if(!s && c.value!==undefined) s+=txt(c.value);
    return s;
  }
  return '';
}
function lore(it){
  try{ const d=it.nbt&&it.nbt.value; if(!d) return [];
    // 1.21 components: minecraft:lore
    const comp=it.components&&it.components.find&&null; // skip
  }catch{}
  return [];
}
function dumpWin(win,tag){
  if(!win){console.log(`[${tag}] (brak okna)`);return;}
  console.log(`\n[${tag}] TYTUL="${txt(win.title)}" slots=${win.slots.length}`);
  win.slots.forEach((it,i)=>{ if(it){
    const name=txt(it.customName)|| (it.displayName||it.name);
    console.log(`  [${i}] ${it.name} x${it.count} | ${name}`);
  }});
}
(async()=>{
  const rcon=await Rcon.connect({host:HOST,port:RPORT,password:RPASS});
  const bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
  bot.on('resourcePack',()=>bot.acceptResourcePack());
  bot.on('kicked',r=>console.log('[KICK]',JSON.stringify(r).slice(0,200)));
  bot.on('error',()=>{}); bot.on('end',()=>{}); process.on('uncaughtException',()=>{});
  let winSeen=0;
  bot.on('windowOpen',w=>{winSeen++;dumpWin(w,'open#'+winSeen);});
  const WD=setTimeout(()=>{console.log('[FATAL] watchdog');process.exit(8);},130000);
  if(!await waitFor(()=>!!bot.entity,90000,500)){console.log('brak spawnu');process.exit(7);}
  await rcon.send(`op ${USER}`);await sleep(400);
  await rcon.send(`gamemode creative ${USER}`);await sleep(400);
  let changed=false;bot.on('spawn',()=>changed=true);changed=false;
  bot.chat(`/md edit ${DUNGEON}`);
  await waitFor(()=>changed,15000,400); await sleep(2500);
  console.log('[bot] po /md edit dim=',bot.game.dimension,'poz=',bot.entity.position);
  await rcon.send(`give ${USER} feather 1`); await sleep(700);
  try{ await bot.setQuickBarSlot(0);}catch(e){}
  const { Vec3 }=require('vec3');
  // RCON tp jest nadpisywane przez pakiety ruchu bota -> bot IDZIE sam do boss-loc (0,64,0)
  const goal=new Vec3(0,64,0.5);
  try{ await bot.lookAt(goal,true); }catch{}
  bot.setControlState('forward',true);
  for(let i=0;i<40 && bot.entity.position.distanceTo(goal)>2.0;i++){
    try{ await bot.lookAt(goal,true); }catch{}
    await sleep(250);
  }
  bot.setControlState('forward',false); await sleep(400);
  console.log('[bot] heldItem=',bot.heldItem&&bot.heldItem.name,'poz=',bot.entity.position,'dist=',bot.entity.position.distanceTo(goal).toFixed(2));
  // retry: pierko czasem nie otwiera okna za 1. razem (race) -> probuj kilka razy (jak w dzialajacej wersji: activateBlock bez lookAt)
  for(let attempt=1;attempt<=14 && !bot.currentWindow;attempt++){
    // blok pod stopami (floor=grass y63, zawsze w zasiegu); bez lookAt - to psulo pitch
    const target=bot.blockAt(bot.entity.position.offset(0,-1,0));
    console.log(`[bot] proba ${attempt} target:`,target&&target.name,'@',target&&target.position);
    try{ await bot.activateBlock(target); }catch(e){console.log('[activateBlock err]',e.message);}
    await sleep(1300);
  }
  dumpWin(bot.currentWindow,'current-after-click');
  for(const slot of PATH){
    if(!bot.currentWindow){console.log('[brak okna, przerywam sciezke]');break;}
    console.log(`\n>>> klikam slot ${slot}...`);
    try{ await bot.clickWindow(slot,0,0); }catch(e){console.log('[click err]',e.message);}
    await sleep(1800);
    dumpWin(bot.currentWindow,'after-click-'+slot);
  }
  await sleep(500);
  clearTimeout(WD); try{await rcon.end();}catch{} try{bot.quit();}catch{} process.exit(0);
})().catch(e=>{console.error('FATAL',e.message);process.exit(1);});
