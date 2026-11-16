'use strict';
const {sleep}=require('../lib/klient');
const {assign,until}=require('../lib/menu');
const {Vec3}=require('vec3');

module.exports=async({k,o,scene,w,connect})=>{
  w.skip('N1-PvP-wilk', 'harness: przywolaniec + drugi gracz zapetla pomocSlug na klonie');
  w.skip('N8-komunikat-walka', 'zalezny od niebezpiecznej sondy N1-PvP-wilk');
  await w.step('N3-slot0-tarcza',async()=>{
    await scene.prepare({target:false});
    await assign(k,o,'plomienie','lewa');
    await o.cmd(`minecraft:item replace entity ${o.name} hotbar.0 with minecraft:shield`);
    k.cyfra(1);
    await until(async()=>/minecraft:shield/.test(await o.selected()),'tarcza w slocie 0');
    await k.ppmTrzymaj(650);
    const selected=await o.selected();
    w.add('N3-slot0-tarcza',/minecraft:shield/.test(selected),'PPM nie zmienia obcego przedmiotu na nosnik',{selected:selected.slice(0,130)},{selected:'minecraft:shield'});
  });
  await w.step('N3-slot5-chleb',async()=>{
    await scene.prepare({target:false});
    await assign(k,o,'plomienie','lewa');
    await o.cmd(`minecraft:item replace entity ${o.name} hotbar.5 with minecraft:bread`);
    k.cyfra(6);
    await until(async()=>/minecraft:bread/.test(await o.selected()),'chleb w slocie 5');
    const s=o.snapshot();
    await k.ppmTrzymaj(650);
    const selected=await o.selected();
    const zmiany=o.events.slice(s.index).filter(e=>e.packet==='held_item_slot')
      .map(e=>e.data.slotId??e.data.slot);
    w.add('N3-slot5-chleb',/minecraft:bread/.test(selected),
      'PPM zwykłym przedmiotem nie zmienia slotu i nie wybiera nośnika',
      {selected:selected.slice(0,130),slotKlienta:k.bot.quickBarSlot,zmianySlotu:zmiany},
      {selected:'minecraft:bread',zmianySlotu:'brak'});
  });
  await w.step('N4-pasek-pakiety',async()=>{
    await scene.prepare({target:false});
    const s=o.snapshot();
    for(let i=0;i<5;i++){await k.shiftQ();k.kolko(1);await k.shiftQ();}
    const m=o.metrics(s);
    const bars=o.events.slice(s.index).filter(e=>e.packet==='boss_bar');
    const nasze=new Set(bars.filter(e=>e.data.action===0&&/✦|│/.test(JSON.stringify(e.data.title)))
      .map(e=>e.data.entityUUID));
    const bossBarZaklec=bars.filter(e=>nasze.has(e.data.entityUUID)).length;
    const grupy={};
    for(const e of bars){const id=e.data.entityUUID;const g=grupy[id]??={ile:0,akcje:{},tekst:''};g.ile++;g.akcje[e.data.action]=(g.akcje[e.data.action]||0)+1;
      if(e.data.action===0){const t=JSON.stringify(e.data.title);g.tekst=t.includes('mythichud:')?'MythicHUD':t.includes('✦')?'Zaklecia':'inny';}}
    console.log('DIAG-N4 '+JSON.stringify({bossBarZaklec,bossBarPozostale:m.bossBars-bossBarZaklec,liczbaPaskow:nasze.size,grupy}));
    w.add('N4-pasek-pakiety',bossBarZaklec<=40&&m.messages<=10&&m.sounds<=15,
      'pięć otwarć, przewinięć i zamknięć paska',
      {bossBarZaklec,bossBarPozostale:m.bossBars-bossBarZaklec,messages:m.messages,sounds:m.sounds},
      {bossBarZaklec:'<=40',messages:'<=10',sounds:'<=15'});
  });
};
