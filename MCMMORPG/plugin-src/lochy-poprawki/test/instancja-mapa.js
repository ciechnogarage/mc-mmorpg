// Czy instancja lochu (MAPA, domyslnie loch_001) sie zamyka: A) /leave, B) rozlaczenie w srodku (KickOfflinePlayers)
// 22.10: po kilku przebiegach 3 puste instancje wisialy i md play dawal "All instances are currently full!" (MaxInstances 3)
const { scenariusz } = require('/bot/lib');
const MAPA = process.env.MAPA || 'loch_001';
const NICK = 'InstLoch';
const sleep = ms => new Promise(r => setTimeout(r, ms));
scenariusz('instancja-'+MAPA, async ({ w, polacz, rcon }) => {
  const ok = (n, p, i = '') => w.sprawdz(n, p, i);
  const jest = async n => /time is/i.test(await rcon(`execute in minecraft:${MAPA}_${n} run minecraft:time query gametime`));
  const lista = async () => { const l = []; for (let n = 0; n < 8; n++) if (await jest(n)) l.push(n); return l; };
  const wejdz = async (bot, etap) => {
    await rcon(`md play ${MAPA} ${NICK}`);
    let inst = null; for (let i = 0; i < 40 && inst === null; i++) { for (let n = 0; n < 8; n++) if ((await rcon(`execute in minecraft:${MAPA}_${n} positioned 0 100 0 if entity @a[name=${NICK},distance=..2000]`)).includes('passed')) inst = n; await sleep(500); }
    ok(`wejscie_${etap}`, inst !== null, `${MAPA}_${inst} swiaty: ${await lista()}`);
    return inst;
  };
  const czekaj = async (inst, ms) => { const t0 = Date.now(); while (Date.now() - t0 < ms) { if (!(await jest(inst))) return (Date.now() - t0) / 1000; await sleep(5000); } return null; };
  ok('start_bez_instancji', (await lista()).length === 0, `swiaty: ${await lista()}`);
  // A: /leave
  let bot = await polacz({ nick: NICK }); await sleep(1500);
  const a = await wejdz(bot, 'A'); await sleep(4000);
  bot.chat('/leave'); const tA = await czekaj(a, 30000);
  ok('A_leave_zamyka_instancje', tA !== null, tA !== null ? `po ${tA.toFixed(0)} s` : `po 30 s dalej jest, swiaty: ${await lista()}`);
  // C: rozlaczenie i powrot przed KickOfflinePlayersDelay = wznowienie lochu (ADVENTURE z configu mapy = 2, widz = 3), potem /leave zamyka
  const c = await wejdz(bot, 'C'); await sleep(4000);
  bot.quit(); await sleep(15000);
  bot = await polacz({ nick: NICK }); await sleep(5000);
  const wC = await rcon(`data get entity ${NICK} Dimension`), gC = await rcon(`data get entity ${NICK} playerGameType`);
  ok('C_powrot_wznawia_loch', new RegExp(`${MAPA}_${c}`).test(wC) && /: 2$/.test(gC.trim()), `${wC.slice(-30)} | ${gC.slice(-25)}`);
  bot.chat('/leave'); const tC = await czekaj(c, 30000);
  ok('C_leave_po_powrocie_zamyka', tC !== null, tC !== null ? `po ${tC.toFixed(0)} s` : `po 30 s dalej jest, swiaty: ${await lista()}`);
  await sleep(3000);
  // B: rozlaczenie w srodku
  const b = await wejdz(bot, 'B'); await sleep(4000);
  bot.quit(); const tB = await czekaj(b, 420000);
  ok('B_rozlaczenie_zamyka_instancje', tB !== null, tB !== null ? `po ${tB.toFixed(0)} s` : `po 420 s dalej jest, swiaty: ${await lista()}`);
  bot = await polacz({ nick: NICK }); await sleep(4000);
  const swiat = await rcon(`data get entity ${NICK} Dimension`);
  ok('powrot_poza_lochem', !new RegExp(MAPA).test(swiat), swiat.slice(-40));
  bot.chat('/leave'); await sleep(1500); bot.quit();
}, { limitMs: 900000 });
