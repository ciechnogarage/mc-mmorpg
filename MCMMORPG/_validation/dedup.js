const mineflayer=require('mineflayer');const{Rcon}=require('rcon-client');
const HOST='127.0.0.1',PORT=25565,RPORT=25575,RPASS=process.env.RCON_PASS,USER='Dungeoneer';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=30000,st=250)=>{const t=Date.now();while(Date.now()-t<to){const v=await fn();if(v)return v;await sleep(st);}return false;};
let rcon;const dimOf=async()=>(await rcon.send(`data get entity ${USER} Dimension`).catch(()=>'')).match(/"([^"]+)"/)?.[1];
const c=async(dim,sel)=>{await rcon.send(`scoreboard objectives add cc dummy`).catch(()=>{});await rcon.send(`execute in ${dim} store result score #c cc run execute if entity ${sel}`);return parseInt((await rcon.send(`scoreboard players get #c cc`)).match(/has (\d+)/)?.[1]||'0',10);};
(async()=>{rcon=await Rcon.connect({host:HOST,port:RPORT,password:RPASS});
 const bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
 bot.on('resourcePack',()=>bot.acceptResourcePack());
 bot.on('message',j=>{const t=j.toString();if(/killed|spawn|mythic|removed/i.test(t)&&!/❤/.test(t))console.log('[chat]',t.slice(0,80));});
 if(!await waitFor(()=>bot.entity,60000,500)){console.log('brak spawnu');process.exit(7);}
 await rcon.send(`op ${USER}`);await sleep(800);
 // ===== FAZA EDIT: dedup =====
 bot.chat('/md edit level_1');await sleep(5000);
 let dim=await dimOf();console.log('[edit] dim=',dim);
 await rcon.send(`gamemode creative ${USER}`);await rcon.send(`tp ${USER} 0 64 0`);await sleep(800);
 console.log('[edit] przed: zombie=',await c(dim,'@e[type=minecraft:zombie]'));
 bot.chat('/mm mobs killall level_1_grove_guardian');await sleep(1500);          // purge Mythic bossy
 await rcon.send(`execute in ${dim} run kill @e[type=minecraft:zombie]`);// purge stray vanilla
 await rcon.send(`execute in ${dim} run kill @e[type=minecraft:wolf]`);
 await rcon.send(`execute in ${dim} run kill @e[type=minecraft:husk]`);
 await sleep(1200);
 console.log('[edit] po purge: zombie=',await c(dim,'@e[type=minecraft:zombie]'),'wolf=',await c(dim,'@e[type=minecraft:wolf]'));
 await rcon.send(`tp ${USER} 0 64 0`);await sleep(500);
 bot.chat('/mm mobs spawn level_1_grove_guardian 1');await sleep(1500);          // dokladnie 1
 console.log('[edit] po spawn: zombie=',await c(dim,'@e[type=minecraft:zombie]'));
 bot.chat('/md leave');await sleep(3500);
 console.log('[edit] leave -> dim=',await dimOf());
 // ===== FAZA REPLAY: werdykt zapisu =====
 bot.chat('/md play level_1:HARD');await sleep(5000);
 dim=await dimOf();console.log('[replay] dim=',dim);
 if(!dim||/overworld/.test(dim)){console.log('[replay] nie wszedl ❌');process.exit(3);}
 const z=await c(dim,'@e[type=minecraft:zombie]');
 console.log('[replay] zombie w SWIEZEJ instancji=',z);
 console.log(await rcon.send(`execute in ${dim} as @e[type=minecraft:zombie] run data get entity @s Health`));
 console.log('[WERDYKT] zapis dziala & dedup OK jesli zombie==1 (1 boss):', z===1?'TAK ✅':`NIE (z=${z}) ❌`);
 bot.chat('/md leave');await sleep(1500);
 try{await rcon.end();}catch{}try{bot.quit();}catch{}process.exit(0);
})().catch(e=>{console.error('FATAL',e);process.exit(1);});
