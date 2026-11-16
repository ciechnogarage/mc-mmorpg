'use strict';
const {sleep,Klient}=require('../lib/klient');
const {Obserwator}=require('../lib/obserwator');
module.exports=async({o,w,connect})=>{
  let fresh;
  try{
    fresh=await connect('MagSakwaR9D');
    const obs=new Obserwator(fresh,o.rcon,fresh.username),client=new Klient(fresh,obs);
    await obs.cmd(`minecraft:execute in minecraft:overworld run minecraft:tp ${fresh.username} 0 80 0`);
    await sleep(1600);
    const slot=async n=>obs.cmd(`minecraft:data get entity ${fresh.username} Inventory[{Slot:${n}b}]`);
    const before={s7:await slot(7),s8:await slot(8),s18:await slot(18)};
    w.add('N_sakwa-nowy-gracz',/Slot: 18b, id: "minecraft:leather"/.test(before.s18),
      'nowe konto otrzymuje Sakwe w slocie 18 bez utraty mapy i dziennika',before);
    if(!/Slot: 18b, id: "minecraft:leather"/.test(before.s18))return;
    client.e();try{await fresh.clickWindow(18,0,0);await sleep(650);}finally{client.zamknij();}
    const opened=obs.events.some(e=>e.packet==='open_window'&&/sakwa|vault|schowek/i.test(JSON.stringify(e.data)));
    w.add('N_sakwa-klik',opened,'klik Sakwy otwiera schowek',{opened,windows:obs.events.filter(e=>e.packet==='open_window').map(e=>JSON.stringify(e.data).slice(0,300))});
    const after={s7:await slot(7),s8:await slot(8),s18:await slot(18)};
    w.add('N_sakwa-bez-strat',before.s7===after.s7&&before.s8===after.s8&&before.s18===after.s18,
      'mapa, dziennik i Sakwa pozostaja po kliknieciu',{before,after});
    client.close();
  }finally{fresh?.quit();}
};
