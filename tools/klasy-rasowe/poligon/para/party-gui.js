const mineflayer=require('/g/node_modules/mineflayer');const {attach}=require('/e/lib/resourcepack.js');const {rcon}=require('/s/rcon.js');const sleep=ms=>new Promise(r=>setTimeout(r,ms))
const nazwa=s=>{try{const c=s.customName;return typeof c==='string'?c:JSON.stringify(c).replace(/[^\p{L}\p{N} ]/gu,'').slice(0,40)}catch{return ''}}
const mk=async n=>{const b=mineflayer.createBot({host:process.env.MC_HOST,username:n,auth:'offline',version:'1.21.11'});attach(b._client);b.on('messagestr',m=>{if(!/❤|SKILL|joined|received|now a|Profile|Postać/.test(m))console.log(n,'<',m)});await new Promise(r=>b.once('spawn',r));await sleep(4000);await rcon(`cc testpostac ${n} czlowiek wojownik`);await sleep(14000);return b}
const okno=b=>b.currentWindow?(JSON.stringify(b.currentWindow.title).slice(0,60)+' | '+b.currentWindow.slots.slice(0,54).map((s,i)=>s?i+':'+s.name+':'+nazwa(s):null).filter(Boolean).join(' ; ')):'brak'
;(async()=>{const a=await mk('PartyC'),b=await mk('PartyD')
a.chat('/party');await sleep(2000);console.log('1',okno(a));await a.clickWindow(12,0,0);await sleep(2000);console.log('2',okno(a))
const inv=48;console.log('invite slot',inv)
if(inv>=0){await a.clickWindow(inv,0,0);await sleep(1500);a.chat('PartyD');await sleep(2500)}
b.chat('/party accept');await sleep(2500);console.log('A w party:',await rcon('papi parse PartyC %mmocore_party_count%'),'| B:',await rcon('papi parse PartyD %mmocore_party_count%'))
a.quit();b.quit();await sleep(300);process.exit(0)})()
