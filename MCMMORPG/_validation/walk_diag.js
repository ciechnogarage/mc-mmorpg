const mineflayer = require('mineflayer');
const { execFileSync } = require('child_process');
const rcon = c => execFileSync('docker', ['exec','-i','srv-world','rcon-cli',c],{encoding:'utf8'}).trim();
const sleep = ms => new Promise(r=>setTimeout(r,ms));
(async()=>{
  const bot = mineflayer.createBot({host:'127.0.0.1',port:25565,username:'WalkDiag',auth:'offline',version:'1.21.11'});
  bot.on('resourcePack',()=>bot.acceptResourcePack()); bot.on('error',()=>{}); process.on('uncaughtException',()=>{});
  while(!bot.entity) await sleep(300);
  rcon('op WalkDiag'); await sleep(300); rcon('time set night'); rcon('tp WalkDiag 8 120 8'); await sleep(2500);
  const A=bot.entity.position.clone();
  rcon(`mm mobs spawn level_1_glimmer_wisp 1 world,${Math.round(A.x)+6},${Math.round(A.y)+1},${Math.round(A.z)}`); await sleep(1500);
  const centr=()=>{const m=rcon('execute as @e[type=husk,limit=1] run data get entity @s Pos').match(/\[([-\d.]+)d, ([-\d.]+)d, ([-\d.]+)d\]/); return m?{x:+m[1],z:+m[3]}:null;};
  const out=[];
  for(let i=0;i<20;i++){ const c=centr(); out.push(c?Math.hypot(c.x-bot.entity.position.x,c.z-bot.entity.position.z).toFixed(1):'?'); await sleep(1000); }
  console.log('dystans[s]:',out.join(' '));
  rcon('mm mobs killall'); process.exit(0);
})();
