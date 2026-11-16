'use strict';
// Licho nie śpi — pełna runda jak gracze (konta bez OP): kreator -> Q -> Drużyna -> LICHO -> Dołącz (oba boty),
// poczekalnia bez skrótów, role, wcielenie PPM w snopek, zastygnięcie, pudło w zaczepę (tchu), 3 trafienia = złapanie,
// koniec rundy i powrót. RCON tylko do obserwacji (`licho stan`, stan.*).
//   /opt/docker/mc/narzedzia/bot/run.sh licho testy/runda.js
const { scenariusz, rcon, czekaj, sleep, stan, postac, otworzHub, kliknijSlot, tytulOkna, obserwuj, ruchGracza, idz } = require('/bot/lib');

const SNOPEK = { x: -6, y: 64, z: 19 };      // najbliższy sprzęt przy bramie (mapa.txt)
const stanRundy = async () => (await rcon('licho stan')).replace(/\s+/g, ' ');
const gracz = (s, nick) => {
  const m = s.match(new RegExp(`\\| ${nick} (\\w+) iskry=(\\d+) tchu=(\\d+) pkt=(\\d+) ksztalt=(\\S+) zastygly=(\\S+)`));
  return m && { rola: m[1], iskry: +m[2], tchu: +m[3], pkt: +m[4], ksztalt: m[5], zastygly: m[6] };
};
const faza = s => (s.match(/faza=(\w+)/) || [])[1] || 'BRAK';

async function dolaczZMenu(w, bot, tag) {
  // po kreatorze gracz chwilę jest jeszcze w creator_limbo (MenuKey tam blokuje Q)
  await czekaj(async () => !/creator_limbo/.test(await stan.wymiar(bot.username)) || null, 15000, 500);
  await sleep(1500);
  let hub = null;
  for (let i = 0; i < 3 && !hub; i++) hub = await otworzHub(bot).catch(async () => { await sleep(1500); return null; });
  // Q po kreatorze INNEGO gracza bywa martwe także bez Licha (24.10, diag q2: Zaklecia `q` -> wybor.przelacz zamiast huba) —
  // znalezisko poza tym zadaniem; tu ta sama komenda, którą Zaklecia wysyła na Q, dalej już tylko kliknięcia gracza
  if (!hub) { w.sprawdz(`${tag}-q`, false, `Q nie otworzyło huba (${await stan.wymiar(bot.username)}) — obejście dm open`); await rcon(`dm open hub_glowne ${bot.username}`); hub = await czekaj(() => bot.currentWindow, 4000, 100); }
  const dr = await kliknijSlot(bot, 38);
  w.sprawdz(`${tag}-druzyna`, !!dr.slots[25], `Drużyna: kafel LICHO w slocie 25 (${tytulOkna(dr)?.slice(0, 40)})`);
  const menu = await kliknijSlot(bot, 25);
  const e = obserwuj(bot);
  w.sprawdz(`${tag}-menu`, /licho_1/.test(e.opis()), 'okno Licha ma tło licho_1..4');
  await kliknijSlot(bot, 5, { noweOkno: false });
  const wym = await czekaj(async () => (await stan.wymiar(bot.username)) === 'minecraft:licho' || null, 8000, 400);
  w.sprawdz(`${tag}-wejscie`, !!wym, `wymiar ${await stan.wymiar(bot.username)}, tryb ${await stan.trybGry(bot.username)}`);
  return hub && menu;
}

