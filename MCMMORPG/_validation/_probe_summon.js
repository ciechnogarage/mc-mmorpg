// SONDA: dlaczego summon{} nie materializuje mobow w instancji level_1?
// A) czy camp_ambience/message odpalily na starcie (stoimy 20s w obozie)
// B) bot castuje /mm test cast level_1_wave_1 na polanie -> czy wilki wchodza (skill dziala w instancji?)
// C) RCON mm mobs spawn wilka w instancji -> czy bezposredni spawn dziala (swiat pozwala?)
const mineflayer=require('mineflayer');const {Rcon}=require('rcon-client');
const H='127.0.0.1',P=25565,RP=25575,RPASS=process.env.RCON_PASS,U='FableProbe';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const wf=async(f,to=30000,st=400)=>{const t=Date.now();while(Date.now()-t<to){const v=await f();if(v)return v;await sleep(st);}return false;};
(async()=>{
 const rcon=await Rcon.connect({host:H,port:RP,password:RPASS});
 const send=async c=>(await rcon.send(c)).replace(/§[0-9a-fklmnorx]/g,'');
 const bot=mineflayer.createBot({host:H,port:P,username:U,auth:'offline',version:'1.21.11'});
 bot.on('resourcePack',()=>bot.acceptResourcePack());
 bot.on('error',()=>{});bot.on('end',()=>{});process.on('uncaughtException',()=>{});
 bot.on('message',m=>{const s=m.toString().trim();if(s&&!/Usage|available|version|CMI|Download|spigotmc|modrinth|FAWE|build/i.test(s))console.log('[chat]',s.slice(0,130));});
 if(!await wf(()=>bot.entity,40000)){console.log('brak spawnu');process.exit(7);}
 await send(`op ${U}`);await sleep(700);
 const dimOf=async()=>(await send(`data get entity ${U} Dimension`)).match(/"([^"]+)"/)?.[1];
 if(/level_1_/.test(await dimOf()||'')){bot.chat('/md leave');await wf(async()=>!/level_1_/.test(await dimOf()||''),15000,600);}
 bot.chat(`/md play level_1:EASY ${U}`);
 const dim=await wf(async()=>{const d=await dimOf();return d&&/level_1_/.test(d)?d:false;},30000,600);
 if(!dim){console.log('[FATAL] brak instancji');process.exit(3);}
 console.log('instancja=',dim);
 await send(`execute in ${dim} run forceload add -64 -64 64 176`);
 const count=async t=>{const r=await send(`execute in ${dim} if entity @e[type=minecraft:${t}]`);const m=r.match(/count:\s*(\d+)/);return m?+m[1]:0;};
 const st=async(tag)=>console.log(`[${tag}] wolf=${await count('wolf')} husk=${await count('husk')} zombie=${await count('zombie')} stand=${await count('armor_stand')}`);
 // pomiar dimension-precyzyjny (positional args ograniczaja selektor do wymiaru executa)
 const cnt=async t=>{const r=await send(`execute in ${dim} if entity @e[type=minecraft:${t},x=-64,y=0,z=-64,dx=128,dy=128,dz=256]`);const m=r.match(/count:\s*(\d+)/);return m?+m[1]:0;};
 bot.chat(`/minecraft:teleport ${U} 0.5 65 74.5`); await sleep(2000);
 console.log('gamerules instancji: doMobSpawning=',(await send(`execute in ${dim} run gamerule doMobSpawning`)).slice(-8),
   ' doDaylight=',(await send(`execute in ${dim} run gamerule doDaylightCycle`)).slice(-8));
 // D1: VANILLA summon w instancji
 console.log('[D1 vanilla]',(await send(`execute in ${dim} run summon minecraft:wolf 3.5 65 74.5`)).slice(0,80));
 await sleep(300); console.log('  wolf po 0.3s:',await cnt('wolf'));
 await sleep(2000); console.log('  wolf po 2.3s:',await cnt('wolf'));
 // D2: mm spawn w instancji
 const world=dim.replace(/^minecraft:/,'');
 console.log('[D2 mm]',(await send(`mm mobs spawn level_1_grove_wolf 1 ${world},5.5,65,74.5`)).slice(0,90));
 await sleep(300); console.log('  wolf po 0.3s:',await cnt('wolf'),' listactive:',(await send('mm mobs listactive')).slice(0,140));
 await sleep(2000); console.log('  wolf po 2.3s:',await cnt('wolf'));
 // D3: mm spawn zombie-bossa (typ zombie)
 console.log('[D3 mm boss]',(await send(`mm mobs spawn level_1_grove_guardian 1 ${world},0.5,65,64.5`)).slice(0,90));
 await sleep(2500); console.log('  zombie po 2.5s:',await cnt('zombie'),' stand:',await cnt('armor_stand'));
 // D4: PRAWDA KLIENCKA — encje widziane przez bota (pakiety serwera) + selektor z kontekstu bota
 const ents=Object.values(bot.entities).filter(e=>e.name&&e.name!=='player'&&e.position&&e.position.distanceTo(bot.entity.position)<48)
   .map(e=>`${e.name}@(${e.position.x.toFixed(0)},${e.position.y.toFixed(0)},${e.position.z.toFixed(0)})`);
 console.log('[D4 bot.entities <48blk]:', ents.join(' ')||'(NIC)');
 bot.chat('/execute if entity @e[type=minecraft:wolf,distance=..32]'); await sleep(1200);
 bot.chat('/execute if entity @e[type=minecraft:zombie,distance=..32]'); await sleep(1200);
 // D5: czy listactive nadal je widzi
 console.log('[D5 listactive]:', (await send('mm mobs listactive')).slice(0,300));
 bot.chat('/md leave'); await sleep(2500);
 try{await rcon.end();}catch{}try{bot.quit();}catch{}
 process.exit(0);
})().catch(e=>{console.error('FATAL',e.message);process.exit(1);});
