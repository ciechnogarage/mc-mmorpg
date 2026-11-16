const w = require('./lib/w')
;(async () => { for (const c of process.argv.slice(2)) console.log('>', c, '\n', (await w.rcon(c)).replace(/§./g, '')) })().catch(e => { console.error(e); process.exit(1) })
