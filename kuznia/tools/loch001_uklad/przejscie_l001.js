'use strict';
// Loch 001 remake, krok 4: przejście blockoutu botem BEZ OP (klasa BASTION poz. 2, Miecz kamienny, jak arena "uważny").
//   python3 blockout_l001.py            -> wynik/trasa.json + wynik/datapack/l1r_blockout (wgrany na klon, function l1r_bo:buduj)
//   LIMIT_MS=1800000 run.sh <klon> kuznia/tools/loch001_uklad/przejscie_l001.js [uwazny|nieuwazny]
// Bot idzie TRASĄ z uklad_l001.py, walczy z każdym wrogiem w sali (logika walki z przeciwnicy/arena_l001.js),
// po śmierci wraca od ogniska (spawnpoint: S1, po murze Bramy O2). Bramy otwiera scenariusz RCON-em, gdy padną
// moby z warunku (w lochu zrobią to funkcje MD). Strażnika Sadu nie ma: przejście kończy wejście do Groty Dzwonu.
// Linie POMIAR {...}: odcinki (czas, HP, zgony, odblok), sale (czas walki), bramy, wynik całkowity.
const fs = require('fs');
const { scenariusz, sleep, ruchGracza } = require('/bot/lib');
const { goals } = require('mineflayer-pathfinder');
const pvp = require('mineflayer-pvp').plugin;

const STYL = process.argv[2] || 'uwazny';
const D = JSON.parse(fs.readFileSync('/scen/wynik/trasa.json', 'utf8'));
const r1 = x => +(+x).toFixed(1);
const ZAPOWIEDZI = {   // jak arena_l001.js: "glosnosc/wysokosc" -> [nazwa, unik, ms]
  '1.6/0.5': ['wilk-skok', 'bok', 700], '1.6/0.8': ['lucznik-naciag', 'bok', 900],
  '2.0/0.5': ['korzeniak', 'tyl', 1100], '1.8/0.7': ['siewca-puch', 'bok', 800],
  '1.8/0.6': ['korowiec-cios', 'tyl', 900], '1.8/0.8': ['sowiedz-skok', 'bok', 900],
  '1.8/0.5': ['sowiedz-lapa', 'tyl', 800],
};
const TYP = { L1R_WILK: 'wolf', L1R_KORZENIAK: 'zombie', L1R_LUCZNIK: 'skeleton', L1R_PNIACZEK: 'husk', L1R_KOTWA: 'zombie',
  L1R_SIEWCA: 'husk', L1R_KOROWIEC: 'zombie', L1R_SOWIEDZ: 'polar_bear', L1R_PAK: 'zombie' };
const [AX0, AY0, AZ0, AX1, AY1, AZ1] = D.obszar;
const BOX = `x=${AX0},y=${AY0},z=${AZ0},dx=${AX1 - AX0},dy=${AY1 - AY0},dz=${AZ1 - AZ0}`;

