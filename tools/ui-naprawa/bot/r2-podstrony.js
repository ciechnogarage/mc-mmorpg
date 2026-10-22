'use strict';
const mineflayer = require('mineflayer');
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const pages = [
  ['hub_postac','䨀'], ['hub_umiejetnosci','䨄'], ['hub_ekonomia','䨈'],
  ['hub_spolecznosc','䨌'], ['hub_ustawienia','䨐'], ['hub_pomoc','䨔'],
  ['stolica_podroze','䨘'], ['wiara','䩀'], ['ksiega_zaklec','䩄'],
  ['profession_main','䩈'], ['dziennik_przygod','䬀'], ['poczta','䬄'],
  ['chowance_menazeria','䫀'], ['chowance_hodowla','䫄'],
  ['chowance_lochy','䫈'], ['chowance_ranking','䫌'],
  ['chowance_magazyn','䫐'], ['chowance_bestiariusz','䫔']
];
const bot = mineflayer.createBot({host:'ui-klon',port:25565,username:'KsieQA',version:'1.21.11',auth:'offline'});
const errors = [];
const timer = setTimeout(() => { console.error('TIMEOUT'); process.exit(2); }, 580000);
bot._client.on('add_resource_pack', p => {
  bot._client.write('resource_pack_receive',{uuid:p.uuid,result:3});
  bot._client.write('resource_pack_receive',{uuid:p.uuid,result:0});
});
bot.on('error', e => {console.error('BOT_ERROR', e.stack || e);process.exit(2)});
bot.on('kicked', e => {console.error('KICKED',JSON.stringify(e));process.exit(2)});
async function nextWindow(old, ms=3500) {
  for (let i=0;i<ms/100;i++) {
    if (bot.currentWindow && bot.currentWindow!==old) return bot.currentWindow;
    await sleep(100);
  }
  return null;
}
async function open(command) {
  if (bot.currentWindow) {bot.closeWindow(bot.currentWindow);await sleep(180)}
  const old=bot.currentWindow;
  bot.chat(command);
  return nextWindow(old);
}
const name = item => item ? (item.displayName || item.name || '?') : '-';
(async () => {
  await Promise.race([new Promise((resolve,reject)=>{bot.once('spawn',resolve);bot.once('error',reject)}),sleep(45000).then(()=>{throw Error('NO_SPAWN')})]);
  await sleep(1000);
  const inventoryBefore=bot.inventory.items().map(i=>[i.slot,i.name,i.count]);
  for (const [id,sign] of pages) {
    const window=await open('/dm open '+id);
    if (!window) {console.log('FAIL',id,'brak okna');errors.push(id+':no-window');continue}
    await sleep(180);
    const title=JSON.stringify(window.title);
    const bottom=window.slots.slice(54,90);
    const hasBack=Boolean(bottom[3]);
    const hasPanel=Boolean(bottom[0] && bottom[1] && bottom[10] && bottom[11] && bottom[12]);
    const travelSlots=bottom.slice(18,27).filter(Boolean).length;
    const titleGood=title.includes(sign);
    if (!titleGood || !hasBack || !hasPanel) errors.push(id+':title='+titleGood+',back='+hasBack+',panel='+hasPanel);
    console.log('PAGE',id,'title='+titleGood,'panel='+hasPanel,'back='+hasBack,
      'slot64='+name(bottom[10]),'slot65='+name(bottom[11]),'slot66='+name(bottom[12]),
      'travel='+travelSlots);
    if (hasBack) {
      const old=bot.currentWindow;
      try {await bot.clickWindow(57,0,0)} catch(e) {errors.push(id+':back-click');console.log('BACK_CLICK_ERROR',id,e.message)}
      const after=await nextWindow(old,2300);
      const backGood=Boolean(after && after!==old);
      if (!backGood) errors.push(id+':back-no-window');
      console.log('BACK',id,backGood,after?JSON.stringify(after.title).slice(0,90):'-');
    }
  }
  if(bot.currentWindow){bot.closeWindow(bot.currentWindow);await sleep(300)}
  const inventoryAfter=bot.inventory.items().map(i=>[i.slot,i.name,i.count]);
  const preserved=JSON.stringify(inventoryBefore)===JSON.stringify(inventoryAfter);
  console.log('INVENTORY_UNCHANGED',preserved,JSON.stringify(inventoryBefore),JSON.stringify(inventoryAfter));
  if(!preserved)errors.push('inventory-changed');
  console.log('RESULT',pages.length,'pages',errors.length,'failures',JSON.stringify(errors));
  clearTimeout(timer);bot.quit();process.exit(errors.length?1:0);
})().catch(e=>{console.error('ERROR',e.stack||e);process.exit(2)});
