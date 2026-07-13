// Test izolujacy: FinishDungeon na TriggerDungeonStart -> wejscie powinno NATYCHMIAST wyrzucic gracza.
const mineflayer=require('mineflayer');const {Rcon}=require('rcon-client');
const H='127.0.0.1',P=25565,RP=25575,RPASS=process.env.RCON_PASS,U='Dungeoneer';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const wf=async(f,to=60000,st=300)=>{const t=Date.now();while(Date.now()-t<to){if(await f())return 1;await sleep(st);}return 0;};
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
 // obserwuj 25s: czy wszedl i czy NATYCHMIAST wyszedl
 let entered=false;
 for(let i=0;i<25;i++){
   const d=await dimOf();
   if(d&&/level_1_/.test(d))entered=true;
   console.log(`t=${i} dim=${d} entered=${entered} finishChat=${finishChat}`);
   if(entered && d && !/level_1_/.test(d)){console.log('>>> WYRZUCONY z instancji po wejsciu = FinishDungeon zadzialal');break;}
   await sleep(1000);
 }
 try{await rcon.end();}catch{}try{bot.quit();}catch{}process.exit(0);
})().catch(e=>{console.error('F',e.message);process.exit(1);});
