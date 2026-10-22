'use strict';
const {sleep}=require('../lib/klient');
module.exports=async({o,w})=>{
  await w.step('N2-kontrola-przeladowanie-bazowe',async()=>{
    const reload=await o.cmd('mythicmobs reload');
    await sleep(1200);
    const ok=!/unknown command|error|exception/i.test(reload);
    w.add('N2-kontrola-przeladowanie-bazowe',ok,
      'przeładowano przywrócone bazowe skille MythicMobs na klonie',
      {reload:reload.slice(0,250)},{ok:true});
  });
};