scenariusz(`przejscie-l001-${STYL}`, async ({ w, polacz, rcon }) => {
  const ile = async sel => { const m = /count: (\d+)|(\d+)$/i.exec((await rcon(`minecraft:execute in minecraft:overworld if entity ${sel}`)).trim()); return m ? +(m[1] || m[2]) : 0; };
  const hpScore = async nick => { const m = /has (\d+)/.exec(await rcon(`minecraft:scoreboard players get ${nick} l1r_dmg`)); return m ? +m[1] / 10 : 0; };
  const pomiar = (id, v) => console.log(`POMIAR ${id} ${JSON.stringify(v)}`);
  const ow = c => rcon(`minecraft:execute in minecraft:overworld run ${c}`);

  // --- scena: wrogowie od startu na miejscach (decyzja 27.09), z tagiem bo + bo_<MOB> do warunków bram
  await ow(`minecraft:forceload add ${D.forceload}`);
  await rcon('minecraft:gamerule spawn_monsters false');
  // blockout stoi w 'world' klona, w regionie WG chw_perlowa_zatoka (x172-268, z316-412, bez flag): WG nie daje
  // nie-członkom ranić zwierząt, więc wilki S1/S2 były nietykalne (16.11: 17 ciosów = 1 HP). Na world1 loch to instancja MD.
  await rcon('rg flag -w world chw_perlowa_zatoka damage-animals allow');
  await rcon('minecraft:scoreboard objectives add l1r_dmg minecraft.custom:minecraft.damage_taken');
  for (let i = 0; i < 3; i++) { await ow(`minecraft:kill @e[type=!player,${BOX}]`); await sleep(300); }
  for (const b of Object.values(D.bramy)) await ow(`minecraft:fill ${b.fill} minecraft:oak_wood`);
  for (const e of D.wrogowie) {
    await rcon(`mm mobs spawn ${e.mob} 1 world,${e.x},${e.y},${e.z}`); await sleep(150);
    await ow(`minecraft:execute positioned ${e.x} ${e.y} ${e.z} run minecraft:tag @e[type=${TYP[e.mob]},tag=!bo,distance=..2.5,sort=nearest,limit=1] add bo_${e.mob}`);
    await ow(`minecraft:execute positioned ${e.x} ${e.y} ${e.z} run minecraft:tag @e[tag=bo_${e.mob},tag=!bo,distance=..2.5,sort=nearest,limit=1] add bo`);
  }
  await sleep(1500);
  const otag = await ile('@e[tag=bo]');
  w.sprawdz('wrogowie', otag === D.wrogowie.length, `z tagiem ${otag}/${D.wrogowie.length}`);
  const ileTyp = {}; for (const e of D.wrogowie) ileTyp[e.mob] = (ileTyp[e.mob] || 0) + 1;
  pomiar('sklad', ileTyp);

  // --- bot jak na arenie (konsola nadaje klasę; bot nie ma OP)
  const nick = (STYL === 'uwazny' ? 'Pu' : 'Pn') + (Date.now() % 100000);
  let bot = await polacz({ nick }); await sleep(2500);
  await rcon(`mmocore admin class ${nick} BASTION`);
  await rcon(`mmocore admin level set ${nick} main 2`);
  await rcon(`mmocore admin attribute give ${nick} bieglosc-miecz 1`);
  bot.quit(); await sleep(8000);
  bot = await polacz({ nick }); await sleep(2500);
  ruchGracza(bot); bot.loadPlugin(pvp);
  // mineflayer-pvp 1.3.2 przy każdym attack() wstawia pathfinderowi WŁASNE domyślne Movements (PVP.js:50,70: kopanie,
  // skosy bez poprawki ruchGracza) -> po pierwszej walce bot „kopał” skałę mieczem i stał (16.11 #10: S3E 180 s, S8 2×120 s)
  bot.pvp.movements = bot.pathfinder.movements;
  const op = await rcon('minecraft:op list').catch(() => '');
  w.sprawdz('bez-op', !new RegExp(nick).test(op), 'bot nie jest na liście OP');
  await rcon(`minecraft:gamemode survival ${nick}`); await rcon(`minecraft:clear ${nick}`);
  await rcon(`mi give SWORD MIECZ_KAMIENNY ${nick} 1`); await sleep(800);
  const inv = await rcon(`minecraft:data get entity ${nick} Inventory`);
  const sl = /\{Slot: (\d)b, id: "minecraft:stone_sword/.exec(inv); if (sl) bot.setQuickBarSlot(+sl[1]);
  w.sprawdz('miecz', !!sl, `slot ${sl && sl[1]}`);
  const ognisko = async k => { const o = D.ogniska[k]; await rcon(`minecraft:spawnpoint ${nick} ${Math.floor(o.x)} ${o.y} ${Math.floor(o.z)}`); };
  await ognisko('S1');
  const t0p = D.trasa[0];
  await ow(`minecraft:tp ${nick} ${t0p.x} ${t0p.y} ${t0p.z} 180 0`); await sleep(3000);
  await rcon(`minecraft:effect clear ${nick}`);
  await rcon(`minecraft:scoreboard players set ${nick} l1r_dmg 0`);

  // --- walka (z arena_l001.js)
  let reakcjaDo = 0, zgon = false, zgony = 0; const reakcje = {}, nieznane = {}; const smierci = [];
  bot.on('death', () => { zgon = true; zgony++; smierci.push(bot.entity ? bot.entity.position.floored().toString() : '?');
    setTimeout(() => { try { bot.respawn(); } catch {} }, 300); });
  async function unik(kier, ms) {
    if (Date.now() < reakcjaDo) return; reakcjaDo = Date.now() + ms;
    bot.pvp.forceStop(); bot.pathfinder.setGoal(null); bot.clearControlStates(); bot.setControlState('sprint', true);
    if (kier === 'bok') bot.setControlState(Math.random() < 0.5 ? 'left' : 'right', true); else bot.setControlState('back', true);
    await sleep(ms); bot.clearControlStates();
  }
  bot.on('soundEffectHeard', (nazwa, poz, vol, pitch) => {
    const k = `${(+vol).toFixed(1)}/${(+pitch).toFixed(1)}`, z = ZAPOWIEDZI[k];
    if (!z) { if (vol >= 1.4) nieznane[`${nazwa} ${k}`] = (nieznane[`${nazwa} ${k}`] || 0) + 1; return; }
    if (poz && poz.distanceTo(bot.entity.position) > 12) return;          // zapowiedź z innej sali
    reakcje[z[0]] = (reakcje[z[0]] || 0) + 1;
    if (STYL !== 'uwazny') return;
    if (z[0] === 'korzeniak' && poz && poz.distanceTo(bot.entity.position) > 6) return;
    unik(z[1], z[2]);
  });
  const nazwaE = e => { const m = e.metadata && e.metadata[2]; return (m && m.value && m.value.text && m.value.text.value) || ''; };
  const jestPak = e => e.name === 'zombie' && (/^(Pąk|Duży pąk)/.test(nazwaE(e)) || (e.equipment || []).some(i => i && /azalea|dandelion/.test(i.name)));
  const TYPY_WROGA = new Set(['wolf', 'zombie', 'husk', 'skeleton', 'polar_bear']);
  const wrog = e => TYPY_WROGA.has(e.name) && nazwaE(e) !== '' && !jestPak(e) && e.isValid !== false;
  const PRIOR = new Set(['husk', 'zombie']);
  const wSali = (e, sala) => { const b = D.sale[sala]; if (!b) return false; const p = e.position;
    return p.x >= b[0] - 1 && p.x <= b[3] + 2 && p.z >= b[2] - 1 && p.z <= b[5] + 2 && p.y >= b[1] - 2 && p.y <= b[4] + 1; };
  const blisko = (e, r) => e.position.distanceTo(bot.entity.position) < r && Math.abs(e.position.y - bot.entity.position.y) < 4;
  const istotne = (e, sale) => sale.some(s => wSali(e, s)) || blisko(e, 7);
  const ruch = {};
  const sledz = () => { for (const e of Object.values(bot.entities)) { if (!PRIOR.has(e.name)) continue;
    const p = ruch[e.id]; if (!p || p.pos.distanceTo(e.position) > 0.3) ruch[e.id] = { pos: e.position.clone(), t: Date.now() };
    e._stoi = Date.now() - ruch[e.id].t > 2000; } };
  const cel = sale => {
    const me = bot.entity.position;
    const wrogowie = Object.values(bot.entities).filter(e => wrog(e) && istotne(e, sale));
    if (STYL === 'uwazny') {
      const pak = Object.values(bot.entities).filter(e => jestPak(e) && e.position.distanceTo(me) < 7 && istotne(e, sale))
        .sort((a, b) => a.position.distanceTo(me) - b.position.distanceTo(me))[0];
      if (pak) return pak;
      // Kotwa (warunek bramy, świeci) to cel: gracz widzi, że budzi Pniaczki
      const kotwa = wrogowie.filter(e => /Kotwa/.test(nazwaE(e)) && e.position.distanceTo(me) < 20)[0];
      if (kotwa) return kotwa;
      // najpierw to, co bije z bliska (16.11 #7: bot szedł do Siewcy na tarasie, Korzeniak i Pniaczek zabili go 4x, nikt nie stracił HP)
      const przy = wrogowie.filter(e => blisko(e, 4)).sort((a, b) => a.position.distanceTo(me) - b.position.distanceTo(me))[0];
      if (przy) return przy;
      const stoj = wrogowie.filter(e => PRIOR.has(e.name) && e._stoi && e.position.distanceTo(me) < 16);
      if (stoj.length) return stoj[0];
    }
    return wrogowie.sort((a, b) => a.position.distanceTo(me) - b.position.distanceTo(me))[0];
  };
  const walka = { s: 0, paki: 0 };
  async function walcz(sale, limitMs = 180000) {
    const t0 = Date.now(); let ostatni = null, pusto = 0;
    while (Date.now() - t0 < limitMs && !zgon) {
      sledz();
      const c = cel(sale);
      if (!c) { if (++pusto > 6) break; await sleep(250); continue; }
      pusto = 0;
      if (Date.now() >= reakcjaDo) {
        if (jestPak(c)) { if (ostatni !== c.id) walka.paki++; bot.pvp.forceStop(); await bot.lookAt(c.position.offset(0, 0.5, 0), true);
          if (c.position.distanceTo(bot.entity.position) > 2.8) { bot.setControlState('forward', true); await sleep(250); bot.clearControlStates(); } else bot.attack(c); }
        else if (bot.pvp.target !== c) bot.pvp.attack(c);
        ostatni = c.id;
      }
      await sleep(150);
    }
    bot.pvp.forceStop(); bot.clearControlStates();
    const s = (Date.now() - t0) / 1000; walka.s += s; return s;
  }

  // --- bramy: warunek sprawdzany co 2 s
  const otwarte = {};
  const tStart = Date.now();
  const pilnuj = setInterval(async () => {
    for (const [n, b] of Object.entries(D.bramy)) {
      if (otwarte[n]) continue;
      if (await ile(`@e[tag=${b.tag}]`) === 0) {
        await ow(`minecraft:fill ${b.fill} minecraft:air replace minecraft:oak_wood`);
        otwarte[n] = r1((Date.now() - tStart) / 1000); pomiar('brama', { n, s: otwarte[n] });
      }
    }
  }, 2000);

  // --- marsz z przerwą na walkę
  async function marsz(p, sale, ms = 120000) {
    const t0 = Date.now(); let odblok = 0, ost = bot.entity.position.clone(), tOst = Date.now();
    const odl = () => { const q = bot.entity.position; return Math.hypot(q.x - p.x, q.z - p.z) + Math.max(0, Math.abs(q.y - p.y) - 2); };
    const goal = () => bot.pathfinder.setGoal(new goals.GoalNear(p.x, p.y, p.z, 1));
    goal();
    while (Date.now() - t0 < ms && odl() > 2.5) {
      if (zgon) { bot.pathfinder.setGoal(null); return { ok: false, zgon: true, odblok }; }
      const c = cel(sale);
      if (c && (blisko(c, 10) || sale.some(s => wSali(c, s) && wSali(bot.entity, s)))) {
        bot.pathfinder.setGoal(null); await walcz(sale);
        if (zgon) return { ok: false, zgon: true, odblok };
        goal(); ost = bot.entity.position.clone(); tOst = Date.now(); continue;
      }
      await sleep(250);
      // unik() na zapowiedź zeruje cel pathfindera: po uniku wracamy na trasę (bez tego bot stał pod ostrzałem Łucznika)
      if (!bot.pathfinder.goal && Date.now() >= reakcjaDo) { goal(); tOst = Date.now(); }
      if (bot.entity.position.distanceTo(ost) > 0.15) { ost = bot.entity.position.clone(); tOst = Date.now(); continue; }
      if (Date.now() - tOst < 2500) continue;
      odblok++; bot.pathfinder.setGoal(null); bot.clearControlStates();
      bot.setControlState('back', true); await sleep(350); bot.setControlState('back', false);
      goal(); ost = bot.entity.position.clone(); tOst = Date.now();
    }
    bot.pathfinder.setGoal(null); bot.clearControlStates();
    return { ok: odl() <= 2.5, odl: r1(odl()), odblok };
  }

  // --- przejście
  const odcinki = []; let i = 1, iOgn = 0, porazki = 0, odblokSum = 0;
  const salaCzas = {};
  while (i < D.trasa.length) {
    const p = D.trasa[i], prev = D.trasa[i - 1];
    const sale = [...new Set([prev.sala, p.sala])];
    const t0 = Date.now(), hp0 = await hpScore(nick), zg0 = zgony, w0 = walka.s;
    const r = await marsz(p, sale);
    odblokSum += r.odblok || 0;
    if (r.zgon) {
      await sleep(2500); zgon = false;
      if (bot.pvp) bot.pvp.forceStop();
      await rcon(`minecraft:effect give ${nick} minecraft:instant_health 1 10 true`).catch(() => {});
      if (sl) bot.setQuickBarSlot(+sl[1]);
      const reka = bot.heldItem ? bot.heldItem.name : 'pusta';
      if (reka !== 'stone_sword') w.sprawdz('miecz-po-zgonie', false, `w ręce ${reka} (keep_inventory?)`);
      pomiar('zgon', { przy: `${prev.sala}->${p.sala}`, poz: smierci[smierci.length - 1], s: r1((Date.now() - tStart) / 1000) });
      if (zgony > 8) { w.sprawdz('przejscie', false, `za dużo zgonów (${zgony})`); break; }
      // po śmierci wraca od ogniska PO PUNKTACH TRASY (jak gracz, który zna drogę): jeden cel ~60 bl. przez
      // zakręty S2/S3W pathfinder gubił (16.11 #6: 120 s, 33 odblokowania przy wyjściu z S2)
      const tPow = Date.now();
      for (let j = iOgn + 1; j < i && !zgon; j++) await marsz(D.trasa[j], [...new Set([D.trasa[j - 1].sala, D.trasa[j].sala])], 60000);
      pomiar('powrot', { do: p.sala, s: r1((Date.now() - tPow) / 1000) });
      continue;
    }
    if (r.ok) { await walcz(sale, 120000); }
    const s = (Date.now() - t0) / 1000;
    salaCzas[p.sala] = r1((salaCzas[p.sala] || 0) + s);
    odcinki.push({ do: `${p.sala}(${p.lok[0]},${p.lok[2]})`, ok: r.ok, s: r1(s), walka_s: r1(walka.s - w0), hp: r1(await hpScore(nick) - hp0), zgony: zgony - zg0, odblok: r.odblok });
    pomiar('odcinek', odcinki[odcinki.length - 1]);
    if (!r.ok) {
      if (++porazki > 2) { w.sprawdz('przejscie', false, `utknął przed ${p.sala} ${JSON.stringify(p.lok)} (odl ${r.odl})`); break; }
      continue;   // jeszcze raz ten sam punkt
    }
    porazki = 0;
    if (p.sala === 'O2') { await ognisko('O2'); iOgn = i; }
    i++;
  }
  clearInterval(pilnuj);
  const calk = r1((Date.now() - tStart) / 1000);
  const dotarl = i >= D.trasa.length;
  const zostalo = await ile('@e[tag=bo]');
  const pozost = {}; for (const m of Object.keys(ileTyp)) pozost[m] = await ile(`@e[tag=bo_${m}]`);
  pomiar('wynik', { styl: STYL, dotarl, czas_s: calk, min: r1(calk / 60), walka_s: r1(walka.s), marsz_s: r1(calk - walka.s),
    zgony, hp_utracone: await hpScore(nick), odblok: odblokSum, paki_zdeptane: walka.paki, wrogowie_zostali: zostalo, pozost,
    bramy: otwarte, sale_s: salaCzas, smierci, reakcje, nieznane });
  w.sprawdz('dotarl-do-groty-dzwonu', dotarl, `czas ${r1(calk / 60)} min, zgony ${zgony}`);
  w.sprawdz('bramy-otwarte', ['korzenie_jeziorka', 'mur_bramy'].every(n => otwarte[n]), JSON.stringify(otwarte));
  bot.quit();
}, { limitMs: Number(process.env.LIMIT_MS || 1800000) });
