'use strict'
const L = require('/w/krytyka2/lib.js')
const [NA] = process.env.EVAL_NICKS.split(' ')
;(async () => {
  const A = await L.gotowy(NA)
  A.on('actionBar', m => console.log('ACTIONBAR', m.toString()))
  A._client.on('packet', (d, m) => { if (/position|teleport|respawn|login/.test(m.name) && !/entity/.test(m.name)) console.log('PKT', m.name, JSON.stringify(d).slice(0, 160)) })
  A.on('message', m => { const s = m.toString(); if (!/[❤♦⛨]/.test(s)) console.log('MSG', s.slice(0, 160)) })
  await L.sleep(40000); await L.H.quit(A); process.exit(0)
})()
