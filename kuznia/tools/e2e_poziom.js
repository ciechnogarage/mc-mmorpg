// E2E poziomu: bot przechodzi loch tak jak gracz i raportuje PASS/FAIL.
// Punkty zasadzek czytamy z argumentow (zgodne z build_md.py).
const mineflayer = require('/e/node_modules/mineflayer');
const sleep = ms => new Promise(r => setTimeout(r, ms));
const NICK = process.env.NICK || 'E2EBot', DUN = process.env.DUN || 'poziom_01';
const PTS = JSON.parse(process.env.PTS);            // {nazwa:[x,y,z], ...}, arena ostatnia
const bot = mineflayer.createBot({ host: process.env.MC_HOST || 'klon-kuznia', port: 25565, username: NICK, auth: 'offline' });
const R = []; const res = (ok, what, info = '') => { R.push(ok); console.log(`${ok ? 'PASS' : 'FAIL'}  ${what}${info ? '  -- ' + info : ''}`); };
let world = null, changes = 0; const titles = [], chat = [], bossbars = [];
bot._client.on('respawn', p => { changes++; world = p.worldState ? p.worldState.name : String(p.worldName); });
const strs = (o, out = []) => { if (typeof o === 'string') out.push(o); else if (o && typeof o === 'object') for (const v of Object.values(o)) strs(v, out); return out; };
const ttxt = v => strs(v).filter(x => !['compound','string','byte','list','int'].includes(x) && !/^#?[0-9a-f]{6}$|^(white|gray|green|gold|light_purple|dark_gray|red)$/i.test(x)).join(' ');
const txt = v => { try { return JSON.stringify(v).replace(/\\u00a7./g,'').replace(/[{}"\[\]]/g,' ').replace(/\s+/g,' ').slice(0, 90); } catch { return String(v); } };
for (const pk of ['set_title_text', 'set_title_subtitle', 'title']) bot._client.on(pk, d => titles.push(`${pk}:${ttxt(d)}`));
bot._client.on('boss_bar', d => { if (d.title) bossbars.push(ttxt(d.title)); });
const effects = new Set();
bot._client.on('packet', (d, meta) => {
  if (/title/i.test(meta.name) && !['set_title_text','set_title_subtitle','title'].includes(meta.name)) titles.push(`${meta.name}:${txt(d)}`);
  if (meta.name === 'entity_effect' && bot.entity && d.entityId === bot.entity.id) effects.add(d.effectId);
});
bot.on('messagestr', m => { if (m.trim()) chat.push(m.slice(0, 140)); });
bot.on('bossBarCreated', b => bossbars.push(String(b.title).replace(/§./g, '')));
bot.on('kicked', r => console.log('[KICK]', r)); bot.on('error', e => console.log('[ERR]', e.message));
let moves = 0; bot.on('forcedMove', () => moves++);
async function waitMove(prev, ms = 8000) { for (let i = 0; i < ms / 200 && moves === prev; i++) await sleep(200); return moves > prev; }
async function wiggle() { await bot.waitForChunksToLoad().catch(() => {}); bot.setControlState('forward', true); bot.setControlState('jump', true);
  await sleep(900); bot.clearControlStates(); bot.setControlState('back', true); await sleep(600); bot.clearControlStates(); }
function mobsNear(p, r = 22) {
  const out = {};
  for (const e of Object.values(bot.entities)) {
    if (e === bot.entity || e.type === 'player' || !e.position || e.name === 'armor_stand' || e.name === 'item') continue;
    if (e.position.distanceTo({ x: p[0], y: e.position.y, z: p[2] }) > r) continue;
    let nm = e.name; try { const c = e.getCustomName && e.getCustomName(); if (c) nm += `"${c.toString().replace(/§./g, '')}"`; } catch {}
    out[nm] = (out[nm] || 0) + 1;
  }
  return out;
}
bot.once('spawn', async () => {
  await sleep(2500);
  const before = changes, mv0 = moves; bot.chat(`/md play ${DUN}`);
  for (let i = 0; i < 40 && changes === before; i++) await sleep(250);
  await waitMove(mv0);
  res(changes > before && /poziom_01_\d+/.test(world), 'wejscie do instancji', `swiat=${world}`);
  // ladowanie
  const y0 = bot.entity.position.y, hp0 = bot.health; let landed = null;
  const slow = () => Object.keys(bot.entity.effects || {}).map(Number).includes(28);
  let sawSlow = false;
  for (let i = 0; i < 60; i++) { if (slow()) sawSlow = true; if (i > 2 && bot.entity.onGround) { landed = bot.entity.position.y; break; } await sleep(300); }
  res(y0 > 104, 'start nad wyspa', `y0=${y0.toFixed(1)}`);
  res(sawSlow || [...effects].includes(27), 'powolne opadanie nalozone', `efekty(pakiety)=${JSON.stringify([...effects])}`);
  res(landed !== null && landed > 95 && landed < 106, 'ladowanie na wyspie', `y=${landed}`);
  res(bot.health >= hp0, 'bez obrazen przy ladowaniu', `hp ${hp0}->${bot.health}`);
  for (let i = 0; i < 30 && !titles.some(t => /Poziom 1/.test(t)); i++) await sleep(250);
  await sleep(1500);
  res(titles.some(t => /Poziom 1/.test(t)), 'tytul startu', titles.slice(0, 3).join(' | '));
  const hares = mobsNear([PTS.start[0], 0, PTS.start[2]], 20);
  res((hares.rabbit || 0) + Object.keys(hares).filter(k => k.startsWith('rabbit')).reduce((a, k) => a + hares[k], 0) > 0, 'zajace przy starcie', JSON.stringify(hares));
  // reguly swiata ustawiane przez skill przybycia (<caster.l.w>) - sprawdzenie z wnetrza instancji
  chat.length = 0; bot.chat('/gamerule fall_damage'); bot.chat('/gamerule keep_inventory'); bot.chat('/time query daytime');
  await sleep(3000); bot.chat('/time query daytime'); await sleep(1500);
  const times = chat.filter(m => /The time is/.test(m)).map(m => +m.match(/-?\d+/)[0]);
  res(chat.some(m => /fall_damage.*false/.test(m)), 'regula: brak obrazen od upadku', chat.filter(m => /fall_damage/.test(m)).join(''));
  res(chat.some(m => /keep_inventory.*true/.test(m)), 'regula: zachowanie ekwipunku', chat.filter(m => /keep_inventory/.test(m)).join(''));
  res(times.length === 2 && times[0] === times[1] && times[0] === 1000, 'czas zamrozony na 1000', JSON.stringify(times));
  bot.chat('/effect give @s minecraft:resistance 900 4 true');
  // zasadzki
  for (const [name, p] of Object.entries(PTS)) {
    if (name === 'start' || name === 'arena') continue;
    { const m0 = moves; bot.chat(`/tp @s ${p[0]} ${p[1] + 6} ${p[2]}`); await waitMove(m0); await sleep(1500); await wiggle(); await sleep(4000); }
    const m = mobsNear(p); const n = Object.values(m).reduce((a, b) => a + b, 0);
    res(n > 0, `zasadzka ${name}`, JSON.stringify(m));
  }
  // arena i boss
  const A = PTS.arena; titles.length = 0;
  { const m0 = moves; bot.chat(`/tp @s ${A[0]} ${A[1] + 6} ${A[2]}`); await waitMove(m0); await sleep(1500); await wiggle(); await sleep(6000); }
  const am = mobsNear(A, 20);
  res(Object.keys(am).some(k => /Stra/.test(k)), 'boss pojawil sie na arenie', JSON.stringify(am));
  res(bossbars.some(b => /Stra/.test(b)), 'pasek bossa', JSON.stringify([...new Set(bossbars)]));
  res(titles.some(t => /Stra/.test(t)), 'tytul bossa', titles.slice(0, 2).join(' | '));
  chat.length = 0; titles.length = 0;
  bot.chat(`/damage @e[type=wolf,sort=nearest,limit=1,distance=..25] 999 minecraft:player_attack by ${NICK}`);
  await sleep(9000);
  res(titles.some(t => /oczyszczona|ukończony|ukonczony/.test(t)), 'tytul zwyciestwa', titles.slice(0, 3).join(' | '));
  res(chat.some(m => /finish|complete|ukończ|zakończ|Dungeon/i.test(m)), 'loch oznaczony jako ukonczony', chat.slice(0, 4).join(' || '));
  const b2 = changes; bot.chat('/md leave');
  for (let i = 0; i < 40 && changes === b2; i++) await sleep(250);
  res(changes > b2 && !/poziom_01_/.test(world), 'wyjscie z lochu', `swiat=${world}`);
  console.log(`\nWYNIK: ${R.filter(Boolean).length}/${R.length} PASS`);
  bot.quit(); setTimeout(() => process.exit(R.every(Boolean) ? 0 : 1), 1000);
});
setTimeout(() => { console.log('[FATAL] watchdog'); process.exit(9); }, 180000);
