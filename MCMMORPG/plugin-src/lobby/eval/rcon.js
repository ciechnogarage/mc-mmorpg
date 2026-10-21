const net = require('net');
class Rcon {
  static connect(host, port, pass) {
    return new Promise((res, rej) => {
      const r = new Rcon(); r.sock = net.connect(port, host); r.id = 1; r.pending = new Map(); r.buf = Buffer.alloc(0);
      r.sock.on('data', d => { r.buf = Buffer.concat([r.buf, d]); while (r.buf.length >= 4) { const len = r.buf.readInt32LE(0); if (r.buf.length < 4 + len) break; const id = r.buf.readInt32LE(4); const body = r.buf.slice(12, 4 + len - 2).toString(); r.buf = r.buf.slice(4 + len); const cb = r.pending.get(id); if (cb) { r.pending.delete(id); cb(body); } } });
      r.sock.on('error', rej);
      r.sock.on('connect', () => r._send(3, pass).then(() => res(r)));
    });
  }
  _send(type, body) { const id = this.id++; const b = Buffer.from(body); const p = Buffer.alloc(14 + b.length); p.writeInt32LE(10 + b.length, 0); p.writeInt32LE(id, 4); p.writeInt32LE(type, 8); b.copy(p, 12); return new Promise(res => { this.pending.set(id, res); this.sock.write(p); }); }
  send(cmd) { return this._send(2, cmd); }
  end() { this.sock.end(); }
}
module.exports = { Rcon };
