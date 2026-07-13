const WebSocket = require('ws');

function connectCDP(wsUrl) {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(wsUrl, { maxPayload: 1024 * 1024 * 200 });
    let id = 0;
    const pending = new Map();
    ws.on('open', () => resolve(client));
    ws.on('error', reject);
    ws.on('message', (data) => {
      const msg = JSON.parse(data.toString());
      if (msg.id && pending.has(msg.id)) {
        const { resolve, reject } = pending.get(msg.id);
        pending.delete(msg.id);
        if (msg.error) reject(new Error(JSON.stringify(msg.error)));
        else resolve(msg.result);
      }
    });
    const client = {
      send(method, params = {}) {
        return new Promise((res, rej) => {
          const thisId = ++id;
          pending.set(thisId, { resolve: res, reject: rej });
          ws.send(JSON.stringify({ id: thisId, method, params }));
        });
      },
      close() { ws.close(); },
    };
  });
}

module.exports = { connectCDP };
