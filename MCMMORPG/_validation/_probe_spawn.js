// SONDA (POPRAWIONA wg protokolu pamieci): czy level_1_grove_guardian spawnuje sie server-side w SWIEZEJ instancji.
// Wymogi: instancje level_1_* wyczyszczone PRZED startem serwera; bot startuje z overworld; forceload chunku bossa.
const mineflayer=require('mineflayer');const {Rcon}=require('rcon-client');
const H='127.0.0.1',P=25565,RP=25575,RPASS=process.env.RCON_PASS,U='FableProbe';
const DIFF=process.argv[2]||'EASY';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const wf=async(f,to=30000,st=400)=>{const t=Date.now();while(Date.now()-t<to){const v=await f();if(v)return v;await sleep(st);}return false;};
(async()=>{
 const rcon=await Rcon.connect({host:H,port:RP,password:RPASS});
 const send=async c=>(await rcon.send(c)).replace(/§[0-9a-fklmnorx]/g,'');
 const bot=mineflayer.createBot({host:H,port:P,username:U,auth:'offline',version:'1.21.11'});
 bot.on('resourcePack',()=>bot.acceptResourcePack());
 bot.on('error',()=>{});bot.on('end',()=>{});process.on('uncaughtException',()=>{});
 if(!await wf(()=>bot.entity,40000)){console.log('brak spawnu');process.exit(7);}
 await send(`op ${U}`);await sleep(700);
 const dimOf=async()=>(await send(`data get entity ${U} Dimension`)).match(/"([^"]+)"/)?.[1];
 let preDim=await dimOf();
 // Jesli bot zalogowal sie w starej instancji -> natywny /md leave (RCON tp jest nadpisywany pakietami ruchu bota).
 // Swiezosc testu gwarantuje NOWY numer instancji ktory /md play stworzy, nie pozycja startowa bota.
 if(preDim&&/level_1_/.test(preDim)){
   bot.chat('/md leave');
   await wf(async()=>{const d=await dimOf();return d&&!/level_1_/.test(d)?d:false;},15000,600);
   preDim=await dimOf();
 }
 console.log('preDim po ewentualnym /md leave=',preDim);
 // KONTROLA fizyki w overworld PRZED wejsciem (czy mineflayer 1.21.11 w ogole chodzi):
 {
   const posOf0=async()=>((await send(`data get entity ${U} Pos`)).match(/\[[^\]]*\]/)||['?'])[0];
   const p0=await posOf0();
   await bot.look(0,0,true); bot.setControlState('forward',true); await sleep(4000); bot.setControlState('forward',false);
   console.log('[ow-walk] przed=',p0.slice(0,38),' po=',(await posOf0()).slice(0,38));
 }
 // TEST: wejscie w edit i wyjscie moze ladowac funkcje lochu do pamieci MD
 // (hipoteza: gdy triggery dzialaly, sesja edycji byla zawsze otwarta).
 if(process.env.EDIT_FIRST==='1'){
   bot.chat('/md edit level_1'); await sleep(6000);
   bot.chat('/md leave'); await sleep(5000);
   console.log('[edit-first] done, dim=', await dimOf());
 }
 bot.chat(`/md play level_1:${DIFF}`); // BEZ sufiksu gracza: sciezka "send player" bywa buggy (changelog MD)
 const dimIn=await wf(async()=>{const d=await dimOf();return d&&/level_1_/.test(d)?d:false;},25000,600);
 if(!dimIn){console.log('[FATAL] nie wszedl do instancji');process.exit(3);}
 console.log('SWIEZA instancja dim=',dimIn);
 await sleep(3000);
 // Ruch przez /minecraft:teleport z bota (jak shoot_level_1.js - ZWALIDOWANE ze dziala w play).
 // Po kazdym tp logujemy realna pozycje (czy cos cofa gracza na StartLocation).
 const posOf=async()=>((await send(`data get entity ${U} Pos`)).match(/\[[^\]]*\]/)||['?'])[0];
 for(const [px,pz] of [[0,120],[0,139],[0,90],[0,72]]){
   bot.chat(`/minecraft:teleport ${U} ${px} 64 ${pz}`); await sleep(2000);
   console.log(`[tp->z=${pz}] pos=`, (await posOf()).slice(0,40));
 }
 await sleep(2500);
 const wolfScan = await send(`execute in ${dimIn} run data get entity @e[type=minecraft:wolf,limit=1] CustomName`);
 console.log('[distance-test] wolf=', /No entity/i.test(wolfScan)?'BRAK':'JEST', '|', wolfScan.slice(0,80));
 // Teleport moze nie generowac eventu, ktorego MD sluchа. Test CHODZENIA: tp na z=120,
 // po zaladowaniu chunkow bot idzie pieszo na poludnie przez promien StartDungeon (z=138 r6).
 bot.chat(`/minecraft:teleport ${U} 0 64 120`); await sleep(3000);
 await bot.look(0,0,true); // yaw 0 = poludnie (+z)
 bot.setControlState('forward',true); await sleep(6000); bot.setControlState('forward',false);
 console.log('[walk-test] pos po 6s marszu na poludnie=', (await posOf()).slice(0,40));
 await sleep(2000);
 // DISKRYMINATOR startGame: po wejsciu startGame ustawia Gamemode SURVIVAL + tp do StartLocation (0,64,10).
 // Jesli gamemode flip + pozycja ~StartLocation -> startGame ODPALIL (zaweza do spawn-triggera).
 await sleep(800);
 const ppos=((await send(`data get entity ${U} Pos`)).match(/\[.*\]/)||[''])[0];
 const pgm=((await send(`data get entity ${U} playerGameType`)).match(/data:\s*(\S+)$/)||[,'?'])[1];
 console.log(`[startGame?] bot Pos=${ppos} playerGameType=${pgm} (0=SURVIVAL => StartDungeon ODPALIL; StartLocation~0,64,10)`);
 // GLOBALNY forceload szerokiego obszaru w wymiarze instancji (offset instancji moze byc duzy!)
 // -256..256 chunkow w obu osiach pokrywa typowe offsety MD
 for(let cx=-16;cx<=16;cx+=16){ await send(`execute in ${dimIn} run forceload add ${cx*16} -256 ${cx*16+255} 256`); }
 const scan=async(tag)=>{
   // GLOBALNY scan po CALYM wymiarze instancji (BEZ positioned/distance) - lapie boss niezaleznie od offsetu
   const zr=await send(`execute in ${dimIn} run data get entity @e[type=minecraft:zombie,limit=1] Pos`);
   let zpos='',zname='';
   if(!/No entity/i.test(zr)){
     zpos=zr.replace(/.*Pos: /,'').slice(0,60);
     const nr=await send(`execute in ${dimIn} run data get entity @e[type=minecraft:zombie,limit=1] CustomName`);
     zname=(nr.match(/CustomName: '?"?(.*)/)||[])[1]||'';
   }
   // dodatkowo policz inne istotne typy globalnie
   const others=[];
   for(const t of ['wolf','husk','text_display']){
     const r=await send(`execute in ${dimIn} run data get entity @e[type=minecraft:${t},limit=1]`);
     if(!/No entity was found/i.test(r)) others.push(t);
   }
   console.log(`[${tag}] zombie=${zpos?'JEST pos='+zpos+' name='+zname:'BRAK'} | inne=[${others.join(',')}]`);
 };
 // wydluzone okno (spawn delay 40t=2s + delayTicks triggera + 1L runTaskLater)
 await sleep(2000); await scan('t+2s');
 await sleep(2000); await scan('t+4s');
 await sleep(2000); await scan('t+6s');
 await sleep(2000); await scan('t+8s');
 await sleep(2000); await scan('t+10s');
 // === KONTROLA warstwowa: rozdziela "trigger nie odpalil" od "Rules/swiat blokuje spawn" ===
 // C1: surowy vanilla summon w instancji (czy Rules/swiat w ogole pozwala utrzymac encje?)
 const c1=await send(`execute in ${dimIn} positioned 0.0 64.0 5.0 run summon minecraft:zombie 0.0 64.0 5.0`);
 console.log('[C1 vanilla summon]',c1.slice(0,80));
 // C2: bezposredni MythicMobs spawn bossa w instancji poprawna skladnia world,x,y,z (omija MD; testuje MM+swiat)
 const world=dimIn.replace(/^minecraft:/,'');
 const c2=await send(`mm mobs spawn level_1_grove_guardian 1 ${world},0.5,64,0.5`);
 console.log('[C2 mm spawn w instancji]',c2.slice(0,140));
 await sleep(1500); await scan('po-kontroli');
 // CZYSTE wyjscie natywne (bez orphana): /md leave PRZED rozlaczeniem
 bot.chat('/md leave'); await sleep(2500);
 const outDim=(await send(`data get entity ${U} Dimension`)).match(/"([^"]+)"/)?.[1];
 console.log('po /md leave dim=',outDim);
 try{await rcon.end();}catch{}try{bot.quit();}catch{}
 process.exit(0);
})().catch(e=>{console.error('FATAL',e.message);process.exit(1);});
