'use strict';
// Wspolne narzedzia testow Wyscigow. Bot steruje jak gracz: pakiet player_input (WASD/skok/Shift) i obrot glowy (look).
// Prawda z serwera przez RCON (`wyscigi poz|zdarzenia|tor`), nie ze stanu bota.
const { sleep, czekaj, plain } = require('/bot/lib');

function narzedzia(rcon) {
  const t = {};
  t.poz = async nick => {
    const r = await rcon(`wyscigi poz ${nick}`);
    const m = r.match(/POZ ([-\d.]+) ([-\d.]+) ([-\d.]+) kier=([-\d.]+) v=([-\d.]+) postep=([-\d.]+) max=([-\d.]+) faza=(\w+) meta=(-?\d+) turbo=(\d+) poslizg=(\w+) lad=(\d+) pobocze=(\w+) kierowca=(\w+) poz=(\d+)/);
    if (!m) return null;
    return { x: +m[1], y: +m[2], z: +m[3], kier: +m[4], v: +m[5], postep: +m[6], max: +m[7], faza: m[8], meta: +m[9], turbo: +m[10],
      poslizg: m[11] === 'true', lad: +m[12], pobocze: m[13] === 'true', kierowca: m[14] === 'true', pozycja: +m[15] };
  };
  t.zdarzenia = async nick => (await rcon(`wyscigi zdarzenia ${nick}`)).replace(/^ZDARZENIA ?/, '').split(' | ').filter(Boolean);
  t.tor = async () => {
    const r = await rcon('wyscigi tor');
    const m = r.match(/TOR ([\d.]+) (.*)/);
    const pkt = m[2].split(';').filter(Boolean).map(s => s.split(',').map(Number));
    return { dlugosc: +m[1], pkt };
  };
  t.wymiar = async n => (await rcon(`data get entity ${n} Dimension`)).replace(/.*data: /, '').replace(/"/g, '').trim();
  t.pozGracza = async n => { const m = (await rcon(`data get entity ${n} Pos`)).match(/\[([-\d.]+)d, ([-\d.]+)d, ([-\d.]+)d\]/); return m ? { x: +m[1], y: +m[2], z: +m[3] } : null; };
  t.wilki = async () => { const r = await rcon('minecraft:execute in minecraft:wyscigi positioned 20 64 40 if entity @e[type=minecraft:wolf,distance=..200]'); const m = r.match(/count: (\d+)/i); return m ? +m[1] : 0; };
  const ile = async typ => { const r = await rcon(`minecraft:execute in minecraft:wyscigi positioned 20 64 40 if entity @e[type=minecraft:${typ},distance=..200]`); const m = r.match(/count: (\d+)/i); return m ? +m[1] : 0; };
  t.skrzynki = () => ile('item_display');
  t.strzalki = () => ile('text_display');
  t.hp = async n => { const m = (await rcon(`data get entity ${n} Health`)).match(/([\d.]+)f/); return m ? +m[1] : null; };
  return t;
}

/** Sterowanie: wejscia jak klawiatura gracza. */
function wejscie(bot, w = {}) {
  bot._client.write('player_input', { inputs: { forward: !!w.forward, backward: !!w.backward, left: !!w.left, right: !!w.right, jump: !!w.jump, shift: !!w.shift, sprint: false } });
}
function patrz(bot, yaw, pitch = 15) {
  bot._client.write('look', { yaw, pitch, flags: { onGround: false, hasHorizontalCollision: false } });
}
const yawDo = (x, z, tx, tz) => Math.atan2(-(tx - x), tz - z) * 180 / Math.PI;
const zawin = k => { k %= 360; if (k >= 180) k -= 360; if (k < -180) k += 360; return k; };

/**
 * Autopilot jak gracz z klawiatura: W + A/D (kat do punktu linii srodkowej `przod` punktow dalej, wzgledem
 * kierunku wierzchowca z `wyscigi poz`), w zakretach Shift + strona = poslizg. Konczy, gdy `stop(poz)` = true albo brak kierowcy.
 */
async function autopilot(bot, t, nick, tor, opcje = {}) {
  const { przod = 4, limitMs = 240000, stop = () => false, poslizg = false, katPoslizgu = 20, gaz = () => true, krok = 100, martwaStrefa = 3 } = opcje;
  const n = tor.pkt.length;
  let idx = -1, ost = null, shift = false, strona = 0;
  const start = Date.now();
  bot.physicsEnabled = false;
  while (Date.now() - start < limitMs) {
    const p = await t.poz(nick);
    if (!p) return { koniec: 'brak', ost };
    ost = p;
    if (stop(p)) { wejscie(bot, {}); return { koniec: 'stop', ost }; }
    let best = 0, bd = 1e9;
    const zakres = idx < 0 ? [...Array(n).keys()] : [...Array(21).keys()].map(k => (idx + k - 10 + n) % n);
    for (const i of zakres) { const d = (tor.pkt[i][0] - p.x) ** 2 + (tor.pkt[i][1] - p.z) ** 2; if (d < bd) { bd = d; best = i; } }
    idx = best;
    const cel = tor.pkt[(idx + przod) % n];
    const roznica = zawin(yawDo(p.x, p.z, cel[0], cel[1]) - p.kier);
    if (poslizg) {
      const a = tor.pkt[(idx + 2) % n], b = tor.pkt[(idx + 4) % n], c = tor.pkt[(idx + 9) % n];
      const skret = zawin(yawDo(b[0], b[1], c[0], c[1]) - yawDo(a[0], a[1], b[0], b[1]));
      if (!shift) {
        if (Math.abs(skret) > katPoslizgu) { shift = true; strona = Math.sign(skret) || 1; }
      } else {
        // puszczamy, gdy zakret sie konczy, zmienia strone (es) albo tor ucieka na zewnatrz luku
        if (Math.abs(skret) < katPoslizgu * 0.35 || Math.sign(skret) !== strona || roznica * strona < -25) shift = false;
      }
    }
    const prawo = roznica > martwaStrefa, lewo = roznica < -martwaStrefa;
    // w poslizgu trzymamy strone poslizgu (A/D tylko zaciesnia/poszerza)
    // w poslizgu: klawisz do srodka (laduje szybciej), na zewnatrz tylko gdy luk jest za ciasny
    const doSrodka = roznica * strona > -4;
    const w = shift ? { forward: gaz(p), shift: true, right: (strona > 0) === doSrodka, left: (strona < 0) === doSrodka }
                    : { forward: gaz(p), right: prawo, left: lewo };
    wejscie(bot, w);
    await sleep(krok);
  }
  wejscie(bot, {});
  return { koniec: 'limit', ost };
}

/** Klawisze z siodla jak gracz: Q (pauza), F (bonus), LPM (cios). */
function klawiszQ(bot) { bot._client.write('block_dig', { status: 4, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 }); }
function klawiszF(bot) { bot._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 }); }
function lpm(bot) { bot._client.write('arm_animation', { hand: 0 }); }
function nazwaSlotu(okno, slot) { const it = okno?.slots?.[slot]; if (!it) return ''; try { return JSON.stringify(it.customName ?? it.nbt ?? ''); } catch { return ''; } }

/** Q (wyrzucenie z reki) -> MenuKey otwiera hub; przy pustej rece gracz uzywa /menu (open_command). */
async function otworzHub(bot) {
  const okno = new Promise(res => bot.once('windowOpen', res));
  bot._client.write('block_dig', { status: 4, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 });
  let w = await Promise.race([okno, sleep(2500).then(() => null)]);
  if (!w) {
    const okno2 = new Promise(res => bot.once('windowOpen', res));
    bot.chat('/menu');
    w = await Promise.race([okno2, sleep(4000).then(() => null)]);
    return { okno: w, droga: '/menu' };
  }
  return { okno: w, droga: 'Q' };
}

async function kliknijIOtworz(bot, slot, czasMs = 4000) {
  const okno = new Promise(res => bot.once('windowOpen', res));
  await bot.clickWindow(slot, 0, 0);
  return Promise.race([okno, sleep(czasMs).then(() => null)]);
}

const tytul = okno => okno ? plain(okno.title) : '';

module.exports = { narzedzia, wejscie, patrz, autopilot, otworzHub, kliknijIOtworz, tytul, yawDo, zawin, sleep, czekaj, klawiszQ, klawiszF, lpm, nazwaSlotu };
