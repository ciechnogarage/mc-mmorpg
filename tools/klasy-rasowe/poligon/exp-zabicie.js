// Czy zabójstwo skillem przez znacznik (Wybuch, strefy) daje graczowi EXP? Cel TEST_SLABY (4 HP, NoAI).
const mineflayer=require('/g/node_modules/mineflayer');const {attach}=require('/e/lib/resourcepack.js');const {rcon}=require('/s/rcon.js');const Vec3=require('/g/node_modules/vec3')
const sleep=ms=>new Promise(r=>setTimeout(r,ms));const F=bot=>bot._client.write('block_dig',{status:6,location:{x:0,y:0,z:0},face:0,sequence:0});const num=s=>{const m=/(-?[\d.]+)\s*$/.exec(String(s).trim());return m?+m[1]:NaN}
;(async()=>{const n='ExpZab';const bot=mineflayer.createBot({host:process.env.MC_HOST,username:n,auth:'offline',version:'1.21.11'});attach(bot._client);bot.on('messagestr',m=>{if(/DBG/.test(m))console.log(m)});await new Promise(r=>bot.once('spawn',r));await sleep(5000)
await rcon(`cc testpostac ${n} czlowiek mag`);await sleep(15000);for(let l=2;l<=7;l++){await rcon(`mmocore admin level set ${n} main ${l}`);await sleep(800)}
const pas=[];for(let s=1;s<=5;s++)pas.push((await rcon(`papi parse ${n} %mmocore_bound_${s}%`)).trim());const bron=bot.quickBarSlot,wolne=[...Array(9).keys()].filter(s=>s!==bron)
const rzuc=async nm=>{const k=pas.findIndex(p=>p.includes(nm));F(bot);await sleep(300);bot.setQuickBarSlot(wolne[k]);await sleep(300);F(bot);await sleep(300);bot.setQuickBarSlot(bron)}
await rcon('forceload add 280 280 320 320');await rcon('minecraft:kill @e[type=!player,x=286,y=-63,z=286,dx=28,dy=12,dz=28]');await rcon(`minecraft:tp ${n} 300.5 -59 300.5 0 0`);await sleep(900)
for(const [skill,opis] of [['Pieczęć','pocisk gracza (kontrola)'],['Wybuch','znacznik Wybuchu']]){
 await rcon('mm mobs spawn TEST_SLABY 1 world,300.5,-59,305.5');await sleep(700);await rcon('execute as @e[name="Slaby"] run data merge entity @s {NoAI:1b}');await bot.lookAt(new Vec3(300.5,-57.8,305.5),true);await rcon(`mmocore admin resource-mana set ${n} 200`);await sleep(500)
 const e0=num(await rcon(`papi parse ${n} %mmocore_experience%`));await rzuc(skill);if(skill==='Pieczęć'){await sleep(1500);await rzuc('Pieczęć')};await sleep(3000)
 const zyje=(await rcon('execute if entity @e[name="Slaby"]')).trim();const e1=num(await rcon(`papi parse ${n} %mmocore_experience%`))
 console.log(`${opis}: cel ${/passed/.test(zyje)?'ŻYJE':'zabity'}, EXP ${e0} -> ${e1} ${e1>e0?'PASS':'FAIL'}`);await rcon('minecraft:kill @e[name="Slaby"]');await sleep(7000)}
bot.quit();await sleep(300);process.exit(0)})().catch(e=>{console.error(e);process.exit(2)})
