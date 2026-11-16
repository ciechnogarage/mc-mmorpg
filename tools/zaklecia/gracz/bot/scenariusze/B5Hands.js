'use strict';
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');
module.exports=async({k,o,scene,w})=>{
  const raw=slot=>o.cmd(slot===-106?`minecraft:data get entity ${o.name} equipment.offhand`:
    `minecraft:data get entity ${o.name} Inventory[{Slot:${slot}b}]`);
  const reset=async()=>{
    await scene.prepare({target:false});
    await o.cmd(`zaklecia przypisz ${o.name} nic prawa`);
    await o.cmd(`zaklecia przypisz ${o.name} nic lewa`);
    await sleep(250);
  };
  const equip=async(type,id,name,dest)=>{
    await o.cmd(`mi give ${type} ${id} ${o.name} 1`);
    const atGive0=await raw(0);
    let source=await until(()=>{
      const n=k.bot.inventory.slots.findIndex((item,index)=>index>=9&&index<=44&&item?.name===name);
      return n<0?null:n;
    },`${id} w ekwipunku`,3000);
    let staged0=null,stagedBackpack=null;
    if(source===dest){
      const backpack=k.bot.inventory.slots.findIndex((item,index)=>index>=20&&index<=35&&!item);
      if(backpack<0)throw Error(`brak miejsca w plecaku dla ${id}`);
      k.e();try{await k.bot.clickWindow(source,0,0);await k.bot.clickWindow(backpack,0,0);}finally{k.zamknij();}
      staged0=await raw(0);stagedBackpack=await raw(backpack);
      source=backpack;await sleep(250);
    }
    if(source!==dest){k.e();try{await k.bot.clickWindow(source,0,0);await k.bot.clickWindow(dest,0,0);}finally{k.zamknij();}}
    await sleep(350);
    return {source,dest,atGive0:atGive0.slice(0,130),staged0:staged0?.slice(0,130),stagedBackpack:stagedBackpack?.slice(0,130)};
  };
  await w.step('B5Hands-luk-prawa-lewy-czar',async()=>{
    await reset();await assign(k,o,'plomienie','lewa');
    const leftBefore=await o.placeholder('lewa');
    const moved=await equip('BOW','LUK_MYSLIWSKI','bow',36);
    k.cyfra(1);await sleep(300);
    const right=await raw(0),left=await raw(-106),leftSpell=await o.placeholder('lewa');
    const bow=/id: "minecraft:bow"/.test(right),leftOccupied=/id: "minecraft:debug_stick"/.test(left);
    w.add('B5Hands-luk-prawa-lewy-czar',bow&&!leftOccupied,
      'łuk dwuręczny w prawej nie może mieć aktywnego nośnika w lewej',
      {moved,leftBefore,right:right.slice(0,220),left:left.slice(0,220),leftSpell},
      {right:'bow',leftPhysical:'bez nośnika'});
    const selectedBefore=await o.selected();
    await k.shiftQ();await sleep(250);k.kolko(1);await sleep(250);k.q();await sleep(250);
    const selectedAfter=await o.selected(),rightAfter=await raw(0),leftAfter=await o.placeholder('lewa'),offAfter=await raw(-106);
    w.add('B5Hands-luk-ShiftQ',/id: "minecraft:bow"/.test(rightAfter)&&!/id: "minecraft:debug_stick"/.test(offAfter),
      'Shift+Q/kółko/Q przy łuku dwuręcznym zachowuje układ',
      {selectedBefore:selectedBefore.slice(0,130),selectedAfter:selectedAfter.slice(0,130),
       rightAfter:rightAfter.slice(0,220),leftAfter,offAfter:offAfter.slice(0,220),barOpen:k.barOpen});
  });
  await w.step('B5Hands-dwureczna-lewa',async()=>{
    await reset();
    const moved=await equip('GREATSWORD','MIECZ_RUNICZNY','netherite_sword',45);
    const left=await raw(-106),right=await raw(0),slot1=await raw(1);
    const heldLeft=/id: "minecraft:netherite_sword"/.test(left);
    w.add('B5Hands-dwureczna-lewa',!heldLeft,
      'próba wyposażenia dwuręcznej broni w lewej zostaje odrzucona',
      {moved,left:left.slice(0,220),right:right.slice(0,220),slot1:slot1.slice(0,220)});
  });
  await w.step('B5Hands-luk-lewa',async()=>{
    await reset();const moved=await equip('BOW','LUK_MYSLIWSKI','bow',45);
    const left=await raw(-106);
    w.add('B5Hands-luk-lewa',!/id: "minecraft:bow"/.test(left),
      'łuk nie może trafić do lewej',{moved,left:left.slice(0,220)});
  });
  await w.step('B5Hands-tarcza-lewa-prawy-czar',async()=>{
    await reset();await assign(k,o,'plomienie','prawa');
    const moved=await equip('SHIELD','TARCZA_DREWNIANA','shield',45);
    const left=await raw(-106),rightSpell=await o.placeholder('prawa');
    w.add('B5Hands-tarcza-po-prawym-czarze',/id: "minecraft:shield"/.test(left),
      'tarcza zakładana w lewej po przypisaniu prawego czaru',
      {moved,left:left.slice(0,220),rightSpell});
    await reset();const movedFirst=await equip('SHIELD','TARCZA_DREWNIANA','shield',45);
    const leftBeforeSpell=await raw(-106);
    w.add('B5Hands-tarcza-lewa',/id: "minecraft:shield"/.test(leftBeforeSpell),
      'kontrola tarczy w lewej bez zaklęcia',
      {movedFirst,left:leftBeforeSpell.slice(0,220)});
    await assign(k,o,'plomienie','prawa');
    const leftAfterSpell=await raw(-106);
    w.add('B5Hands-tarcza-przed-prawym-czarem',/id: "minecraft:shield"/.test(leftAfterSpell),
      'tarcza w lewej po przypisaniu prawego czaru',
      {leftBeforeSpell:leftBeforeSpell.slice(0,220),leftAfterSpell:leftAfterSpell.slice(0,220),rightSpell:await o.placeholder('prawa')});
  });
  await w.step('B5Hands-tarcza-prawa',async()=>{
    await reset();k.cyfra(1);await sleep(150);
    const beforeRight=await raw(0),moved=await equip('SHIELD','TARCZA_DREWNIANA','shield',36);
    const right=await raw(0);
    console.log('B5SHIELD-RIGHT '+right);
    w.add('B5Hands-tarcza-prawa',!/id: "minecraft:shield"/.test(right),
      'tarcza nie może trafić do prawej',{moved,beforeRight:beforeRight.slice(0,220),right:right.slice(0,220),selected:(await o.selected()).slice(0,180)});
    await k.shiftQ();await sleep(200);k.kolko(1);await sleep(200);k.q();await sleep(250);
    const afterCancelled=await raw(0);
    w.add('B5Hands-tarcza-prawa-po-anulowaniu',!/id: "minecraft:shield"/.test(afterCancelled),
      'anulowana zmiana slotu powinna naprawić nieprawidłową prawą dłoń',
      {rightBefore:right.slice(0,180),rightAfter:afterCancelled.slice(0,180),barOpen:k.barOpen});
    const selectedBefore=k.bot.quickBarSlot;k.cyfra(6);await sleep(300);
    const afterAccepted=await raw(0);
    w.add('B5Hands-tarcza-prawa-po-zmianie',!/id: "minecraft:shield"/.test(afterAccepted),
      'zaakceptowana zmiana slotu odświeża układ rąk',
      {rightAfter:afterAccepted.slice(0,180),selectedBefore,selectedAfter:k.bot.quickBarSlot});
  });
};
