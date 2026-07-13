// Faza 1: bot wchodzi do swiata Iris `level_1`, waliduje ksztalt wyspy (grass vs void),
// i buduje arene bossa (vanilla /fill + /setblock — bot jest w level_1, wiec komendy cela ten swiat).
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const { Vec3 } = require('vec3');
const HOST='127.0.0.1', PORT=25565, RPORT=25575, RPASS=process.env.RCON_PASS, USER='Builder', WORLD='level_1';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function waitFor(fn,to=30000,st=250){const t=Date.now();while(Date.now()-t<to){if(await fn())return true;await sleep(st);}return false;}

(async()=>{
  const rcon=await Rcon.connect({host:HOST,port:RPORT,password:RPASS});
  console.log('[rcon] ok');
  const bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
  bot.on('resourcePack',()=>bot.acceptResourcePack());
  bot.on('kicked',r=>console.log('[KICK]',JSON.stringify(r)));
  bot.on('error',e=>console.log('[ERR]',e.message));
  const WD=setTimeout(()=>{console.log('[FATAL] watchdog');process.exit(8);},280000);

  if(!await waitFor(()=>!!bot.entity,90000,500)){console.log('[FATAL] brak spawnu');process.exit(7);}
  console.log('[bot] spawned w', bot.game.dimension);
  await rcon.send(`op ${USER}`); await sleep(400);
  await rcon.send(`gamemode spectator ${USER}`);

  // wejscie do level_1
  let changed=false; bot.on('spawn',()=>changed=true);
  changed=false; bot.chat(`/mvtp ${WORLD}`);
  let ok=await waitFor(()=>changed,15000,400);
  if(!ok){changed=false;bot.chat(`/mv tp ${WORLD}`);ok=await waitFor(()=>changed,15000,400);}
  await sleep(2000);
  console.log('[bot] po tp, pozycja', bot.entity.position);

  // --- skan ksztaltu wyspy ---
  function surface(x,z){for(let y=80;y>=20;y--){const b=bot.blockAt(new Vec3(x+0.5,y,z+0.5));if(b&&b.name!=='air'&&b.name!=='void_air'&&b.name!=='cave_air')return b.name;}return null;}
  const grid={},hist={};
  const wps=[-60,-20,20,60].flatMap(z=>[[0,z]]);
  for(const [wx,wz] of wps){
    await rcon.send(`tp ${USER} ${wx} 100 ${wz}`); await sleep(1800);
    await waitFor(()=>surface(wx,wz)!==null,6000);
    for(let x=-90;x<=90;x+=10)for(let z=wz-45;z<=wz+45;z+=10){
      const n=surface(x,z); const t=n==='grass_block'?'G':(n===null||n==='bedrock'?'.':'?');
      grid[`${x},${z}`]=t; hist[t]=(hist[t]||0)+1;
    }
    console.log('[scan] z=',wz,'ok');
  }
  console.log('\n=== WYSPA (G=grass, .=void/bedrock) Z malejaco / X poziomo ===');
  for(let z=80;z>=-80;z-=10){let row='';for(let x=-90;x<=90;x+=10){row+=(grid[`${x},${z}`]||' ')+' ';}console.log(String(z).padStart(4),row);}
  console.log('histogram:',hist);

  // --- budowa areny bossa w centrum (0,63,0) ---
  console.log('\n[build] arena bossa @ (0,0)...');
  await rcon.send(`tp ${USER} 0 90 0`); await sleep(1500);
  const SY=63; // powierzchnia lakowa
  const cmd=async c=>{bot.chat(c);await sleep(120);};
  // plac areny: wyrownaj 24x24 do grass na SY, oczysc nad nim
  await cmd(`/fill -12 ${SY} -12 12 ${SY} 12 minecraft:grass_block`);
  await cmd(`/fill -12 ${SY+1} -12 12 ${SY+5} 12 minecraft:air`);
  // murek mossy_cobblestone wokol (4 sciany, wys 2)
  await cmd(`/fill -12 ${SY+1} -12 12 ${SY+2} -12 minecraft:mossy_cobblestone`);
  await cmd(`/fill -12 ${SY+1} 12 12 ${SY+2} 12 minecraft:mossy_cobblestone`);
  await cmd(`/fill -12 ${SY+1} -12 -12 ${SY+2} 12 minecraft:mossy_cobblestone`);
  await cmd(`/fill 12 ${SY+1} -12 12 ${SY+2} 12 minecraft:mossy_cobblestone`);
  // 4 filary-drzewa w rogach (oak_log + leaves)
  for(const [px,pz] of [[-10,-10],[10,-10],[-10,10],[10,10]]){
    await cmd(`/fill ${px} ${SY+1} ${pz} ${px} ${SY+4} ${pz} minecraft:oak_log`);
    await cmd(`/fill ${px-1} ${SY+5} ${pz-1} ${px+1} ${SY+6} ${pz+1} minecraft:oak_leaves`);
  }
  // skrzynia lootu w centrum + lectern dekoracyjny
  await cmd(`/setblock 0 ${SY+1} 2 minecraft:chest`);
  await cmd(`/setblock 0 ${SY+1} -2 minecraft:flowering_azalea`);
  // dekoracje lakowe: troche kwiatow na placu
  for(let i=0;i<14;i++){const x=Math.floor(Math.random()*22-11),z=Math.floor(Math.random()*22-11);const f=['minecraft:poppy','minecraft:dandelion','minecraft:cornflower','minecraft:oxeye_daisy','minecraft:short_grass'][i%5];await cmd(`/setblock ${x} ${SY+1} ${z} ${f}`);}
  console.log('[build] arena gotowa');

  // weryfikacja: odczyt kilku blokow areny
  await rcon.send(`tp ${USER} 0 80 0`); await sleep(1500);
  const checks={chest:surface(0,2),wallN:bot.blockAt(new Vec3(0.5,SY+1,-12+0.5)),log:bot.blockAt(new Vec3(-10+0.5,SY+2,-10+0.5))};
  console.log('[verify] chest@0,2 =',checks.chest,'| wallN =',checks.wallN&&checks.wallN.name,'| log =',checks.log&&checks.log.name);

  clearTimeout(WD); await rcon.end(); bot.quit(); process.exit(0);
})().catch(e=>{console.error('FATAL',e);process.exit(1);});
