'use strict';
const {sleep}=require('../lib/klient');const {assign,until}=require('../lib/menu');const {Vec3}=require('vec3');
const items=[['miecz MMOItems MIECZ_KAMIENNY','mi give SWORD MIECZ_KAMIENNY'],['tarcza','minecraft:shield'],['jedzenie','minecraft:bread'],['mikstura','minecraft:potion'],['łuk','minecraft:bow'],['narzędzie','minecraft:iron_pickaxe'],['blok','minecraft:stone'],['mapa','minecraft:filled_map']];
const pattern=label=>label.includes('miecz')?/sword|cutlass/i:label.includes('tarcza')?/shield/i:label.includes('jedzenie')?/bread/i:label.includes('mikstura')?/potion/i:label.includes('łuk')||label.includes('luk')?/bow/i:label.includes('narzędzie')?/pickaxe/i:label.includes('blok')?/stone/i:/filled_map/i;
async function equip(k,label){const re=pattern(label);const slot=await until(()=>{const i=k.bot.inventory.slots.findIndex((x,n)=>n>=9&&n<=44&&n!==43&&x&&re.test(`${x.name} ${x.displayName}`));return i<0?null:i},`przedmiot ${label}`);
  const dest=label.includes('miecz')?36:41;
  if(slot!==dest){if(k.bot.currentWindow)k.zamknij();await until(()=>!k.bot.currentWindow&&!k.screen,'zamknięcie przed ekwipunkiem');k.e();try{await k.bot.clickWindow(slot,0,0);await k.bot.clickWindow(dest,0,0);}finally{k.zamknij();}}
  await until(()=>!k.bot.currentWindow&&!k.screen,'zamknięcie ekwipunku');
  const item=await until(()=>{const x=k.bot.inventory.slots[dest];return x&&re.test(`${x.name} ${x.displayName}`)?x:null},`${label} w docelowym slocie ${dest}`,1800);
  let server=null;
  for(let attempt=0;attempt<4&&!server;attempt++){
    k.cyfra(dest-35);
    server=await until(async()=>{const s=await k.o.selected();return re.test(s)?s:null},`${label} wybrany na serwerze`,1000).catch(()=>null);
  }
  const serverDest=await k.o.cmd(`minecraft:data get entity ${k.o.name} Inventory[{Slot:${dest-36}b}]`);
  return {slot,server,clientDest:item.name,serverDest};
}
module.exports=async({k,o,scene,w})=>{
  for(const [label,item] of items)await w.step(`C-${label}`,async()=>{
    await scene.prepare();if(label.includes('miecz'))await o.cmd(`mmocore admin attribute give ${o.name} bieglosc-miecz 1`);await assign(k,o,'plomienie','lewa');
    const zwykly=!label.includes('miecz')&&label!=='tarcza';
    const cmd=zwykly?`minecraft:item replace entity ${o.name} hotbar.5 with ${item}`:
      item.startsWith('mi ')?`${item} ${o.name} 1`:`minecraft:give ${o.name} ${item} 1`;
    const give=await o.cmd(cmd);
    let equipped;
    if(zwykly){
      k.cyfra(6);
      const server=await until(async()=>{const selected=await o.selected();return pattern(label).test(selected)?selected:null},`${label} w slocie 5`);
      equipped={slot:'hotbar.5',server,serverDest:await o.cmd(`minecraft:data get entity ${o.name} Inventory[{Slot:5b}]`)};
    }else equipped=await equip(k,label);
    if(label==='tarcza')console.log('DIAG-C-SLOT '+JSON.stringify({quick:k.bot.quickBarSlot,held:k.bot.heldItem?.name,slot0:k.bot.inventory.slots[36]?.name,slot1:k.bot.inventory.slots[37]?.name,selected:await o.selected(),server0:await o.cmd(`data get entity ${o.name} Inventory[{Slot:0b}]`),server1:await o.cmd(`data get entity ${o.name} Inventory[{Slot:1b}]`)}));
    const s=o.snapshot(),mana0=await o.mana();await k.ppmTrzymaj(650);const m=o.metrics(s),hand=await o.placeholder('lewa'),mana1=await o.mana();
    const selected=await o.selected();w.add(`C-${label}`,hand!=='pusta'&&!!equipped.server&&pattern(label).test(selected),`PPM przy ${label} w prawej i czarze w lewej; give=${give.slice(0,80)}`,{...m,zrodlo:equipped.slot,slotPo:k.bot.quickBarSlot,heldPo:k.bot.heldItem?.name,selected:selected.slice(0,100),poWyborze:equipped.server?.slice(0,100)||'brak potwierdzenia',slotSerwera:equipped.serverDest.slice(0,130),manaDelta:mana0!==null&&mana1!==null?mana0-mana1:null},{...(m.latencyTicks==null?{}:{latencyTicks:'≤10'})});
  });
  for(const [label,item] of [['tarcza-L','minecraft:shield'],['jedzenie-L','minecraft:bread'],['mikstura-L','minecraft:potion'],['luk-L','minecraft:bow'],['blok-L','minecraft:stone']])await w.step(`C-${label}`,async()=>{
    await scene.prepare();await assign(k,o,'plomienie','prawa');
    await o.cmd(`minecraft:give ${o.name} ${item} 1`);await equip(k,label);const from=k.bot.quickBarSlot+36;k.e();await k.bot.clickWindow(from,0,0);await k.bot.clickWindow(45,0,0);k.zamknij();k.cyfra(1);await k.bot.look(0,-0.5,true);
    const off=k.bot.inventory.slots[45],s=o.snapshot(),mana0=await o.mana();
    const rightBefore=await o.placeholder('prawa'),selectedBefore=await o.selected();
    k.ppmWcisnij();await sleep(450);const lpmSent=k.lpmKlik();k.ppmPusc();await sleep(label==='blok-L'?750:200);const mana1=await o.mana();
    const rightAfter=await o.placeholder('prawa'),selectedAfter=await o.selected();
    const delta=mana0!==null&&mana1!==null?mana0-mana1:null;
    const cast=label==='blok-L';
    const m=o.metrics(s);w.add(`C-${label}`,!!off&&delta!==null&&(cast?delta>0:Math.abs(delta)<0.1),`${label} w lewej; ${cast?'LPM prawej ma działać':'isUsingItem powinien połknąć LPM'}` ,{...m,offhand:off?.name,manaDelta:delta,rightBefore,rightAfter,selectedBefore:selectedBefore.slice(0,130),selectedAfter:selectedAfter.slice(0,130),lpmSent},{offhand:item,manaDelta:cast?'>0':'0 przy połknięciu LPM'});
  });
  await w.step('C-bron-konflikt',async()=>{
    const husk='@e[type=minecraft:husk,tag=C01Target,limit=1,sort=nearest]';
    const mark=async()=>o.cmd('minecraft:execute in minecraft:overworld positioned 0 80 5 run tag @e[type=minecraft:husk,distance=..1,limit=1,sort=nearest] add C01Target');
    const fresh=async z=>{await o.cmd('minecraft:tag @e[type=minecraft:husk,tag=C01Target] remove C01Target');const killed=await o.cmd('minecraft:execute in minecraft:overworld positioned 0 80 0 run minecraft:kill @e[type=minecraft:husk,distance=..12]');await sleep(300);await o.cmd(`minecraft:summon minecraft:husk 0 80 ${z} {NoAI:1b,PersistenceRequired:1b,Tags:["C01Target"]}`);await sleep(700);console.log('DIAG-C01-FRESH '+JSON.stringify({z,killed,count:await o.cmd('minecraft:execute in minecraft:overworld positioned 0 80 0 if entity @e[type=minecraft:husk,distance=..12]')}));};
    await scene.prepare();await mark();const grant=await o.cmd(`mmocore admin attribute give ${o.name} bieglosc-miecz 1`);console.log('DIAG-C01-GRANT '+grant);
    await assign(k,o,'odmrozenie','lewa');await o.cmd(`minecraft:tp ${husk} 0 80 2`);await until(()=>Object.values(k.bot.entities).some(e=>e.name==='husk'&&e.position.distanceTo(new Vec3(0.5,80,2.5))<1),'husk kontrolny w zasięgu',2000);await k.bot.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(150);
    const kontrola0=await o.hp(husk);await k.ppmTrzymaj(850);const kontrola1=await o.hp(husk);
    await scene.prepare();await mark();await assign(k,o,'odmrozenie','lewa');await o.cmd(`mi give SWORD MIECZ_KAMIENNY ${o.name} 1`);
    const equipped=await equip(k,'miecz MMOItems');await o.cmd(`minecraft:tp ${husk} 0 80 2`);await until(()=>Object.values(k.bot.entities).some(e=>e.name==='husk'&&e.position.distanceTo(new Vec3(0.5,80,2.5))<1),'husk w zasięgu miecza',2000);await k.bot.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(800);
    console.log('DIAG-C01-TARGET '+JSON.stringify({cel:k.target().type,odleglosc:k.target().distance,zasieg:k.target().attackRange,komponent:o.attackRange(k.item()),atrybut:o.attribute('minecraft:entity_attack_range'),gracz:await o.cmd(`data get entity ${o.name} Pos`),obrot:await o.cmd(`data get entity ${o.name} Rotation`),husk:await o.cmd(`data get entity ${husk} Pos`)}));
    console.log('DIAG-C01-ITEM '+(await o.selected()));
    const melee=o.snapshot(),hp0=await o.hp(husk);for(let i=0;i<3;i++){k.lpmKlik();await sleep(700);}const hp1=await o.hp(husk);
    w.add('C01-LPM-seria',!!equipped.server&&Number.isFinite(hp0)&&Number.isFinite(hp1)&&hp1<hp0,'MIECZ_KAMIENNY z biegłością 1: trzy LPM na husku',{...o.metrics(melee),hpDelta:hp0-hp1,wybrany:equipped.server?.slice(0,75),kontrola:kontrola0-kontrola1},{hpDelta:'>0'});
    await fresh(2);await k.bot.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(200);
    console.log('DIAG-C01-PO-LPM-CEL '+JSON.stringify({gracz:await o.cmd(`data get entity ${o.name} Pos`),obrot:await o.cmd(`data get entity ${o.name} Rotation`),husk:await o.cmd(`data get entity ${husk} Pos`),cel:k.target().type,d:k.target().distance,offhand:k.item('off')?.name,held:k.bot.heldItem?.name}));
    const bezPrzerwy0=await o.hp(husk),bezPrzerwyMana0=await o.mana();await k.ppmTrzymaj(850);const bezPrzerwy1=await o.hp(husk),bezPrzerwyMana1=await o.mana();
    console.log('DIAG-C01-PO-LPM '+JSON.stringify({hp0:bezPrzerwy0,hp1:bezPrzerwy1,odRazUHP:bezPrzerwy0-bezPrzerwy1,odRazUMana:bezPrzerwyMana0-bezPrzerwyMana1}));
    await sleep(2200);await fresh(2);await k.bot.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(200);
    console.log('DIAG-C01-PO-LPM-2S-CEL '+JSON.stringify({gracz:await o.cmd(`data get entity ${o.name} Pos`),obrot:await o.cmd(`data get entity ${o.name} Rotation`),husk:await o.cmd(`data get entity ${husk} Pos`),cel:k.target().type,d:k.target().distance,offhand:k.item('off')?.name,held:k.bot.heldItem?.name}));
    const poPrzerwie0=await o.hp(husk),poPrzerwieMana0=await o.mana();await k.ppmTrzymaj(850);const poPrzerwie1=await o.hp(husk),poPrzerwieMana1=await o.mana();
    console.log('DIAG-C01-PO-LPM-2S '+JSON.stringify({hp0:poPrzerwie0,hp1:poPrzerwie1,hpDelta:poPrzerwie0-poPrzerwie1,manaDelta:poPrzerwieMana0-poPrzerwieMana1}));
    const wolny=k.bot.inventory.slots.findIndex((x,n)=>n>=9&&n<=35&&!x);if(wolny<0)throw Error('brak miejsca na kontrolę miecza bez czaru');
    k.e();await k.bot.clickWindow(45,0,0);await k.bot.clickWindow(wolny,0,0);k.zamknij();
    await fresh(2);await k.bot.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(800);
    const sam0=await o.hp(husk),samWybrany=await o.selected(),samCel=k.target(),samZasieg=await o.cmd(`minecraft:attribute ${o.name} minecraft:entity_interaction_range get`),samAtak=await o.cmd(`minecraft:attribute ${o.name} minecraft:entity_attack_range get`);k.lpmKlik();await sleep(750);const sam1=await o.hp(husk);
    w.add('C01-miecz-bez-czaru',sam1<sam0,'Kontrola MIECZ_KAMIENNY bez zaklęcia w lewej dłoni',{hpDelta:sam0-sam1,wybrany:samWybrany.slice(0,75),offhand:k.item('off')?.name||'pusta',cel:samCel.type,odleglosc:samCel.distance,zasiegSerwera:samZasieg,zasiegAtaku:samAtak},{hpDelta:'>0'});
    await scene.prepare();await mark();await o.cmd(`minecraft:item replace entity ${o.name} hotbar.6 with minecraft:stone_sword`);k.cyfra(7);await until(async()=>/minecraft:stone_sword/.test(await o.selected()),'zwykły miecz wybrany',1000).catch(()=>null);await o.cmd(`minecraft:tp ${husk} 0 80 2`);await k.bot.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(800);
    const wan0=await o.hp(husk),wanSelected=await o.selected(),wanCel=k.target(),wanZasieg=await o.cmd(`minecraft:attribute ${o.name} minecraft:entity_interaction_range get`),wanAtak=await o.cmd(`minecraft:attribute ${o.name} minecraft:entity_attack_range get`);k.lpmKlik();await sleep(750);const wan1=await o.hp(husk);
    w.add('C01-miecz-wanilia',/minecraft:stone_sword/.test(wanSelected)&&wan1<wan0,'Kontrola zwykłym kamiennym mieczem bez czaru',{hpDelta:wan0-wan1,wybrany:wanSelected.slice(0,75),cel:wanCel.type,odleglosc:wanCel.distance,zasiegSerwera:wanZasieg,zasiegAtaku:wanAtak},{wybrany:'minecraft:stone_sword',hpDelta:'>0'});
    await scene.prepare();await mark();await assign(k,o,'odmrozenie','lewa');await o.cmd(`mi give SWORD MIECZ_KAMIENNY ${o.name} 1`);const equipped2=await equip(k,'miecz MMOItems');await o.cmd(`minecraft:tp ${husk} 0 80 2`);await k.bot.lookAt(new Vec3(0.5,80.8,2.5),true);await sleep(200);
    const charge=o.snapshot(),cm0=await o.mana(),cs0=await o.stamina(),spellHp0=await o.hp(husk);await k.ppmTrzymaj(850);const cm1=await o.mana(),cs1=await o.stamina(),spellHp1=await o.hp(husk);
    w.add('C01-PPM-obrazenia',cm1<cm0&&spellHp1<spellHp0,'MIECZ_KAMIENNY PPM: Odmrożenie z lewej ma zadać obrażenia',{...o.metrics(charge),manaDelta:cm0-cm1,staminaDelta:cs0-cs1,hpDelta:spellHp0-spellHp1,wybrany:equipped2.server?.slice(0,75)},{manaDelta:'>0',hpDelta:'>0'});
    await fresh(5);await until(()=>Object.values(k.bot.entities).some(e=>e.name==='husk'&&e.position.distanceTo(new Vec3(0.5,80,5.5))<1),'husk w zasięgu zaklęcia',2000);await k.bot.lookAt(new Vec3(0.5,80.8,5.5),true);await sleep(200);
    const daleko0=await o.hp(husk),dm0=await o.mana();await k.ppmTrzymaj(850);const daleko1=await o.hp(husk),dm1=await o.mana();
    w.add('C01-PPM-zasieg5',daleko1<daleko0&&dm1<dm0,'Miecz w prawej, zaklęcie w lewej, cel dokładnie pod celownikiem na 5 bloków',{hpDelta:daleko0-daleko1,manaDelta:dm0-dm1},{hpDelta:'>0',manaDelta:'>0'});
    const s=o.snapshot(),mana0=await o.mana(),stamina0=await o.stamina();
    k.shift(true);await k.ppmTrzymaj(700);k.shift(false);
    const mana1=await o.mana(),stamina1=await o.stamina(),dm=mana0!==null&&mana1!==null?mana0-mana1:null,ds=stamina0!==null&&stamina1!==null?stamina0-stamina1:null;
    w.add('C01-ShiftPPM-blok',dm!==null&&ds!==null&&!(dm>0&&ds>0),'Shift+PPM: blok MIECZ_KAMIENNY vs czar lewej, bez podwójnego kosztu',{...o.metrics(s),manaDelta:dm,staminaDelta:ds},{manaDelta:'nie oba zasoby naraz',staminaDelta:'nie oba zasoby naraz'});
  });
  await w.step('C-blok-PPM',async()=>{await scene.prepare();await assign(k,o,'plomienie','lewa');await o.cmd(`minecraft:give ${o.name} minecraft:stone 1`);await o.cmd('minecraft:setblock 0 80 3 minecraft:stone');await equip(k,'blok');await k.bot.lookAt(new Vec3(0,80.5,3),true);const s=o.snapshot();k.ppmWcisnij();k.ppmPusc();await sleep(350);const m=o.metrics(s);w.add('C-blok-PPM',m.inventoryChanged,'blok przy graczu: pierwszeństwo interakcji',m,{latencyTicks:'≤10'});if(k.bot.currentWindow)k.zamknij();});
  await w.step('C-stale-sloty',async()=>{if(k.bot.currentWindow)k.zamknij();await scene.prepare();k.cyfra(9);const a=await o.selected();k.cyfra(1);k.e();const b=k.bot.inventory.slots[18],s=await o.cmd(`minecraft:data get entity ${o.name} Inventory[{Slot:18b}]`);k.zamknij();const hasSakwa=/Slot: 18b, id: "minecraft:leather"/.test(s)&&b?.name==='leather';w.add('C-stale-sloty',/minecraft:book/.test(a)&&hasSakwa,'Dziennik slot 8, Sakwa slot 18',{dziennik:a.slice(0,160),sakwa:hasSakwa,slot18:b?.name||null,serwer18:s.slice(0,250)},{dziennik:'obecny',sakwa:true});});
};
