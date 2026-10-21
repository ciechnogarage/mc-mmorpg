// @ts-check
'use strict';

// Warstwa ochrony przed PartialReadError (plan sekcja D).
// ModelEngine/LibsDisguises/MythicMobs generują entity metadata, które potrafią
// łamać parser protodef (mineflayer #3756, #3669). Ta warstwa:
//  - przechwytuje błędy klienta protokołu zamiast pozwolić im zabić proces,
//  - zlicza błędy per nazwa pakietu (diagnostyka izolacyjna: który plugin winny),
//  - klasyfikuje błąd jako recoverable (parser) vs fatal (socket/kick).

const PACKET_NAME_RE = /packet_([a-z_]+)/;

function classify(err) {
  const msg = String(err && (err.message || err));
  if (err && (err.name === 'PartialReadError' || msg.includes('PartialReadError'))) return 'partial-read';
  if (msg.includes('Unexpected buffer end') || msg.includes('Missing characters in string')) return 'partial-read';
  if (msg.includes('Chunk size') || msg.includes('deserialization')) return 'deserialization';
  if (msg.includes('ECONNRESET') || msg.includes('ECONNREFUSED') || msg.includes('socketClosed')) return 'socket';
  return 'unknown';
}

function packetName(err) {
  const src = String((err && err.stack) || (err && err.message) || err);
  const m = src.match(PACKET_NAME_RE);
  return m ? m[1] : null;
}

// Podpina bezpieczną obsługę błędów do instancji bota mineflayer.
// Zwraca obiekt statystyk; recoverable błędy NIE propagują dalej.
function attachSafeProtocol(bot, log = console) {
  const stats = {
    total: 0,
    byKind: {},
    byPacket: {},
    lastError: null,
    // log zdarzeń z timestampami — pod regułę korelacji z asercjami QA:
    // recoverable na pakiecie istotnym dla asercji w jej oknie => BLOCKED,
    // bo dropniętego pakietu nie da się odróżnić od "brak zdarzenia".
    events: []
  };

  function record(err, origin) {
    const kind = classify(err);
    const pkt = packetName(err);
    stats.total += 1;
    stats.byKind[kind] = (stats.byKind[kind] || 0) + 1;
    if (pkt) stats.byPacket[pkt] = (stats.byPacket[pkt] || 0) + 1;
    stats.events.push({ at: Date.now(), kind, packet: pkt });
    if (stats.events.length > 5000) stats.events.shift();
    stats.lastError = { kind, packet: pkt, message: String(err && err.message || err), origin, at: new Date().toISOString() };
    if (kind === 'partial-read' || kind === 'deserialization') {
      // recoverable: loguj i jedź dalej — zdarzenie z tego pakietu jest stracone
      if (stats.byKind[kind] <= 5 || stats.byKind[kind] % 100 === 0) {
        log.warn(`[safe-protocol] ${kind} (${pkt || '?'}) x${stats.byKind[kind]}: ${String(err.message).slice(0, 160)}`);
      }
      return true;
    }
    log.error(`[safe-protocol] ${kind} (${origin}): ${String(err && err.message || err)}`);
    return false;
  }

  bot.on('error', err => { record(err, 'bot'); });
  if (bot._client) {
    bot._client.on('error', err => { record(err, 'client'); });
  }
  return stats;
}

// Global error handling per-proces (plan D: twarda izolacja awarii).
// Wywołać RAZ na starcie procesu bota.
function installProcessGuards(log = console, onFatal = null) {
  process.on('uncaughtException', err => {
    const kind = classify(err);
    if (kind === 'partial-read' || kind === 'deserialization') {
      log.warn(`[safe-protocol] uncaught ${kind} zignorowany: ${String(err.message).slice(0, 160)}`);
      return;
    }
    log.error('[safe-protocol] uncaughtException (fatal):', err);
    if (onFatal) onFatal(err);
    else process.exit(1);
  });
  process.on('unhandledRejection', err => {
    log.error('[safe-protocol] unhandledRejection:', err);
  });
}

const RECOVERABLE_KINDS = new Set(['partial-read', 'deserialization']);

/**
 * Recoverable błędy protokołu w oknie czasowym, opcjonalnie zawężone do
 * pakietów istotnych dla scenariusza (null = dowolny pakiet, w tym
 * niesparsowana nazwa — konserwatywnie liczy się jako trafienie).
 * @param {{events: Array<{at:number,kind:string,packet:string|null}>}} stats
 * @param {number} startMs @param {number} endMs
 * @param {string[]|null} packets
 */
function recoverableInWindow(stats, startMs, endMs, packets = null) {
  if (!stats || !Array.isArray(stats.events)) return [];
  return stats.events.filter(e =>
    e.at >= startMs && e.at <= endMs
    && RECOVERABLE_KINDS.has(e.kind)
    && (packets == null || e.packet == null || packets.includes(e.packet))
  );
}

module.exports = { attachSafeProtocol, installProcessGuards, classify, packetName, recoverableInWindow };
