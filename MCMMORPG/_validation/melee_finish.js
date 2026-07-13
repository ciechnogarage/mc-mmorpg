// DECYDUJACY test finishu: bot zabija bossa PRAWDZIWYM melee (bot.attack) -> przechodzi
// przez pipeline obrazen MythicMobs -> MythicMobDeathEvent -> MD TriggerMythicMobDeath -> FinishDungeon.
// /damage i /kill tego NIE robia (vanilla death / despawn). Bot ma resistance+strength+netherite, by przezyc i bic szybko.
const mineflayer=require('mineflayer');const {Rcon}=require('rcon-client');const {Vec3}=require('vec3');
const H='127.0.0.1',P=25565,RP=25575,RPASS=process.env.RCON_PASS,U='Dungeoneer';
const DIFF=process.argv[2]||'NORMAL';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const wf=async(f,to=60000,st=300)=>{const t=Date.now();while(Date.now()-t<to){const v=await f();if(v)return v;await sleep(st);}return false;};
(async()=>{
 const rcon=await Rcon.connect({host:H,port:RP,password:RPASS});
 const send=async c=>(await rcon.send(c)).replace(/§[0-9a-fklmnorx]/g,'');
 const bot=mineflayer.createBot({host:H,port:P,username:U,auth:'offline',version:'1.21.11'});
 let finishChat=false;
 bot.on('resourcePack',()=>bot.acceptResourcePack());
 bot.on('message',j=>{const t=j.toString();if(/you completed|uko[nń]czy|completed the dungeon|left the dungeon|kwietn|powr[oó]/i.test(t)){finishChat=true;console.log('[CHAT]',t);}});
 bot.on('error',()=>{});bot.on('end',()=>{});process.on('uncaughtException',()=>{});
 if(!await wf(()=>bot.entity,60000)){console.log('brak spawnu');process.exit(7);}
 await send(`op ${U}`);await sleep(700);
 const dimOf=async()=>(await send(`data get entity ${U} Dimension`)).match(/"([^"]+)"/)?.[1];
 // MUSI startowac poza instancja (return-point = overworld/hub). Jesli w instancji - tp do overworld.
 let preDim=await dimOf();
 if(preDim&&/level_1_/.test(preDim)){
   await send(`execute in minecraft:overworld run tp ${U} 0 80 0`);await sleep(1500);
   preDim=await dimOf();
 }
 console.log('preDim (powinno byc overworld)=',preDim);
 bot.chat(`/md play level_1:${DIFF} ${U}`);
 const dimIn=await wf(async()=>{const d=await dimOf();return d&&/level_1_/.test(d)?d:false;},25000,600);
 if(!dimIn){console.log('[FATAL] nie wszedl do instancji');process.exit(3);}
 console.log('instancja=',dimIn,'diff=',DIFF,'| finish = powrot do',preDim);
 await sleep(2000);
 // bot ma przezyc i bic mocno
 await send(`gamemode survival ${U}`);await sleep(300);
 await send(`effect give ${U} minecraft:resistance 999 4 true`);
 await send(`effect give ${U} minecraft:strength 999 5 true`);
 await send(`effect give ${U} minecraft:regeneration 999 4 true`);
 await send(`effect give ${U} minecraft:speed 999 2 true`);
 await send(`give ${U} minecraft:netherite_sword`);await sleep(800);
 try{const sw=bot.inventory.items().find(i=>i.name.includes('netherite_sword'));if(sw)await bot.equip(sw,'hand');}catch{}
 const bossNear=()=>{const me=bot.entity.position;return Object.values(bot.entities).filter(e=>e.name==='zombie'&&e.position).map(e=>({e,d:me.distanceTo(e.position)})).sort((a,b)=>a.d-b.d)[0];}
 const ecount=()=>{const all=Object.values(bot.entities).filter(e=>e.position&&e.type!=='player');const byname={};all.forEach(e=>byname[e.name]=(byname[e.name]||0)+1);return JSON.stringify(byname);}
 console.log('[bot] po wejsciu poz=',bot.entity.position,'widzi encje=',ecount());
 // podejdz do bossa i bij; finish = zmiana wymiaru / chat
 let finished=false;
 for(let i=0;i<70 && !finished;i++){
   const dn=await (async()=>i%4===0?await dimOf():dimIn)();
   if(finishChat||(dn&&!/level_1_/.test(dn))){finished=true;break;}
   const b=bossNear();
   if(!b){ // boss nie widoczny - idz w strone boss-loc (0,64,0) zeby doladowac chunk
     if(i%5===0) console.log(`  i=${i} BRAK zombie w percepcji; poz=${bot.entity.position.toString().slice(0,40)} encje=${ecount()}`);
     try{ await bot.lookAt(new Vec3(0,64,0),true); }catch{}
     bot.setControlState('forward',true);await sleep(500);
     if(i>3){const d=await dimOf();if(finishChat||(d&&!/level_1_/.test(d))){finished=true;break;}}
     continue;
   }
   try{ await bot.lookAt(b.e.position.offset(0,1.2,0),true); }catch{}
   if(b.d>3){ // idz do bossa
     bot.setControlState('sprint',true);bot.setControlState('forward',true);
     await sleep(350);
   }else{
     bot.setControlState('forward',false);
     try{ bot.attack(b.e); }catch{}
     await sleep(620); // ~cooldown miecza
   }
   if(i%6===0) console.log(`  i=${i} bossDist=${b.d.toFixed(1)} botHP=${bot.health}`);
 }
 bot.setControlState('forward',false);bot.setControlState('sprint',false);
 const dimEnd=await dimOf();
 const ok=finished||finishChat||(dimEnd&&!/level_1_/.test(dimEnd));
 console.log('\n[WERDYKT] FinishDungeon palnal:', ok?'TAK ✅':'NIE ❌','| dimEnd=',dimEnd,'finishChat=',finishChat);
 try{await rcon.end();}catch{}try{bot.quit();}catch{}
 process.exit((finished||finishChat)?0:1);
})().catch(e=>{console.error('FATAL',e.message);process.exit(1);});
