'use strict'
const L = require('/w/krytyka2/lib.js')
const { sleep } = L
const [NA] = process.env.EVAL_NICKS.split(' ')
;(async () => {
  const A = await L.gotowy(NA)
  const pos = async () => (await L.cmd(A, `minecraft:data get entity ${NA} Pos`, 800)).replace(/.*data: /, '')
  let pk = []
  A._client.on('packet', (d, m) => { if (/position|teleport/.test(m.name) && !/entity/.test(m.name)) pk.push(m.name + ' ' + JSON.stringify(d).slice(0, 200)) })
  console.log('start serwer', await pos(), 'bot', A.entity.position.toString())
  A.chat(`/minecraft:execute in minecraft:overworld run tp ${NA} 151.5 99 199.5`); await sleep(1500)
  console.log('po tp serwer', await pos(), 'bot', A.entity.position.toString())
  console.log('pakiety:', pk.join('\n  ')); pk = []
  // obejscie: recznie ustaw pozycje bota i wyslij ruch
  A.entity.position.set(151.5, 99, 199.5)
  await sleep(1500)
  console.log('po recznym ustawieniu serwer', await pos(), 'bot', A.entity.position.toString())
  A.chat(`/minecraft:execute in minecraft:overworld run tp ${NA} 146.5 99 194.5`); await sleep(300)
  A.entity.position.set(146.5, 99, 194.5); await sleep(1500)
  console.log('tp+ustawienie serwer', await pos(), 'bot', A.entity.position.toString())
  await L.H.quit(A); process.exit(0)
})().catch(e => { console.log('PRZERWANE ' + e.stack); process.exit(2) })
