'use strict';
const mineflayer = require('mineflayer');
const sleep = ms => new Promise(r => setTimeout(r, ms));
const bot = mineflayer.createBot({host:'ui-klon',port:25565,username:'KsieQA',version:'1.21.11',auth:'offline'});
bot._client.on('add_resource_pack',p=>{
  bot._client.write('resource_pack_receive',{uuid:p.uuid,result:3});
  bot._client.write('resource_pack_receive',{uuid:p.uuid,result:0});
});
const seen = [];
let updates = 0;
bot._client.on('set_slot', () => updates++);
bot._client.on('window_items', () => updates++);
bot.on('messagestr', s => { if (/unknown|nie|error|błąd|zakaz/i.test(s)) console.log('CHAT',s.slice(0,150)); });
bot.on('windowOpen', w => {
  seen.push(w);
  console.log('OKNO',seen.length,JSON.stringify({title:w.title,slots:w.slots.length,
    items:w.slots.slice(0,54).flatMap((it,i)=>it?[`${i}:${it.displayName||it.name}`]:[]).slice(0,35)}));
});
async function waitWindow(before, label) {
  for(let i=0;i<70;i++){if(bot.currentWindow&&bot.currentWindow!==before)return bot.currentWindow;await sleep(100);}
  console.log('BRAK_OKNA',label);return null;
}
async function open(command,label){
  if(bot.currentWindow){bot.closeWindow(bot.currentWindow);await sleep(300);}
  const before=bot.currentWindow;bot.chat(command);return waitWindow(before,label);
}
async function click(slot,label){
  const before=bot.currentWindow;if(!before?.slots[slot]){console.log('PUSTY',label,slot);return null;}
  await bot.clickWindow(slot,0,0);return waitWindow(before,label);
}
(async()=>{
  await Promise.race([
    new Promise((resolve,reject)=>{bot.once('spawn',resolve);bot.once('error',reject);}),
    sleep(20000).then(()=>{throw Error('Brak wejścia do gry przez 20 s');})
  ]);
  await sleep(1700);
  console.log('SPAWN',bot.entity.position.toString(),bot.game?.gameMode);
  const hub=await open('/menu','Q');
  if(hub){
    let sub=await click(20,'Umiejętności');
    if(sub){let book=await click(25,'Księga');if(book){let x=updates;await sleep(3600);console.log('ODSWIEZENIA_KSIEGA_3_6S',updates-x);}}
    await open('/menu','Q2');
    sub=await click(7,'Dziennik');
    if(sub){await click(24,'Menażeria z Dziennika');}
    await open('/menu','Q3');
    await click(43,'Portal');
  }
  await open('/quests','Questy');
  bot.quit();
})().catch(e=>{console.error('BLAD',e.stack||e);bot.quit();process.exitCode=1});
