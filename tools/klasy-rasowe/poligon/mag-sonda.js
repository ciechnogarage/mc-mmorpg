const mineflayer=require('/g/node_modules/mineflayer');const {attach}=require('/e/lib/resourcepack.js');const {rcon}=require('/s/rcon.js')
const sleep=ms=>new Promise(r=>setTimeout(r,ms));const F=bot=>bot._client.write('block_dig',{status:6,location:{x:0,y:0,z:0},face:0,sequence:0})
;(async()=>{const nick=process.env.NICK||'MagSondaX';const bot=mineflayer.createBot({host:process.env.MC_HOST,username:nick,auth:'offline',version:'1.21.11'});attach(bot._client);const msg=[];bot.on('messagestr',m=>{if(!/❤|SKILL CASTING/.test(m))msg.push(m)})
await new Promise(r=>bot.once('spawn',r));await sleep(5000);await rcon(`cc testpostac ${nick} ${process.env.RASA||'czlowiek'} mag`);await sleep(15000);for(let l=2;l<=7;l++){await rcon(`mmocore admin level set ${nick} main ${l}`);await sleep(1500)}
const pas=[];for(let s=1;s<=5;s++)pas.push((await rcon(`papi parse ${nick} %mmocore_bound_${s}%`)).trim());const bron=bot.quickBarSlot;const wolne=[...Array(9).keys()].filter(s=>s!==bron)
const rzuc=async n=>{const k=pas.findIndex(p=>p.includes(n));F(bot);await sleep(300);bot.setQuickBarSlot(wolne[k]);await sleep(300);F(bot);await sleep(300);bot.setQuickBarSlot(bron)}
for(const [typ,nazwa,mob] of [['wolf','Wilk Gajowy','GAJ_WILK'],['zombie','Korzeniak','GAJ_KORZENIAK']]){
 const SEL=`@e[name="${nazwa}",limit=1]`
 await rcon('minecraft:kill @e[type=!player,x=286,y=-63,z=286,dx=28,dy=12,dz=28]');await rcon(`minecraft:tp ${nick} 300.5 -59 300.5 0 0`);await sleep(800);await rcon(`mm mobs spawn ${mob} 1 world,300.5,-59,305.5`);await sleep(800);await rcon(`execute as ${SEL} run data merge entity @s {NoAI:1b}`)
 const c=bot.nearestEntity(e=>e.name===typ);if(c)await bot.lookAt(c.position.offset(0,typ==='zombie'?1.2:0.4,0),true);await rcon(`mmocore admin resource-mana set ${nick} 200`)
 const h0=(await rcon(`data get entity ${SEL} Health`)).replace(/.*data: /,'');const m0=(await rcon(`papi parse ${nick} %mmocore_mana%`)).trim();await rzuc('Wybuch');await sleep(2000)
 console.log(nazwa,'HP',h0,'->',(await rcon(`data get entity ${SEL} Health`)).replace(/.*data: /,''),'| mana',m0,'->',(await rcon(`papi parse ${nick} %mmocore_mana%`)).trim())}
bot.quit();await sleep(300);process.exit(0)})()
