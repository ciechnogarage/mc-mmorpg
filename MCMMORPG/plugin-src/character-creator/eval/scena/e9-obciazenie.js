'use strict'
// E9: N botow stoi w kreatorze (scena, nosiciele skorek) przez T sekund — do pomiaru kosztu.
const s = require('./lib/scena')
const { h, sleep } = s
;(async () => {
  const N = +(process.env.N || 6), T = +(process.env.T || 70)
  const bots = []
  for (let i = 0; i < N; i++) {
    try { const b = await s.join(`NapObc${i}_${Date.now() % 1000}`); await s.waitScene(b); bots.push(b) } catch (e) { console.log('blad', e.message) }
  }
  console.log('w scenie:', bots.filter(b => s.shield(b)).length)
  const enc = await s.rcon('execute in minecraft:creator_limbo if entity @e')
  const encG = await s.rcon('execute in minecraft:creator_limbo if entity @e[type=!player]')
  console.log('encje creator_limbo:', enc.trim(), '| bez graczy:', encG.trim())
  const t0 = Date.now()
  while (Date.now() - t0 < T * 1000) { console.log('mspt', (await s.rcon('mspt')).replace(/\s+/g, ' ').slice(0, 140)); await sleep(15000) }
  for (const b of bots) await h.quit(b)
  process.exit(0)
})()
