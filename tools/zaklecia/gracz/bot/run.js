'use strict';
const mineflayer=require('mineflayer');
const {Klient,sleep}=require('./lib/klient');const {Obserwator}=require('./lib/obserwator');
const {Scena}=require('./lib/scena');const {Wyniki}=require('./lib/wynik');
const {Niezmienniki}=require('./lib/niezmienniki');
const {assign}=require('./lib/menu');const {Vec3}=require('vec3');
const fs=require('node:fs');const {discoverGroups}=require('./lib/grupy');
const filter=process.argv[2]||'';
const selected=discoverGroups(fs.readdirSync(require('node:path').join(__dirname,'scenariusze')),filter);
if(!selected.length)throw Error(`filtr ${filter} nie pasuje do scenariusze/*.js`);
let deadline=setTimeout(()=>{console.error('FAIL limit czasu całego uruchomienia');process.exit(2)},Number(process.env.TIMEOUT_MS||1800000));
async function connect(name='MagTest'){
  const bot=mineflayer.createBot({host:process.env.MC_HOST||'zaklecia-gracz',port:Number(process.env.MC_PORT||25565),username:name,version:'1.21.11',auth:'offline'});
  bot._client.on('add_resource_pack',p=>{bot._client.write('resource_pack_receive',{uuid:p.uuid,result:3});bot._client.write('resource_pack_receive',{uuid:p.uuid,result:0});});
  await Promise.race([new Promise((resolve,reject)=>{bot.once('spawn',resolve);bot.once('error',reject);bot.once('kicked',x=>reject(Error(String(x))))}),sleep(30000).then(()=>{throw Error('spawn timeout / CONFIGURATION')})]);
  await sleep(1500);return bot;
}
(async()=>{
  const rcon=await Obserwator.connect();let failures=0;
  try{
    for(const group of selected){let bot,scene,k,fatal=false;const w=new Wyniki(group);
      try{
        bot=await connect(group.startsWith('B6')?`T${group}`:'MagTest');const o=new Obserwator(bot,rcon,bot.username);k=new Klient(bot,o);scene=new Scena(o);w.checker=new Niezmienniki(o,k,w);
        await scene.prepare();
        let control=false,controlDetail='';
        for(let attempt=1;attempt<=3&&!control;attempt++)await w.step(`${group}-kontrola-dodatnia-${attempt}`,async()=>{
          if(attempt>1)await scene.prepare();
          await assign(k,o,'plomienie','lewa');await bot.lookAt(new Vec3(0.5,80.8,5.5),true);
          const hp0=await o.hp('@e[type=minecraft:husk,limit=1,sort=nearest]'),m0=await o.mana();
          await k.ppmTrzymaj(650);
          const hp1=await o.hp('@e[type=minecraft:husk,limit=1,sort=nearest]'),m1=await o.mana();
          control=hp1<hp0&&m1<m0;
          controlDetail=`próba ${attempt}: husk ${hp0}→${hp1} HP, mana ${m0}→${m1}`;
        });
        w.add(`${group}-kontrola-dodatnia`,control,`Płomienie: ${controlDetail}`);
        await scene.prepare();
        const groupLimit=group.startsWith('MX')||/^LAT[123]$/.test(group)?1500000:group.startsWith('FZ')||group==='B5Fuzz'||group.startsWith('B6F')?1700000:['B','EDGE','DBL','STAT'].includes(group)?600000:300000;
        try{await Promise.race([require(`./scenariusze/${group}.js`)({k,o,scene,w,connect}),sleep(groupLimit).then(()=>{throw Error(`limit grupy ${group}`)})]);}
        catch(e){fatal=/Not connected|ECONNREFUSED|Server closed|Timeout for packet/i.test(String(e));w.blad(`${group}-timeout`,e);}
      }catch(e){fatal=/Not connected|ECONNREFUSED|Server closed|Timeout for packet/i.test(String(e));w.blad(`${group}-start`,e);}
      finally{
        try{if(scene&&!fatal){await scene.cleanup();await w.checker?.cleanup();}}catch(e){w.blad(`${group}-cleanup`,e);}
        try{k?.close();}catch{}
        try{bot?.quit();}catch{}w.finish();failures+=w.rows.filter(x=>x.status==='FAIL'||x.status==='BLAD').length;
        await sleep(1200);
      }
      if(fatal)break;
    }
  }finally{clearTimeout(deadline);await rcon.end().catch(()=>{});}
  process.exitCode=failures?1:0;
  setTimeout(()=>process.exit(process.exitCode),300); // mineflayer/rcon trzymaja petle zdarzen
})().catch(e=>{console.error(e.stack||e);process.exit(2)});
