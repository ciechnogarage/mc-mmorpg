// Sonda premii za broń rasową: Magister Wieży, Pieczęć bez grimuaru i z grimuarem (TOME) w drugiej ręce.
const mineflayer=require('/g/node_modules/mineflayer');const {attach}=require('/e/lib/resourcepack.js');const {rcon}=require('/s/rcon.js')
const sleep=ms=>new Promise(r=>setTimeout(r,ms));const F=bot=>bot._client.write('block_dig',{status:6,location:{x:0,y:0,z:0},face:0,sequence:0})
;(async()=>{const nick=process.env.NICK||'BronSonda';const bot=mineflayer.createBot({host:process.env.MC_HOST,username:nick,auth:'offline',version:'1.21.11'});attach(bot._client);bot.on('messagestr',m=>{if(/DBG/.test(m))console.log(m)})
await new Promise(r=>bot.once('spawn',r));await sleep(5000);await rcon(`cc testpostac ${nick} czlowiek mag`);await sleep(15000);await rcon(`mmocore admin level set ${nick} main 3`);await sleep(2000)
const pas=[];for(let s=1;s<=5;s++)pas.push((await rcon(`papi parse ${nick} %mmocore_bound_${s}%`)).trim());console.log('pasek',pas.join(' | '))
const bron=bot.quickBarSlot;const wolne=[...Array(9).keys()].filter(s=>s!==bron)
const rzuc=async n=>{const k=pas.findIndex(p=>p.includes(n));F(bot);await sleep(300);bot.setQuickBarSlot(wolne[k]);await sleep(300);F(bot);await sleep(300);bot.setQuickBarSlot(bron)}
const SEL='@e[name="Korzeniak",limit=1]'
for(const zBronia of [false,true]){
 if(zBronia){await rcon(`mi give TOME AMPLIFICATION_TOME ${nick} 1`);await sleep(1000);const it=bot.inventory.items().find(i=>i.name==='book'||/tome|book/i.test(i.name));console.log('grimuar w slocie',it&&it.slot,it&&it.name)
  if(it){try{await bot.equip(it,'off-hand')}catch(e){console.log('equip',e.message)};await sleep(800)}
  console.log('druga reka nbt:',(await rcon(`data get entity ${nick} equipment.offhand.components."minecraft:custom_data".MMOITEMS_ITEM_TYPE`)).trim())}
 await rcon('minecraft:kill @e[type=!player,x=286,y=-63,z=286,dx=28,dy=12,dz=28]');await rcon(`minecraft:tp ${nick} 300.5 -59 300.5 0 0`);await sleep(800);await rcon(`mm mobs spawn GAJ_KORZENIAK 1 world,300.5,-59,305.5`);await sleep(800);await rcon(`execute as ${SEL} run data merge entity @s {NoAI:1b}`)
 const c=bot.nearestEntity(e=>e.name==='zombie');if(c)await bot.lookAt(c.position.offset(0,1.2,0),true);await rcon(`mmocore admin resource-mana set ${nick} 200`)
 const h0=(await rcon(`data get entity ${SEL} Health`)).replace(/.*data: /,'');await rzuc('Pieczęć');let ef='';for(let i=0;i<15;i++){const e=(await rcon(`data get entity ${SEL} active_effects`)).trim();if(/slowness/.test(e)){ef=e;break};ef=e;await sleep(150)}
 console.log(`${zBronia?'Z GRIMUAREM':'BEZ BRONI  '}: HP ${h0} -> ${(await rcon(`data get entity ${SEL} Health`)).replace(/.*data: /,'')} | spowolnienie: ${/slowness/.test(ef)?'TAK':'nie'} | ${ef.slice(0,160)}`)}
bot.quit();await sleep(300);process.exit(0)})().catch(e=>{console.error(e);process.exit(2)})
