'use strict';
const {sleep}=require('./klient');
const SLOTS={plomienie:23,odmrozenie:24,ognisty_pocisk:25,kula_ognia:32,leczenie:33,runa_ognia:34,oslona:41,przywolany_miecz:42,duch_wilka:43,atronach_ognia:50,leczenie_dloni:51,uspokojenie:52,uzdrowienie_innego:14,zasklepienie_ran:15,wielkie_uzdrowienie:16};
const NAMES={plomienie:'Płomienie',odmrozenie:'Odmrożenie',ognisty_pocisk:'Ognisty pocisk',kula_ognia:'Kula ognia',leczenie:'Leczenie',runa_ognia:'Runa ognia',oslona:'Osłona',przywolany_miecz:'Przywołany miecz',duch_wilka:'Duch wilka',atronach_ognia:'Atronach ognia',leczenie_dloni:'Leczenie dłoni',uspokojenie:'Uspokojenie',uzdrowienie_innego:'Uzdrowienie innego',zasklepienie_ran:'Zasklepienie ran',wielkie_uzdrowienie:'Wielkie uzdrowienie'};
async function until(test,label,timeout=3500){const end=Date.now()+timeout;do{const v=await test();if(v)return v;await sleep(50);}while(Date.now()<end);throw Error(`timeout stanu: ${label}`);}
async function windowWith(bot,slot,label){return until(()=>{const w=bot.currentWindow;return w?.slots?.[slot]?w:null;},`okno ${label}, slot ${slot}`);}
async function click(bot,slot,button=0,mode=0){const w=await windowWith(bot,slot,'przed kliknięciem');if(!w.slots[slot])throw Error(`pusty slot ${slot}`);await bot.clickWindow(slot,button,mode);}
async function bookOnce(k){
  if(k.bot.currentWindow)k.zamknij();
  // Serwer nie potwierdza clientbound zamknięcia okna 0; czekamy na kolejny
  // znacznik stanu ekwipunku po close, z ograniczonym awaryjnym odstępem.
  if(k.closedAt && Date.now()-k.closedAt<250)await sleep(250-(Date.now()-k.closedAt));
  if(!k.q())throw Error('Q nie wysłane');
  const hub=await windowWith(k.bot,20,'hub');await click(k.bot,20);
  await until(()=>k.bot.currentWindow&&k.bot.currentWindow!==hub&&k.bot.currentWindow.slots?.[25], 'Umiejętności');
  // 22 ze zlecenia to w klonie slot skilli; właściwy wpis Księgi jest w 25.
  const skills=k.bot.currentWindow;await click(k.bot,25);
  await until(()=>k.bot.currentWindow&&k.bot.currentWindow!==skills&&k.bot.currentWindow.slots?.[23]?.name==='trial_key','Księga');
}
async function book(k){let last;for(let attempt=0;attempt<2;attempt++){try{return await bookOnce(k);}catch(e){last=e;if(k.bot.currentWindow)k.zamknij();await sleep(250);}}throw last;}
async function assign(k,o,id,hand='prawa'){
  if(!(id in SLOTS))throw Error(`nieznane zaklęcie ${id}`);
  await book(k);await click(k.bot,SLOTS[id],hand==='lewa'?1:0);
  const p=await until(async()=>{const v=await o.placeholder(hand);return v===NAMES[id]?v:null;},`przypisanie ${id} do ${hand}`);
  k.zamknij();return p;
}
async function favorite(k,id){const state=await k.o.cmd(`papi parse ${k.o.name} %zaklecia_ulubione_${id}%`);if(state.includes('★'))return;await book(k);const s=k.o.snapshot();await click(k.bot,SLOTS[id],0,1);await until(()=>k.o.events.slice(s.index).some(x=>x.packet==='set_slot'||x.packet==='window_items'),'ulubione');k.zamknij();}
module.exports={SLOTS,NAMES,book,assign,favorite,click,until,windowWith};
