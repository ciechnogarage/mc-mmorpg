// WALKTHROUGH E2E lochu level_1 po redesignie: pelny luk oboz->sciezka->polana->grota->
// most(gatekeeper)->boss->komora nagrod->portal. REALNE kille (bot z mieczem+strength),
// zgodnie z memory mc-death-trigger-real-kill (NIGDY /kill).
// Pomiary server-side przez RCON (mc-measure-server-side). Uzycie: node walkthrough_level_1.js [EASY|NORMAL|HARD]
const mineflayer=require('mineflayer');const {Rcon}=require('rcon-client');
const H='127.0.0.1',P=25565,RP=25575,RPASS=process.env.RCON_PASS,U='FableProbe';
const DIFF=process.argv[2]||'EASY';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const wf=async(f,to=30000,st=400)=>{const t=Date.now();while(Date.now()-t<to){const v=await f();if(v)return v;await sleep(st);}return false;};
const T0=Date.now(); const el=()=>((Date.now()-T0)/1000).toFixed(0)+'s';
(async()=>{
 const rcon=await Rcon.connect({host:H,port:RP,password:RPASS});
 const send=async c=>(await rcon.send(c)).replace(/§[0-9a-fklmnorx]/g,'');
 const bot=mineflayer.createBot({host:H,port:P,username:U,auth:'offline',version:'1.21.11'});
 bot.on('resourcePack',()=>bot.acceptResourcePack());
 bot.on('error',()=>{});bot.on('end',()=>{});process.on('uncaughtException',()=>{});
 let msgs=[]; bot.on('message',m=>{const s=m.toString().trim(); if(s&&msgs.length<200){msgs.push(s); if(/Sekret|Korzenie|Gaj|Komora|fala|Fala|otworem|nagr/i.test(s)) console.log(`[chat ${el()}]`,s.slice(0,120));}});
 if(!await wf(()=>bot.entity,40000)){console.log('brak spawnu');process.exit(7);}
 await send(`op ${U}`);await sleep(700);
 const dimOf=async()=>(await send(`data get entity ${U} Dimension`)).match(/"([^"]+)"/)?.[1];
 if(/level_1_/.test(await dimOf()||'')){bot.chat('/md leave');await wf(async()=>!/level_1_/.test(await dimOf()||''),15000,600);}
 bot.chat(`/md play level_1:${DIFF} ${U}`);
 const dim=await wf(async()=>{const d=await dimOf();return d&&/level_1_/.test(d)?d:false;},30000,600);
 if(!dim){console.log('[FATAL] brak instancji');process.exit(3);}
 console.log(`[${el()}] instancja=${dim}`);
 await sleep(1500);
 const pos=async()=>((await send(`data get entity ${U} Pos`)).match(/\[.*\]/)||[''])[0];
 console.log(`[${el()}] start pos=${await pos()} (oczekiwane ~0,64,140 oboz)`);
 // forceload trasy (chunki -3..3 x, -4..10 z w blokach: -48..48 / -64..160)
 await send(`execute in ${dim} run forceload add -64 -64 64 176`);
 // uzbrojenie bota: realne kille w walce, ale szybkie
 await send(`give ${U} minecraft:netherite_sword`);
 await send(`effect give ${U} minecraft:strength infinite 29 true`);
 await send(`effect give ${U} minecraft:resistance infinite 4 true`);
 await send(`effect give ${U} minecraft:regeneration infinite 4 true`);
 await send(`effect give ${U} minecraft:saturation infinite 0 true`);
 await sleep(500);
 // ekwipuj miecz (MMOProfiles moze wyczyscic eq przy inicie profilu -> arm() wolane ponownie przed walkami)
 const arm=async()=>{
   if(!bot.inventory.items().some(i=>/netherite_sword/.test(i.name))){ await send(`give ${U} minecraft:netherite_sword`); await sleep(600); }
   const sw=bot.inventory.items().find(i=>/netherite_sword/.test(i.name));
   if(sw) await bot.equip(sw,'hand').catch(()=>{});
   console.log(`[${el()}] arm: held=${bot.heldItem?bot.heldItem.name:'(pusta reka, strength i tak zabija)'}`);
 };
 await arm();
 // POMIAR KLIENCKI: RCON @e nie widzi encji w wymiarach instancji MD (sonda _probe_summon 2026-07-02),
 // za to pakiety klienckie bota TAK. Moby z modelem ModelEngine (boss, sprout) sa ukryte —
 // widoczny jest ich hitbox 'interaction'; atak w interaction trafia moba.
 const HOSTILE=new Set(['wolf','husk','zombie','interaction']);
 const hostiles=(r=40)=>Object.values(bot.entities).filter(e=>e.position&&e.name&&HOSTILE.has(e.name)&&e.position.distanceTo(bot.entity.position)<r);
 const counts=()=>{const h=hostiles();const by={};for(const e of h)by[e.name]=(by[e.name]||0)+1;return JSON.stringify(by);};
 const isAir=async(x,y,z)=>/passed/i.test(await send(`execute in ${dim} if block ${x} ${y} ${z} minecraft:air`));
 // walka: atakuj najblizsze wrogie encje az znikna (timeout)
 const fight=async(label,toMs=90000)=>{
   const t=Date.now(); let calm=0;
   while(Date.now()-t<toMs){
     const hs=hostiles();
     if(hs.length===0){ calm++; if(calm>=4){console.log(`[${el()}] ${label}: wyczyszczone`);return true;} await sleep(600); continue; }
     calm=0;
     const e=hs.sort((a,b)=>a.position.distanceTo(bot.entity.position)-b.position.distanceTo(bot.entity.position))[0];
     if(e.position.distanceTo(bot.entity.position)>2.6){
       bot.chat(`/minecraft:teleport ${U} ${e.position.x.toFixed(1)} ${(e.position.y+0.2).toFixed(1)} ${(e.position.z-1.2).toFixed(1)}`); await sleep(450);
     }
     try{ await bot.lookAt(e.position.offset(0,1,0)); bot.attack(e); }catch{}
     await sleep(600);
   }
   console.log(`[${el()}] ${label}: TIMEOUT walki, zostalo: ${counts()}`);return false;
 };
 const go=async(x,y,z,note)=>{bot.chat(`/minecraft:teleport ${U} ${x} ${y} ${z}`);await sleep(1800);console.log(`[${el()}] tp ${note} (${x},${y},${z})`);};

 // === STREFA 2: zasadzki na sciezce ===
 await arm();
 await go(6,65,111,'zasadzka A'); await sleep(3500); console.log(`[${el()}] po A: ${counts()}`); await fight('zasadzka A');
 await go(-5,65,93,'zasadzka B'); await sleep(3500); console.log(`[${el()}] po B: ${counts()}`); await fight('zasadzka B');
 // === STREFA 3: polana prob (2 fale) ===
 await go(0,65,74,'polana'); await sleep(4000); console.log(`[${el()}] fala1: ${counts()}`); await fight('fala 1');
 await sleep(6000); console.log(`[${el()}] fala2 (po smierci wilkow): ${counts()}`);
 await fight('fala 2');
 // === STREFA 4: grota + sekret ===
 await go(27,65,76,'grota-sekret'); await sleep(3000);
 // === STREFA 5: most + gatekeeper ===
 const gateBefore=await isAir(0,66,46);
 await go(0,65,64,'przed mostem'); await sleep(3500); console.log(`[${el()}] warden: ${counts()} (brama z=46 air przed walka: ${gateBefore})`);
 await fight('gatekeeper',120000);
 await sleep(2500);
 console.log(`[${el()}] brama mostu otwarta (0,66,46 air): ${await isAir(0,66,46)}`);
 // === BOSS ===
 await go(0,65,12,'wejscie na arene'); await sleep(5000);
 console.log(`[${el()}] boss: ${counts()}`);
 await fight('BOSS',240000);
 await sleep(3000);
 console.log(`[${el()}] po bossie: brama nagrod (0,65,-32 air): ${await isAir(0,65,-32)} | finish msgs: ${msgs.filter(m=>/finish|ukonczon|complete|Gaj/i.test(m)).slice(-3).join(' | ')}`);
 // === KOMORA NAGROD + PORTAL ===
 await go(0,60,-39,'komora nagrod'); await sleep(3000);
 await go(0,60,-41,'portal wyjscia');
 const out=await wf(async()=>{const d=await dimOf();return d&&!/level_1_/.test(d)?d:false;},20000,800);
 console.log(`[${el()}] LeaveDungeon: dim po portalu = ${out||'NIE ZADZIALAL'}`);
 console.log(`[${el()}] inwentarz bota (nagrody):`, bot.inventory.items().map(i=>`${i.name}x${i.count}`).join(', ')||'(pusty)');
 console.log('=== OSTATNIE KOMUNIKATY ==='); msgs.slice(-25).forEach(m=>console.log('  ',m.slice(0,130)));
 try{await send(`execute in ${dim} run forceload remove all`);}catch{}
 try{await rcon.end();}catch{} try{bot.quit();}catch{}
 process.exit(0);
})().catch(e=>{console.error('FATAL',e.message);process.exit(1);});
