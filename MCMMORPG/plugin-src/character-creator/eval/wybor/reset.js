// Stan startowy evalu: kosz pusty (wszystko przywrocone konsola), Morth istnieje.
const fs = require('fs')
const w = require('./lib/w')
;(async () => {
  const out = (await w.rcon('cc kosz')).replace(/§./g, '')
  for (const m of out.matchAll(/profil ([0-9a-f-]{36})/g)) console.log((await w.rcon('cc kosz przywroc ' + m[1])).replace(/§./g, ''))
  const chars = fs.readFileSync('/clone-data/plugins/CharacterCreator/characters.yml', 'utf8')
  console.log('Morth jest:', /name: Morth/.test(chars))
  process.exit(/name: Morth/.test(chars) ? 0 : 3)
})().catch(e => { console.error(e); process.exit(1) })
