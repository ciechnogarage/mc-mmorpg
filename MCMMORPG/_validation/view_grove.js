// Wolna kamera dla sceny level_1: zaklada ze swiat MV `l1view` (kopia template) jest zaladowany.
// Bot -> mvtp l1view -> spectator -> RCON tp na kolejne wygodne pozycje z obrotem celujacym w scene
// (NORMALNY swiat, wiec tp DZIALA, w przeciwienstwie do /md edit) -> prismarine-viewer + puppeteer PNG.
// Uzycie: najpierw import swiata (patrz _grove_view.sh / komendy), potem: node view_grove.js
const mineflayer=require('mineflayer'); const {Rcon}=require('rcon-client');
const {startViewer}=require('./foundation_viewer'); const puppeteer=require('puppeteer');
const fs=require('fs'); const path=require('path');
const HOST='127.0.0.1',PORT=25565,RPORT=25575,RPASS=process.env.RCON_PASS,USER='Camera',WORLD='l1view',VPORT=3000;
const SHOTS=path.join(__dirname,'shots'); const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=60000,st=300)=>{const t=Date.now();while(Date.now()-t<to){if(await fn())return true;await sleep(st);}return false;};
// MC rotacja: yaw 0=+z(pld),90=-x(zach),180=-z(pln),270=+x(wsch); pitch + = w dol.
function lookAt(cx,cy,cz,tx,ty,tz){const dx=tx-cx,dy=ty-cy,dz=tz-cz;const h=Math.hypot(dx,dz);
  let yaw=Math.atan2(-dx,dz)*180/Math.PI; let pitch=-Math.atan2(dy,h)*180/Math.PI; return {yaw,pitch};}
