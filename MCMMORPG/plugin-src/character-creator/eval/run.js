'use strict'
/** Uruchamia scenariusze po kolei i drukuje zwiezly raport. Kod wyjscia = liczba porazek. */
const fs = require('fs'); const path = require('path')
const { Results } = require('./lib/harness')
;(async () => {
  const dir = path.join(__dirname, 'scenarios')
  const only = process.argv[2]
  const files = fs.readdirSync(dir).filter(f => /^\d.*\.js$/.test(f))
    .filter(f => !only || f.includes(only)).sort()
  let failed = 0
  for (const f of files) {
    const r = new Results(f.replace(/\.js$/, ''))
    const t0 = Date.now()
    try { await require(path.join(dir, f))(r) }
    catch (e) { r.fail('scenariusz przerwany wyjatkiem', e.message) }
    r.print(); failed += r.failed
    console.log(`     (${((Date.now() - t0) / 1000).toFixed(0)} s)`)
  }
  console.log(failed ? `\nWYNIK: ${failed} NIEPOWODZEN` : '\nWYNIK: wszystko PASS')
  process.exit(failed ? 1 : 0)
})()
