// Skan powierzchni backupu (l1view): dla siatki punktow znajdz najwyzszy niepowietrzny blok
// (od y100 w dol) -> mapa terenu/void wzdluz stref start(pld)->mid->end(0,0)->mid(pln).
const mineflayer=require('mineflayer'); const {Rcon}=require('rcon-client'); const {Vec3}=require('vec3');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=30000,st=300)=>{const t=Date.now();while(Date.now()-t<to){if(await fn())return true;await sleep(st);}return false;};
(async()=>{
 const rcon=await Rcon.connect({host:'127.0.0.1',port:25575,password:process.env.RCON_PASS});
 const bot=mineflayer.createBot({host:'127.0.0.1',port:25565,username:'ZC',auth:'offline',version:'1.21.11'});
 bot._client.on('error',()=>{});process.on('uncaughtException',()=>{});
 await new Promise(r=>bot.once('spawn',r)); await sleep(800);
 await rcon.send('op ZC'); await sleep(300);
 let sp=0; bot.on('spawn',()=>sp++); const b0=sp;
 await rcon.send('mv tp ZC l1view'); await waitFor(()=>sp>b0,12000,300); await sleep(2000);
 await rcon.send('minecraft:gamemode spectator ZC'); await sleep(300);
 const surf=async(x,z)=>{
   await rcon.send(`minecraft:tp ZC ${x} 100 ${z}`); await sleep(700);
   for(let y=100;y>=0;y--){const b=bot.blockAt(new Vec3(x+0.5,y,z+0.5)); if(b&&b.name!=='air'&&b.name!=='void_air'&&b.name!=='cave_air') return `y${y}:${b.name}`;}
   return 'VOID(brak)';
 };
 console.log('=== os Z (x=0): start~z38, end~z0, mid~z-40 ===');
 for(let z=64;z>=-76;z-=6){ console.log(` z${String(z).padStart(4)}: ${await surf(0,z)}`); }
 console.log('=== os X (z=0) przez arene ===');
 for(let x=-40;x<=40;x+=8){ console.log(` x${String(x).padStart(4)}: ${await surf(x,0)}`); }
 console.log('=== VOID rogi/daleko ===');
 for(const [x,z] of [[0,75],[0,-80],[60,0],[-60,0],[50,50],[-50,-50]]) console.log(` (${x},${z}): ${await surf(x,z)}`);
 try{await rcon.end();}catch{} bot.quit(); process.exit(0);
})().catch(e=>{console.error(e);process.exit(1);});