// Podmiana proxy (l1view to throwaway kopia MV -> template NIETKNIETY): bloki bez tekstury
// w prismarine-viewerze -> renderowalne o zblizonym vibe. moss=zielen, sculk=ciemny teal.
const PROXY = process.env.PROXY !== '0'; // domyslnie ON; PROXY=0 wylacza
const PROXY_SWAP=[
 ['moss_block','lime_concrete'],['moss_carpet','lime_carpet'],['mossy_cobblestone','green_concrete_powder'],
 ['sculk','warped_planks'],['sculk_vein','warped_planks'],['sculk_catalyst','warped_wart_block'],
 ['sculk_shrieker','warped_wart_block'],['sculk_sensor','warped_wart_block'],
];
const PROXY_BOX={x1:-46,y1:54,z1:-46,x2:46,y2:122,z2:156};
// [nazwa, camx,camy,camz, targetx,targety,targetz] — footprint: wyspa z~0, korytarz z48..152, oboz z146, komora z-42
const VIEWS=[
 // --- A2: domkniecie otoku (czy zniknela szara otchlan z linii wzroku) ---
 ['N1_topdown_all',   0,195,57,  0,64,57],    // rzut z gory na CALY build (wyspa+korytarz+oboz)
 ['N2_camp_horizon',  0,108,118, 0,70,152],   // z gory-pld na amfiteatr obozu: horyzont domkniety?
 ['N3_corridor_walls',0,96,124,  0,70,72],    // wzdluz korytarza: sciany drzew A2 po bokach osi X
 // --- A1: drzewa z konarami (geometria wierna w viewerze) ---
 ['N4_grove_closeup', 20,74,82,  -12,74,92],  // zblizenie na drzewa korytarza: konary czy lizaki?
 ['N5_island_hero',   0,82,58,   0,72,-2],    // money shot: sightline z korytarza na Drzewo-Serce
 ['N6_island_ground', 0,70,42,   0,74,-2],    // z ziemi u wejscia na wyspe: sylweta+konary
 // --- A4 baseline (jeszcze nieprzebudowane) ---
 ['N7_reward_chamber',0,66,-30,  0,61,-42],   // komora nagrod: stan przed A4
];
(async()=>{
 fs.mkdirSync(SHOTS,{recursive:true});
 const rcon=await Rcon.connect({host:HOST,port:RPORT,password:RPASS});
 const bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
 bot.on('resourcePack',()=>bot.acceptResourcePack()); bot.on('error',e=>console.log('[ERR]',e.message));
 bot._client.on('error',()=>{}); process.on('uncaughtException',e=>{if(!/PartialReadError|Read error/.test(String(e)))console.log('[UNC]',e.message);});
 const WD=setTimeout(()=>{console.log('[FATAL] wd');process.exit(8);},260000);
 if(!await waitFor(()=>!!bot.entity,60000)){console.log('[FATAL] brak spawnu');process.exit(7);}
 await rcon.send(`op ${USER}`); await sleep(500);
 let sp=0; bot.on('spawn',()=>sp++); const b0=sp;
 bot.chat(`/mv tp ${WORLD}`);
 if(!await waitFor(()=>sp>b0,15000,300)){ bot.chat(`/mvtp ${WORLD}`); await waitFor(()=>sp>b0,15000,300); }
 await sleep(1500);
 await rcon.send(`minecraft:gamemode spectator ${USER}`); await sleep(500);
 console.log('[bot] swiat=',bot.game?.dimension,'pos=',bot.entity.position.toString());
 // --- PODMIANA PROXY w l1view (throwaway kopia) przed renderem ---
 if(PROXY){
   const B=PROXY_BOX;
   await rcon.send(`minecraft:gamemode creative ${USER}`); await sleep(400); // FAWE //pos wymaga nie-spectator
   bot.chat(`//pos1 ${B.x1},${B.y1},${B.z1}`); await sleep(700);
   bot.chat(`//pos2 ${B.x2},${B.y2},${B.z2}`); await sleep(700);
   console.log('[proxy] podmieniam',PROXY_SWAP.length,'typow blokow w l1view');
   for(const [real,proxy] of PROXY_SWAP){ bot.chat(`//replace ${real} ${proxy}`); await sleep(2400); }
   console.log('[proxy] zlecone — czekam na FAWE'); await sleep(4000);
   await rcon.send(`minecraft:gamemode spectator ${USER}`); await sleep(500);
 }
 const viewer=startViewer(bot,{enabled:true,port:VPORT,firstPerson:true,viewDistance:12}); await sleep(2500);
 const browser=await puppeteer.launch({headless:'new',args:['--no-sandbox','--use-gl=swiftshader','--enable-webgl','--ignore-gpu-blocklist','--enable-unsafe-swiftshader','--window-size=1280,720']});
 const page=await browser.newPage(); await page.setViewport({width:1280,height:720});
 await page.goto(`http://127.0.0.1:${VPORT}`,{waitUntil:'domcontentloaded',timeout:30000}); await sleep(6000);
 for(const [name,cx,cy,cz,tx,ty,tz] of VIEWS){
   const {yaw,pitch}=lookAt(cx,cy,cz,tx,ty,tz);
   await rcon.send(`minecraft:gamemode spectator ${USER}`); // spectator nie trzyma sie -> re-assert (inaczej spada)
   await rcon.send(`minecraft:tp ${USER} ${cx} ${cy} ${cz} ${yaw.toFixed(1)} ${pitch.toFixed(1)}`);
   // czekaj az KLIENT bota faktycznie sie przeniesie (viewer sledzi bot.entity)
   await waitFor(()=>bot.entity && Math.abs(bot.entity.position.x-cx)<1.5 && Math.abs(bot.entity.position.y-cy)<2 && Math.abs(bot.entity.position.z-cz)<1.5, 6000, 200);
   await rcon.send(`minecraft:tp ${USER} ${cx} ${cy} ${cz} ${yaw.toFixed(1)} ${pitch.toFixed(1)}`); // 2. raz po zaladowaniu chunkow
   await sleep(1500);
   // KLUCZOWE: viewer firstPerson NIE sledzi tp RCON -> przeladuj strone, klient zainicjuje kamere
   // na biezacej pozycji/orientacji bota.
   await page.reload({waitUntil:'domcontentloaded',timeout:30000});
   await sleep(6000); // render chunkow po reloadzie
   const p=bot.entity.position;
   await page.screenshot({path:path.join(SHOTS,`${name}.png`)});
   console.log(`[shot] ${name} cel=${cx},${cy},${cz} REAL=${p.x.toFixed(1)},${p.y.toFixed(1)},${p.z.toFixed(1)} yaw=${(bot.entity.yaw*180/Math.PI).toFixed(0)} pitch=${(bot.entity.pitch*180/Math.PI).toFixed(0)}`);
 }
 await browser.close(); try{viewer.close();}catch{} try{await rcon.end();}catch{} try{bot.quit();}catch{}
 clearTimeout(WD); console.log('[done] screeny w shots/'); process.exit(0);
})().catch(e=>{console.error('FATAL',e);process.exit(1);});
