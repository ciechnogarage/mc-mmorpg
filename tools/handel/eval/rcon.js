'use strict'
const net = require('net')
// ---------- RCON (tylko pomiar i przygotowanie) ----------
function rconClient() {
  const sock = net.connect(25575, process.env.RCON_HOST || 'world1')
  let buf = Buffer.alloc(0); let id = 10; const pending = new Map()
  sock.on('data', d => {
    buf = Buffer.concat([buf, d])
    while (buf.length >= 4) {
      const len = buf.readInt32LE(0); if (buf.length < len + 4) break
      const rid = buf.readInt32LE(4); const body = buf.toString('utf8', 12, 4 + len - 2)
      buf = buf.subarray(4 + len)
      const p = pending.get(rid); if (p) { pending.delete(rid); p(body) }
    }
  })
  const send = (type, body) => new Promise(res => {
    const rid = ++id; const b = Buffer.from(body, 'utf8'); const pkt = Buffer.alloc(14 + b.length)
    pkt.writeInt32LE(10 + b.length, 0); pkt.writeInt32LE(rid, 4); pkt.writeInt32LE(type, 8); b.copy(pkt, 12)
    pending.set(rid, res); sock.write(pkt)
    setTimeout(() => { if (pending.has(rid)) { pending.delete(rid); res('') } }, 5000)
  })
  return new Promise((res, rej) => {
    sock.once('error', rej)
    sock.once('connect', async () => { await send(3, process.env.RCON_PW); res({ cmd: c => send(2, c), close: () => sock.end() }) })
  })
}
module.exports = { rconClient }
