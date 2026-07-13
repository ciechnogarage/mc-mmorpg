const mineflayer=require('mineflayer');const {Rcon}=require('rcon-client');const {Vec3}=require('vec3');
const H='127.0.0.1',P=25565,RP=25575,RPASS=process.env.RCON_PASS,U='Dungeoneer';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const wf=async(f,to=60000,st=300)=>{const t=Date.now();while(Date.now()-t<to){const v=await f();if(v)return v;await sleep(st);}return false;};
(async()=>{
 const rcon=await Rcon.connect({host:H,port:RP,password:RPASS});
 const send=async c=>(await rcon.send(c)).replace(/§[0-9a-fklmnorx]/g,'');
 const bot=mineflayer.createBot({host:H,port:P,username:U,auth:'offline',version:'1.21.11'});
 let finishChat=false;
 bot.on('resourcePack',()=>bot.acceptResourcePack());
 bot.on('message',j=>{const t=j.toString();if(/you completed|uko[nń]czy|completed|kwietn/i.test(t)){finishChat=true;console.log('[CHAT]',t);}});
 bot.on('error',()=>{});bot.on('end',()=>{});process.on('uncaughtException',()=>{});
 if(!await wf(()=>bot.entity,60000)){process.exit(7);}
 await send(`op ${U}`);await sleep(700);
 const dimOf=async()=>(await send(`data get entity ${U} Dimension`)).match(/"([^"]+)"/)?.[1];
 let pre=await dimOf();
 if(pre&&/level_1_/.test(pre)){await send(`execute in minecraft:overworld run tp ${U} 0 80 0`);await sleep(1500);pre=await dimOf();}
 console.log('preDim=',pre);
 bot.chat(`/md play level_1:NORMAL ${U}`);
 const dimIn=await wf(async()=>{const d=await dimOf();return d&&/level_1_/.test(d)?d:false;},25000,600);
 if(!dimIn){console.log('[FATAL] nie wszedl');process.exit(3);}
 console.log('instancja=',dimIn);
 await sleep(1500);
 await send(`gamemode creative ${U}`);await sleep(300);
 await send(`give ${U} minecraft:netherite_sword`);await sleep(600);
 await send(`effect give ${U} minecraft:strength 999 9 true`);await sleep(300);
 try{const sw=bot.inventory.items().find(i=>i.name.includes('netherite_sword'));if(sw)await bot.equip(sw,'hand');}catch{}
 const boss=()=>{const me=bot.entity.position;return Object.values(bot.entities).filter(e=>e.name==='zombie'&&e.position).map(e=>({e,d:me.distanceTo(e.position)})).sort((a,b)=>a.d-b.d)[0];}
 console.log('widzi:',JSON.stringify(Object.values(bot.entities).filter(e=>e.position&&e.type!=='player').reduce((a,e)=>(a[e.name]=(a[e.name]||0)+1,a),{})));
 for(let i=0;i<60 && !finishChat;i++){
   const d=await (async()=>i%4===0?await dimOf():dimIn)();
   if(finishChat||(d&&!/level_1_/.test(d))){break;}
   const b=boss();
   if(!b){if(i%5===0)console.log(`i=${i} brak zombie`);await sleep(400);continue;}
   try{await bot.lookAt(b.e.position.offset(0,1.0,0),true);}catch{}
   if(b.d>3.2){bot.setControlState('sprint',true);bot.setControlState('forward',true);await sleep(300);}
   else{bot.setControlState('forward',false);try{bot.attack(b.e);}catch{}await sleep(550);}
   if(i%5===0)console.log(`i=${i} dist=${b.d.toFixed(1)}`);
 }
 bot.setControlState('forward',false);bot.setControlState('sprint',false);
 const dimEnd=await dimOf();
 console.log('\n[WERDYKT] finish:', (finishChat||(dimEnd&&!/level_1_/.test(dimEnd)))?'TAK ✅':'NIE ❌','| finishChat=',finishChat,'dimEnd=',dimEnd);
 try{await rcon.end();}catch{}try{bot.quit();}catch{}process.exit(finishChat?0:1);
})().catch(e=>{console.error('F',e.message);process.exit(1);});
