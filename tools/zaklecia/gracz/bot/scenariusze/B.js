'use strict';
const {sleep}=require('../lib/klient');const {assign,SLOTS,until}=require('../lib/menu');const {Vec3}=require('vec3');
const TYPES={plomienie:'koncentracja',odmrozenie:'koncentracja',ognisty_pocisk:'ladowany',kula_ognia:'ladowany',leczenie:'koncentracja',runa_ognia:'runa',oslona:'tarcza',przywolany_miecz:'bron',duch_wilka:'przywolanie',atronach_ognia:'przywolanie',leczenie_dloni:'koncentracja',uspokojenie:'na_cel',uzdrowienie_innego:'ladowany',zasklepienie_ran:'ladowany',wielkie_uzdrowienie:'ladowany'};
const latencyLimit=(type,m)=>m.latencyTicks==null?{}:{latencyTicks:['koncentracja','tarcza'].includes(type)?'≤10':type==='na_cel'?'≤20':'≤40'};
async function aim(k,id){if(id==='runa_ognia')await k.bot.lookAt(new Vec3(0.5,79.8,3.5),true);else if(id==='uspokojenie')await k.bot.lookAt(new Vec3(0.5,80.8,2.5),true);else if(['duch_wilka','atronach_ognia'].includes(id))await k.bot.lookAt(new Vec3(-1,81,5),true);else if(['leczenie','oslona','zasklepienie_ran','przywolany_miecz'].includes(id))await k.bot.look(0,0,true);else await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);await sleep(120);}
module.exports=async({k,o,scene,w,connect})=>{
  await w.step('B-postac-bez-klasy',async()=>{await scene.prepare();const cls=await o.characterClass(),mana=await o.mana();const p=await assign(k,o,'plomienie','lewa');w.add('B-postac-bez-klasy',p!=='pusta'&&mana!==null,`domyślna klasa ${cls}, mana ${mana}; pobranie Płomieni: ${p}`,{klasa:cls,mana},{mana:'20 dla Human z danych klona'});});
  await w.step('B-cel-gracz',async()=>{let target;try{
    target=await connect('MagCel');await scene.prepare({target:false});await scene.targetBot();
    await o.cmd('minecraft:effect give MagCel minecraft:instant_health 1 10 true');console.log('DIAG-B-DAMAGE '+JSON.stringify({before:await o.hp('MagCel'),response:await o.cmd('minecraft:damage MagCel 6'),after:await o.hp('MagCel')}));const hp0=await o.hp('MagCel');
    await assign(k,o,'leczenie_dloni','lewa');await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);await sleep(120);
    const s=o.snapshot();await k.ppmTrzymaj(900);const hp1=await o.hp('MagCel');
    w.add('B-cel-gracz',Number.isFinite(hp0)&&Number.isFinite(hp1)&&hp1-hp0>0.5,`leczenie drugiego gracza: ${hp0}→${hp1}`,{...o.metrics(s),hpDelta:hp1-hp0},{hpDelta:'>0,5',latencyTicks:'≤10'});
    await scene.prepare({target:false});await scene.targetBot();await o.cmd('minecraft:effect give MagCel minecraft:instant_health 1 10 true');await sleep(700);console.log('DIAG-B-DAMAGE '+JSON.stringify({before:await o.hp('MagCel'),response:await o.cmd('minecraft:damage MagCel 6'),after:await o.hp('MagCel')}));
    await assign(k,o,'uzdrowienie_innego','lewa');await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);await sleep(120);
    const hp2=await o.hp('MagCel'),s2=o.snapshot();await k.ppmTrzymaj(1250);
    await until(async()=>{const h=await o.hp('MagCel');return h>hp2+0.5?h:null},'uzdrowienie MagCel',1600).catch(()=>null);
    const hp3=await o.hp('MagCel');w.add('B-uzdrowienie-innego-cel',hp3>hp2+0.5,`ładowane uzdrowienie MagCel: ${hp2}→${hp3}`,{...o.metrics(s2),hpDelta:hp3-hp2},{hpDelta:'>0,5'});
  }finally{target?.quit();}});
  // KRYT pomocSlug: DamageCheckEvent MythicLib zapętla się przy celu-graczu w świecie PvP.
  // Flaga jest stała; tego scenariusza nie wolno uruchamiać na klonie ani produkcji.
  w.skip('B-PvP','zablokowano: przywołaniec + drugi gracz może zapętlić DamageCheckEvent; PvP wymaga poprawki pluginu');
  for(const [index,[id,type]] of Object.entries(TYPES).entries())for(const hand of ['lewa','prawa'])await w.step(`B${String(index+1).padStart(2,'0')}-${id}-${hand}`,async()=>{
    await scene.prepare();if(id==='uspokojenie')await o.cmd('minecraft:tp @e[type=minecraft:husk,limit=1,sort=nearest] 0 80 2');if(['leczenie','zasklepienie_ran','wielkie_uzdrowienie'].includes(id))await o.cmd(`minecraft:damage ${o.name} 6`);
    const p=await assign(k,o,id,hand);await sleep(450);const before=o.snapshot(),mana0=await o.mana(),h0=await o.hp('MagTest');
    await aim(k,id);
    if(id==='runa_ognia'&&hand==='lewa')console.log('DIAG-RUNA '+JSON.stringify({cel:k.target().type,blokZasieg:o.attribute('minecraft:block_interaction_range'),main:k.components(k.item()),off:k.components(k.item('off')),mainName:k.item()?.name,offName:k.item('off')?.name}));
    if(hand==='lewa')await k.ppmTrzymaj(type==='koncentracja'||type==='tarcza'?850:id==='wielkie_uzdrowienie'?1700:1200);
    else {k.lpmKlik();await sleep(type==='koncentracja'||type==='tarcza'?850:id==='wielkie_uzdrowienie'?1700:1400);if(type==='koncentracja'||type==='tarcza')k.lpmKlik();}
    if(type!=='koncentracja'&&type!=='tarcza'&&!['leczenie_dloni','uzdrowienie_innego'].includes(id))
      await until(async()=>{const n=await o.mana();return n!==null&&n<mana0-0.2},`koszt ${id}`,1300).catch(()=>null);
    const mana1=await o.mana(),h1=await o.hp('MagTest'),m=o.metrics(before);
    const cost=mana0!==null&&mana1!==null?Number((mana0-mana1).toFixed(2)):null;
    const assigned=p!=='pusta';
    const needsOther=['leczenie_dloni','uzdrowienie_innego'].includes(id);
    w.add(`B${String(index+1).padStart(2,'0')}-${id}-${hand}`,assigned&&cost!==null&&(needsOther?cost<=0.2:cost>0),`${type}: ${hand} ${hand==='lewa'?'PPM':'LPM'}; mana ${mana0}→${mana1}; HP ${h0}→${h1}${needsOther?'; brak celu — brak kosztu':''}`,{...m,manaKoszt:cost,spell:id},{...latencyLimit(type,m),setSlotPerSec:'≤20',manaKoszt:needsOther?'≤0,2 bez celu':'>0 i zgodnie z config/lore'});
  });
  for(const [index,[id,type]] of Object.entries(TYPES).entries())await w.step(`B${String(index+1).padStart(2,'0')}-${id}-oburacz`,async()=>{
    await scene.prepare();if(id==='uspokojenie')await o.cmd('minecraft:tp @e[type=minecraft:husk,limit=1,sort=nearest] 0 80 2');if(['leczenie','zasklepienie_ran','wielkie_uzdrowienie'].includes(id))await o.cmd(`minecraft:damage ${o.name} 6`);
    await assign(k,o,id,'prawa');await assign(k,o,id,'lewa');
    await aim(k,id);
    if(id==='ognisty_pocisk')console.log('DIAG-OBIE '+JSON.stringify({allow:k.allow(),using:k.using,destroying:k.destroying,screen:k.screen,window:!!k.bot.currentWindow,main:k.components(k.item()),off:k.components(k.item('off'))}));
    const s=o.snapshot(),mana0=await o.mana();
    if(id==='przywolany_miecz'){
      await sleep(350);k.ppmWcisnij();await sleep(70);k.lpmKlik();
      await until(async()=>{
        const l=await o.cmd(`papi parse ${o.name} %zaklecia_lewa_stan%`);
        const p=await o.cmd(`papi parse ${o.name} %zaklecia_prawa_stan%`);
        return l==='gotowe'&&p==='gotowe';
      },'oburącz gotowy Przywołany miecz',3500);
      await sleep(100);k.ppmPusc();await sleep(200);
    }else await k.obie(type==='koncentracja'||type==='tarcza'?850:id==='wielkie_uzdrowienie'?1700:1350);
    const needsOther=['leczenie_dloni','uzdrowienie_innego'].includes(id);
    if(type!=='koncentracja'&&type!=='tarcza'&&!needsOther)await until(async()=>{const n=await o.mana();return n!==null&&n<mana0-0.2},`koszt oburącz ${id}`,1300).catch(()=>null);
    const mana1=await o.mana(),delta=mana0!==null&&mana1!==null?mana0-mana1:null;
    const base=w.rows.find(r=>r.id===`B${String(index+1).padStart(2,'0')}-${id}-lewa`)?.FEEL?.manaKoszt;
    const ratio=base>1&&delta!==null?delta/base:null;
    const metrics=o.metrics(s);
    w.add(`B${String(index+1).padStart(2,'0')}-${id}-oburacz`,delta!==null&&(needsOther?delta<=0.2:delta>0&&(ratio===null||ratio>=2.2&&ratio<=3.4)),`${id}: obie dłonie, to samo${needsOther?' bez celu':''}`,{...metrics,manaDelta:delta,kosztDoJednej:ratio},{manaDelta:needsOther?'≤0,2 bez celu':'>0 i koszt ×2,8',...latencyLimit(type,metrics)});
  });
  await w.step('B-obie',async()=>{await scene.prepare();await assign(k,o,'plomienie','prawa');await assign(k,o,'plomienie','lewa');const s=o.snapshot(),mana0=await o.mana();await k.obie(700);await sleep(200);const mana1=await o.mana(),m=o.metrics(s);w.add('B-obie',mana0!==null&&mana1!==null&&mana1<mana0,'to samo w obu dłoniach: PPM + klik LPM',{...m,manaDelta:mana0-mana1},{...latencyLimit('koncentracja',m),setSlotPerSec:'≤20',manaDelta:'koszt ×2,8'});});
  await w.step('B-rozne',async()=>{await scene.prepare();await assign(k,o,'plomienie','prawa');await assign(k,o,'odmrozenie','lewa');const s=o.snapshot(),mana0=await o.mana();await k.obie(700);const mana1=await o.mana(),m=o.metrics(s);w.add('B-rozne',mana0!==null&&mana1!==null&&mana1<mana0,'dwa różne zaklęcia',{...m,manaDelta:mana0-mana1},{...latencyLimit('koncentracja',m),setSlotPerSec:'≤20',manaDelta:'suma kosztów'});});
  await w.step('B-brak-many',async()=>{await scene.prepare();await assign(k,o,'plomienie','lewa');const s=o.snapshot();k.ppmWcisnij();for(let i=0;i<3;i++){await sleep(180);k.ppmWcisnij();}await o.cmd(`mmocore admin resource-mana set ${o.name} 0`);for(let i=0;i<3;i++){await sleep(180);k.ppmWcisnij();}k.ppmPusc();await until(async()=>{const x=await o.cmd(`papi parse ${o.name} %zaklecia_lewa_stan%`);return x!=='rzuca'?x:null},'koniec strumienia po braku many',1000).catch(()=>null);const m=o.metrics(s),mana=await o.mana(),stan=await o.cmd(`papi parse ${o.name} %zaklecia_lewa_stan%`);w.add('B-brak-many',mana!==null&&mana<=0.3&&stan!=='rzuca','mana wyzerowana w trakcie rzucania; strumień ma zgasnąć',{...m,mana,stan},{mana:'≤0,3',stan:'gotowa'});});
  await w.step('B-sciana',async()=>{await scene.prepare({target:false});await assign(k,o,'kula_ognia','lewa');await k.bot.lookAt(new Vec3(0,81,7),true);const s=o.snapshot(),mana0=await o.mana();await k.ppmTrzymaj(1450);await until(async()=>{const n=await o.mana();return n!==null&&n<mana0-0.2},'koszt kuli w ścianę',1300).catch(()=>null);const mana1=await o.mana(),m=o.metrics(s);w.add('B-sciana',mana0!==null&&mana1!==null&&mana1<mana0,'ładowany czar w kamienną ścianę areny',{...m,manaDelta:mana0-mana1},{manaDelta:'>0',...latencyLimit('ladowany',m)});});
  const count=async type=>{const s=await o.cmd(`minecraft:execute positioned 0 80 0 if entity @e[type=minecraft:${type},distance=..12]`);return /test passed|test passed, count: (\d+)|Found (\d+)|Found 1|Test passed/i.test(s)?Number(s.match(/(\d+) matching/)?.[1]||s.match(/count: (\d+)/)?.[1]||1):0;};
  await w.step('B21-runa-limit',async()=>{
    await scene.prepare({target:false});await assign(k,o,'runa_ognia','lewa');await k.bot.lookAt(new Vec3(0.5,79.8,3.5),true);await sleep(120);
    await k.ppmTrzymaj(1050);await until(async()=>await count('armor_stand'),'pierwsza runa',2000).catch(()=>null);const one=await count('armor_stand');
    const uuid0=await o.cmd('data get entity @e[type=minecraft:armor_stand,limit=1,sort=nearest] UUID');
    const mana0=await o.mana();await k.ppmTrzymaj(1050);await until(async()=>{const m=await o.mana();return m<mana0-2?m:null},'druga runa',2000).catch(()=>null);await until(async()=>await count('armor_stand')===1?true:null,'nowa runa',2000).catch(()=>null);const two=await count('armor_stand'),uuid1=await o.cmd('data get entity @e[type=minecraft:armor_stand,limit=1,sort=nearest] UUID');
    w.add('B21-runa-limit',one===1&&two===1&&uuid0!==uuid1,`runa po pierwszym i drugim rzucie: ${one}, ${two}; zastąpienie ${uuid0!==uuid1}`,{pierwsza:one,druga:two,zastapiona:uuid0!==uuid1},{druga:'1 po zastąpieniu'});
  });
  for(const [id,type] of [['duch_wilka','wolf'],['atronach_ognia','blaze']])await w.step(`B21-${id}-limit`,async()=>{
    await scene.prepare();await assign(k,o,id,'prawa');await aim(k,id);
    k.lpmKlik();await until(async()=>await count(type),'pierwsze przywołanie',2200).catch(()=>null);const one=await count(type),uuid0=await o.cmd(`data get entity @e[type=minecraft:${type},limit=1,sort=nearest] UUID`);
    const mana0=await o.mana();k.lpmKlik();await until(async()=>{const m=await o.mana();return m<mana0-2?m:null},'drugie przywołanie',2200).catch(()=>null);
    const uuid1=await o.cmd(`data get entity @e[type=minecraft:${type},limit=1,sort=nearest] UUID`),two=await count(type);
    w.add(`B21-${id}-limit`,one===1&&two===1&&uuid0!==uuid1,`${id} z huskiem jako jedynym celem: ${one}, ${two}; zastąpienie starego ${uuid0!==uuid1}`,{pierwszy:one,drugi:two,zastapiony:uuid0!==uuid1},{drugi:'≤1'});
  });

};
module.exports.TYPES=TYPES;
