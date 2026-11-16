'use strict'
// Minimalny klient RCON (protokol Source) dla evalow w kontenerze node:20.
const net = require('net')
function rcon(cmd) {
  return new Promise((resolve, reject) => {
    const s = net.connect(25575, process.env.MC_HOST || 'world1')
    const pkt = (id, type, body) => {
      const b = Buffer.from(body, 'utf8'); const p = Buffer.alloc(14 + b.length)
      p.writeInt32LE(10 + b.length, 0); p.writeInt32LE(id, 4); p.writeInt32LE(type, 8)
      b.copy(p, 12); return p
    }
    let buf = Buffer.alloc(0); let authed = false
    s.on('data', d => {
      buf = Buffer.concat([buf, d])
      while (buf.length >= 4 && buf.length >= buf.readInt32LE(0) + 4) {
        const len = buf.readInt32LE(0); const id = buf.readInt32LE(4)
        const body = buf.slice(12, 4 + len - 2).toString('utf8'); buf = buf.slice(4 + len)
        if (!authed) { if (id === -1) return reject(new Error('rcon auth')); authed = true; s.write(pkt(2, 2, cmd)) }
        else { s.end(); resolve(body.replace(/§./g, '')) }
      }
    })
    s.on('error', reject)
    s.write(pkt(1, 3, process.env.RCON_PASS))
  })
}

module.exports = { rcon }
