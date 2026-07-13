const mineflayer=require('mineflayer');const{Rcon}=require('rcon-client');
const HOST='127.0.0.1',PORT=25565,RPORT=25575,RPASS=process.env.RCON_PASS,USER='Dungeoneer';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=30000,st=250)=>{const t=Date.now();while(Date.now()-t<to){const v=await fn();if(v)return v;await sleep(st);}return false;};
let rcon;const dimOf=async()=>(await rcon.send(`data get entity ${USER} Dimension`).catch(()=>'')).match(/"([^"]+)"/)?.[1];
const c=async(dim,sel)=>{await rcon.send(`scoreboard objectives add cc dummy`).catch(()=>{});await rcon.send(`execute in ${dim} store result score #c cc run execute if entity ${sel}`);return parseInt((await rcon.send(`scoreboard players get #c cc`)).match(/has (\d+)/)?.[1]||'0',10);};
(async()=>{rcon=await Rcon.connect({host:HOST,port:RPORT,password:RPASS});
 const bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
 bot.on('resourcePack',()=>bot.acceptResourcePack());
 if(!await waitFor(()=>bot.entity,60000,500)){console.log('brak spawnu');process.exit(7);}
 await rcon.send(`op ${USER}`);await sleep(800);
 bot.chat('/md play level_1:HARD');await sleep(4000);
 const dim=await dimOf();console.log('dim=',dim);
 for(const t of ['minecraft:zombie','minecraft:husk','minecraft:wolf','minecraft:armor_stand','minecraft:item_frame','minecraft:item_display','minecraft:interaction','minecraft:area_effect_cloud','minecraft:marker']){
   console.log(t,'=',await c(dim,`@e[type=${t}]`));
 }
 console.log('ALL non-player =',await c(dim,'@e[type=!minecraft:player]'));
 bot.chat('/md leave');await sleep(2000);
 try{await rcon.end();}catch{}try{bot.quit();}catch{}process.exit(0);
})().catch(e=>{console.error('FATAL',e);process.exit(1);});