scenariusz('licho-runda', async ({ w, polacz }) => {
  const sfx = Date.now() % 10000;
  // po kolei: jeden bot naraz w kreatorze (lżej dla klonu)
  const wejdz = async nick => {
    const b = await polacz({ nick }); const e = obserwuj(b);
    const r = await postac.wejdz(b, { imie: (nick[2] === 'A' ? 'Licha' : 'Lichb') + nick.slice(-4) });  // imiona postaci muszą się różnić
    w.sprawdz(`${nick}-postac`, !!r, `droga ${r && r.droga}`);
    return [b, e];
  };
  const [A, eA] = await wejdz('LiA' + sfx);
  const [B, eB] = await wejdz('LiB' + sfx);
  const skad = await stan.wymiar(A.username);
  await dolaczZMenu(w, A, 'A');
  await dolaczZMenu(w, B, 'B');
  w.sprawdz('poczekalnia', /POCZEKALNIA/.test(await stanRundy()), await stanRundy());

  // odliczanie: 25 s przy 2 graczach (+5), bez skrótu
  const s1 = await czekaj(async () => { const s = await stanRundy(); return faza(s) === 'ROZPROSZENIE' ? s : null; }, 60000, 1000);
  w.sprawdz('rozproszenie', !!s1, s1 || await stanRundy());
  if (!s1) return;
  const ra = gracz(s1, A.username), rb = gracz(s1, B.username);
  const [L, Z] = ra.rola === 'LICHO' ? [A, B] : [B, A];
  const eL = L === A ? eA : eB, eZ = Z === A ? eA : eB;
  w.sprawdz('role', [ra.rola, rb.rola].sort().join() === 'LICHO,ZAMAWIACZ', `${A.username}=${ra.rola} ${B.username}=${rb.rola}`);
  w.sprawdz('licho-adventure', (await stan.trybGry(L.username)) === 'adventure', await stan.trybGry(L.username));
  const pz = await stan.poz(Z.username);
  w.sprawdz('komora', pz.y < 50, `Zamawiacz y=${pz.y.toFixed(1)}, efekty ${JSON.stringify(await stan.efekty(Z.username))}`);
  w.sprawdz('hud-licho', /Lichem|Licho/i.test(eL.opis()), eL.opis().split('\n').filter(l => /TYTUL|BOSSBAR|ACTION/.test(l)).join(' / '));

  // Licho idzie do snopka i bierze jego kształt (PPM w zaczepę sprzętu)
  ruchGracza(L);
  const t1 = await idz(L, SNOPEK.x + 2, SNOPEK.y, SNOPEK.z, { r: 1, ms: 20000 });
  w.sprawdz('licho-chodzi', t1.ok, t1.opis);
  const zaczepa = Object.values(L.entities).find(e => e.name === 'interaction' && e.position.distanceTo({ x: SNOPEK.x + 0.5, y: SNOPEK.y, z: SNOPEK.z + 0.5 }) < 0.6);
  w.sprawdz('zaczepa-snopka', !!zaczepa, zaczepa ? `encja ${zaczepa.id}` : 'brak Interaction na snopku');
  if (zaczepa) { await L.lookAt(zaczepa.position.offset(0, 0.5, 0)); L.activateEntity(zaczepa); }
  const sk = await czekaj(async () => { const g = gracz(await stanRundy(), L.username); return g && g.ksztalt === 'hay_block' ? g : null; }, 4000, 300);
  w.sprawdz('wcielenie', !!sk, JSON.stringify(gracz(await stanRundy(), L.username)) + ' / ' + (eL.actionbar || ''));
  L.clearControlStates();
  const zs = await czekaj(async () => { const g = gracz(await stanRundy(), L.username); return g && g.zastygly !== '-' ? g : null; }, 5000, 300);
  w.sprawdz('zastygniecie', !!zs, JSON.stringify(gracz(await stanRundy(), L.username)));

  // łowy: Zamawiacz przy bramie
  const s2 = await czekaj(async () => { const s = await stanRundy(); return faza(s) === 'LOWY' ? s : null; }, 40000, 1000);
  w.sprawdz('lowy', !!s2, s2 || await stanRundy());
  if (!s2 || !zs) return;
  const [zx, zy, zz] = zs.zastygly.split(',').map(Number);
  ruchGracza(Z);
  const t2 = await idz(Z, zx + 2.5, zy, zz + 0.5, { r: 1, ms: 20000 });
  w.sprawdz('zamawiacz-chodzi', t2.ok, t2.opis);
  const ents = () => Object.values(Z.entities).filter(e => e.name === 'interaction');
  const blok = await czekaj(() => { const b = Z.blockAt(Z.entity.position.constructor ? new (Z.entity.position.constructor)(zx, zy, zz) : null); return b && b.name === 'hay_block' ? b : null; }, 3000, 200);
  w.sprawdz('pozorny-blok', !!blok, `Zamawiacz widzi w ${zx},${zy},${zz}: ${Z.blockAt(new (Z.entity.position.constructor)(zx, zy, zz))?.name}`);

  // pudło: cios w prawdziwy snopek (zaczepa sprzętu) -> -1 tchu; najpierw w zasięg ręki (4,5+0,8) od snopka
  const t3 = await idz(Z, SNOPEK.x + 1.5, SNOPEK.y, SNOPEK.z + 1.5, { r: 1, ms: 15000 });
  w.sprawdz('zamawiacz-do-snopka', t3.ok, t3.opis);
  const przed = gracz(await stanRundy(), Z.username);
  const pudlo = ents().find(e => e.position.distanceTo({ x: SNOPEK.x + 0.5, y: SNOPEK.y, z: SNOPEK.z + 0.5 }) < 0.6);
  if (pudlo) { await Z.lookAt(pudlo.position.offset(0, 0.5, 0)); Z.attack(pudlo); }
  await sleep(700);
  const po = gracz(await stanRundy(), Z.username);
  w.sprawdz('pudlo-tchu', po.tchu === przed.tchu - 1, `tchu ${przed.tchu} -> ${po.tchu}`);

  // trzy trafienia w Licho -> złapane
  const cel = ents().find(e => e.position.distanceTo({ x: zx + 0.5, y: zy, z: zz + 0.5 }) < 0.6);
  w.sprawdz('zaczepa-licha', !!cel, cel ? 'jest' : JSON.stringify(ents().map(e => e.position)));
  if (!cel) return;
  const hist = [];
  for (let i = 0; i < 3; i++) {
    await Z.lookAt(cel.position.offset(0, 0.5, 0)); Z.attack(cel); await sleep(800);
    const g = gracz(await stanRundy(), L.username); hist.push(g ? `${g.rola}/${g.iskry}` : faza(await stanRundy()));
  }
  w.sprawdz('trafienia', /\/2/.test(hist[0]) && /\/1/.test(hist[1]), hist.join(' '));
  const s3 = await stanRundy();
  w.sprawdz('zlapane-koniec', /ZAMAWIACZ/.test(hist[2]) || faza(s3) === 'KONIEC' || faza(s3) === 'BRAK', `${hist[2]} | ${s3}`);
  await sleep(1500);
  w.sprawdz('podsumowanie', /zamówi|złap|Licho|Zamawiacz/i.test(eL.czat.slice(-8).join(' | ') + eZ.czat.slice(-8).join(' | ')), eZ.czat.slice(-5).join(' | '));

  // po podsumowaniu (8 s) obaj wracają skąd przyszli, w swoim trybie, bez encji gry
  const wrot = await czekaj(async () => (await stan.wymiar(A.username)) === skad && (await stan.wymiar(B.username)) === skad || null, 20000, 1000);
  w.sprawdz('powrot', !!wrot, `${await stan.wymiar(A.username)} / ${await stan.wymiar(B.username)} (skąd ${skad}), tryb ${await stan.trybGry(L.username)}`);
  w.sprawdz('tryb-po', (await stan.trybGry(L.username)) === 'survival', await stan.trybGry(L.username));
  const ile = await rcon('minecraft:execute in minecraft:licho if entity @e[type=minecraft:interaction]');
  const ile2 = await rcon('minecraft:execute in minecraft:licho if entity @e[type=minecraft:block_display]');
  w.sprawdz('encje-sprzatniete', !/passed/i.test(ile) && !/passed/i.test(ile2), `${ile} / ${ile2}`);
}, { limitMs: 330000 });
