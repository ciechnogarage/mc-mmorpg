'use strict';
// Okna MMOCore otwierane komendą gracza bez OP mają polski tytuł i teksty przedmiotów (16.11, tłumaczenie gui/*.yml):
//   /class      -> gui/class-select.yml                          ("Wybór klasy")
//   /waypoints  -> gui/waypoints.yml                             ("Punkty podróży")
//   /skilltrees <id> -> gui/specific-skill-tree/...-default.yml  ("Drzewko: ...")
// Okno potwierdzenia klasy sprawdza specjalizacja-kafel.js (krok "potwierdzenie-po-polsku").
// Uruchom: /opt/docker/mc/narzedzia/bot/run.sh <klon --z-kreatorem> <ten plik>
// Klon nie ma bazy LuckPerms: lp group default permission set mmocore.class-select|mmocore.waypoints|mmocore.skilltrees true
const { scenariusz, postac, obserwuj, sleep, czekajNaOkno, tytulOkna, sloty } = require('/bot/lib');

const czysc = s => String(s ?? '').replace(/§./g, '').trim();
const teksty = n => { if (n == null) return ''; if (typeof n === 'string') { try { return teksty(JSON.parse(n)); } catch { return n; } }
  if (Array.isArray(n)) return n.map(teksty).join(''); if (typeof n === 'object') { if (n.type === 'string') return n.value;
    if ('value' in n && n.type) return teksty(n.value); return (n.text ? teksty(n.text) : '') + (n.extra ? teksty(n.extra) : ''); } return ''; };
const ANG = /\b(Class Selection|Waypoints?|Locked|Next Page|Previous Page|Skill Tree|Reallocate|Up|Down|Left|Right|Level|Costs|Available|You|Click|None)\b/;

scenariusz('okna-mmocore-pl', async ({ w, polacz }) => {
  const nick = 'KsPl' + (Date.now() % 100000);
  const bot = await polacz({ nick }); bot._ekran = obserwuj(bot);
  await postac.wejdz(bot, { imie: 'Pl' + (Date.now() % 10000), rasa: 0, klasa: 2 });
  await sleep(3000);
  const okno = async (kom, wzor, klucz) => {
    const pop = bot.currentWindow; bot.chat(kom);
    const o = await czekajNaOkno(bot, { poprzednie: pop, ms: 6000 }).catch(() => null);
    const tyt = czysc(tytulOkna(o));
    const t = o ? sloty(o).map(s => czysc(s.nazwa) + ' ' + czysc(((o.slots[s.slot] || {}).customLore || []).map(teksty).join(' / '))).join(' | ') : '';
    const ang = (tyt + ' | ' + t).match(ANG);
    console.log(kom, '->', JSON.stringify(tyt), '|', t.slice(0, 400));
    w.sprawdz(klucz, !!o && wzor.test(tyt) && !ang, `tytul "${tyt}", angielskie: ${ang ? ang[0] : 'brak'}`);
    if (o) { bot.closeWindow(o); await sleep(800); }
  };
  await okno('/class', /Wybór klasy/, 'class-select-po-polsku');
  await okno('/waypoints', /Punkty podróży/, 'waypoints-po-polsku');
  await okno('/skilltrees general', /Drzewko/, 'specific-skill-tree-po-polsku');
});
