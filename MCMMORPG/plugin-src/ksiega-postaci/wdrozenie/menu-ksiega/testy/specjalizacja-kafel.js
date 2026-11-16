'use strict';
// Kafel "Ścieżka" (specjalizacja) w menu Q -> Postać (hub_postac.yml, sloty 41,42,50,51). Droga gracza bez OP:
//   poz. 1: Q -> Postać -> kafel w stanie "zablokowana" (klik nic nie otwiera);
//   poz. 10 (+1 punkt klasy z exp-tables): kafel "Kliknij: wybierz specjalizację" -> okno MMOCore
//   "Wybierz specjalizację" -> kafel klasy -> potwierdzenie (slot 12) -> klasa = specjalizacja;
//   ponownie Q -> Postać: kafel "wybrana" z nazwą klasy.
// RCON tylko: EXP (mmocore admin exp give) i odczyt placeholderów.
// Uruchom: /opt/docker/mc/narzedzia/bot/run.sh <klon --z-kreatorem> <ten plik> [akolita|ninja]
// Klon: lp group default permission set mmocore.class-select true (klon nie ma bazy LuckPerms).
const { scenariusz, postac, stan, obserwuj, sleep, otworzHub, kliknijSlot, czekajNaOkno, tytulOkna, sloty } = require('/bot/lib');

const TRYB = process.argv[2] || 'akolita';
const KLASA = { akolita: { rasa: 0, klasa: 2 }, ninja: { rasa: 1, klasa: 3 } }[TRYB];   // rasa/archetyp jak w kreatorze
const czysc = s => String(s ?? '').replace(/§./g, '').trim();
const lore = (w, slot) => {
  const it = w && w.slots[slot]; const raw = (it && it.customLore) || [];
  // linia lore = NBT {type:'compound', value:{text|extra:...}} -> zbieramy wszystkie pola text (string) w kolejnosci
  const teksty = n => { if (n == null) return ''; if (typeof n === 'string') return n; if (Array.isArray(n)) return n.map(teksty).join('');
    if (typeof n === 'object') { if (n.type === 'string') return n.value; if ('value' in n && n.type) return teksty(n.value);
      return (n.text ? teksty(n.text) : '') + (n.extra ? teksty(n.extra) : ''); } return ''; };
  return raw.map(l => czysc(teksty(typeof l === 'string' ? (() => { try { return JSON.parse(l); } catch { return l; } })() : l))).join(' / ');
};

scenariusz('specjalizacja-kafel-' + TRYB, async ({ w, polacz, rcon }) => {
  const nick = 'KsSp' + (Date.now() % 100000);
  const bot = await polacz({ nick }); bot._ekran = obserwuj(bot);
  await postac.wejdz(bot, { imie: 'Sp' + (Date.now() % 10000), rasa: KLASA.rasa, klasa: KLASA.klasa });
  await sleep(3000);
  const ph = async p => czysc(await stan.papi(nick, p));
  const postacOkno = async () => {
    const hub = await otworzHub(bot);
    return kliknijSlot(bot, 18, { tytul: null, ms: 5000 });
  };
  const kafel = (o) => { const k = sloty(o).find(s => s.slot === 41); return k ? { nazwa: czysc(k.nazwa), lore: lore(o, 41) } : null; };

  // 1. przed 10. poziomem
  let o = await postacOkno(); let k = kafel(o);
  console.log('poz', await ph('%mmocore_level%'), 'okno', tytulOkna(o), 'kafel', JSON.stringify(k));
  w.sprawdz('kafel-zablokowany', !!k && /10\. poziomie/.test(k.lore), JSON.stringify(k));
  await bot.clickWindow(41, 0, 0); await sleep(1500);
  w.sprawdz('kafel-zablokowany-nic-nie-otwiera', bot.currentWindow === o, `okno po kliku: ${tytulOkna(bot.currentWindow)}`);
  bot.closeWindow(o); await sleep(500);

  // 2. poziom 10 -> wybór
  await rcon(`mmocore admin exp give ${nick} main 200000`); await sleep(2500);
  const poz = await ph('%mmocore_level%'), pk = await ph('%mmocore_class_points%');
  o = await postacOkno(); k = kafel(o);
  console.log('poz', poz, 'punkty', pk, 'kafel', JSON.stringify(k));
  w.sprawdz('kafel-aktywny-na-10', poz === '10' && !!k && /wybierz specjalizac/i.test(k.lore), `poz ${poz}, punkty ${pk}, ${JSON.stringify(k)}`);
  const spec = await kliknijSlot(bot, 41, { ms: 6000 }).catch(e => { console.log(e.message); return null; });
  const kafle = sloty(spec).map(s => [s.slot, czysc(s.nazwa)]);
  console.log('okno po kafelku:', tytulOkna(spec), JSON.stringify(kafle));
  w.sprawdz('kafel-otwiera-okno-specjalizacji', !!spec && /specjaliz/i.test(tytulOkna(spec) || ''), `okno "${tytulOkna(spec)}"`);
  const klasaPrzed = await ph('%mmocore_class_id%');
  const wyb = spec && sloty(spec).find(s => !/Wróć/i.test(czysc(s.nazwa)));   // pierwsza ścieżka (gracz wybiera jedną z dwóch)
  console.log('wybieram:', wyb && czysc(wyb.nazwa));
  if (wyb) {
    const pot = await kliknijSlot(bot, wyb.slot, { ms: 5000 }).catch(() => null);
    console.log('potwierdzenie:', tytulOkna(pot));
    if (pot) { await bot.clickWindow(12, 0, 0); await sleep(3000); }
  }
  if (bot.currentWindow) bot.closeWindow(bot.currentWindow);
  const kl = await ph('%mmocore_class_id%'), pk2 = await ph('%mmocore_class_points%');
  w.sprawdz('specjalizacja-wybrana-przez-kafel', !!wyb && kl && kl !== klasaPrzed && pk2 === '0', `klasa ${klasaPrzed} -> ${kl}, punkty klasy ${pk} -> ${pk2}`);

  // 3. po wyborze
  await sleep(1000);
  o = await postacOkno(); k = kafel(o);
  console.log('po wyborze kafel', JSON.stringify(k));
  w.sprawdz('kafel-po-wyborze', !!k && /Twoja ścieżka/.test(k.lore) && !/wybierz specjalizac/i.test(k.lore), JSON.stringify(k));
  bot.closeWindow(o);
});
