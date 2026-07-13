// Census bloków w zaladowanym swiecie MV `l1view` (kopia template). Normalny swiat -> blockAt
// wiarygodny gdy chunk zaladowany. Bot mvtp, spectator, czeka na chunki, czyta siatke.
const mineflayer=require('mineflayer'); const {Rcon}=require('rcon-client'); const {Vec3}=require('vec3');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=30000,st=300)=>{const t=Date.now();while(Date.now()-t<to){if(await fn())return true;await sleep(st);}return false;};
(async()=>{
 const rcon=await Rcon.connect({host:'127.0.0.1',port:25575,password:process.env.RCON_PASS});
 const bot=mineflayer.createBot({host:'127.0.0.1',port:25565,username:'Census',auth:'offline',version:'1.21.11'});
 bot._client.on('error',()=>{});process.on('uncaughtException',()=>{});
 await new Promise(r=>bot.once('spawn',r)); await sleep(800);
 await rcon.send('op Census'); await sleep(400);
 let sp=0; bot.on('spawn',()=>sp++); const b0=sp;
 await rcon.send('mv tp Census l1view'); await waitFor(()=>sp>b0,12000,300); await sleep(2500);
 await rcon.send('minecraft:gamemode spectator Census'); await sleep(400);
 const sx=Math.floor(bot.entity.position.x), sy=Math.floor(bot.entity.position.y), sz=Math.floor(bot.entity.position.z);
 console.log('SPAWN l1view=',bot.entity.position.toString(),'dim=',bot.game.dimension);
 console.log('mv info spawn:', (await rcon.send('mv info l1view')).replace(/\n/g,' ').slice(0,300));
 await sleep(2500);
 const at=(x,y,z)=>{const b=bot.blockAt(new Vec3(x+0.5,y,z+0.5));return b?b.name:'?';};
 console.log('=== PION w SPAWN (',sx,',',sz,') ===');
 for(let y=sy+26;y>=sy-6;y-=3) console.log(` y${y}: ${at(sx,y,sz)}`);
 console.log('=== POWIERZCHNIA os Z przez spawn (x'+sx+') ===');
 for(let z=sz+14;z>=sz-30;z-=2) console.log(` z${z}: ${at(sx,sy,z)} / y+1:${at(sx,sy+1,z)}`);
 // profil pionowy w centrum drzewa (0,-17)
 console.log('=== PION DRZEWO (0,y,-17) ===');
 for(let y=88;y>=58;y-=4) console.log(` y${y}: ${at(0,y,-17)}`);
 // przekroj poziomy powierzchni y64 wzdluz osi z (od wejscia z12 do drzewa z-24)
 console.log('=== POWIERZCHNIA os Z (x0, y64) ===');
 for(let z=14;z>=-26;z-=2) console.log(` z${z}: ${at(0,64,z)}  (y65:${at(0,65,z)})`);
 // przekroj poziomy y64 wzdluz osi x
 console.log('=== POWIERZCHNIA os X (y64, z0) ===');
 for(let x=-28;x<=28;x+=4) console.log(` x${x}: ${at(x,64,0)}`);
 // co jest daleko (poza r26) - czy Iris
 console.log('=== POZA GAJEM ===');
 for(const [x,z] of [[0,30],[30,0],[0,-30],[35,35]]) console.log(` (${x},${z}) y64=${at(x,64,z)} y70=${at(x,70,z)} y75=${at(x,75,z)}`);
 try{await rcon.end();}catch{} bot.quit(); process.exit(0);
})().catch(e=>{console.error(e);process.exit(1);});
