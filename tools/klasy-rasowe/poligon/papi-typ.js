const mineflayer=require('/g/node_modules/mineflayer');const {attach}=require('/e/lib/resourcepack.js');const {rcon}=require('/s/rcon.js');const sleep=ms=>new Promise(r=>setTimeout(r,ms))
;(async()=>{const n='PapiTyp';const bot=mineflayer.createBot({host:process.env.MC_HOST,username:n,auth:'offline',version:'1.21.11'});attach(bot._client);await new Promise(r=>bot.once('spawn',r));await sleep(4000)
await rcon(`cc testpostac ${n} czlowiek mag`);await sleep(14000);await rcon(`mi give TOME AMPLIFICATION_TOME ${n} 1`);await sleep(1000)
const P=['%mmoitems_type%','%mmoitems_type_mainhand%','%mmoitems_type_main_hand%','%mmoitems_type_hand%','%mmoitems_type_offhand%','%mmoitems_type_off_hand%','%mmoitems_tier%']
const pokaz=async t=>{const o=[];for(const p of P)o.push(p+'='+(await rcon(`papi parse ${n} ${p}`)).trim());console.log(t,o.join(' '))}
await pokaz('PUSTO     ')
const it=bot.inventory.items().find(i=>i.name==='book');await bot.equip(it,'hand');await sleep(800);await pokaz('W GLOWNEJ ')
await bot.equip(bot.heldItem,'off-hand');await sleep(800);await pokaz('W DRUGIEJ ')
bot.quit();await sleep(300);process.exit(0)})().catch(e=>{console.error(e);process.exit(2)})
