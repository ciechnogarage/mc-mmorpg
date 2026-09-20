'use strict'
/**
 * Velocity "modern" player-info forwarding po stronie KLIENTA.
 *
 * Po co: bot musi wejsc na world1, a world1 ma 'velocity.enabled: true', wiec odrzuca
 * kazde polaczenie bez podpisanych danych od proxy. Droga przez proxy odpada z innego
 * powodu: przelaczenie serwera w Velocity wraca do fazy CONFIGURATION, a mineflayer tego
 * nie przechodzi — bot zawisa (proxy melduje "-> world1 has connected", a w logu world1
 * nigdy nie pada "joined the game"). Sprawdzone, nie zalozone.
 *
 * Wiec bot podpisuje te dane sam, tym samym sekretem co proxy, i laczy sie WPROST
 * z world1:25565. Zadnej zmiany po stronie produkcji: nie ruszamy velocity.toml, nie
 * restartujemy proxy, nie dokladamy forced-hosts.
 *
 * Format ladunku (VelocityProxy#createForwardingData, wersja 1):
 *   signature(32 B HMAC-SHA256) || varint(version) || string(adres) || uuid || string(nick)
 *   || varint(liczba properties)
 */
const crypto = require('crypto')

const varInt = value => {
  const bytes = []
  let v = value
  do {
    let b = v & 0x7f
    v >>>= 7
    if (v !== 0) b |= 0x80
    bytes.push(b)
  } while (v !== 0)
  return Buffer.from(bytes)
}

const str = s => {
  const b = Buffer.from(s, 'utf8')
  return Buffer.concat([varInt(b.length), b])
}

/** UUID trybu offline: v3/MD5 z "OfflinePlayer:<nick>" — tak liczy je sam serwer. */
function offlineUuid(username) {
  const md5 = crypto.createHash('md5').update(`OfflinePlayer:${username}`, 'utf8').digest()
  md5[6] = (md5[6] & 0x0f) | 0x30
  md5[8] = (md5[8] & 0x3f) | 0x80
  return md5
}

function forwardingData(version, username, address) {
  return Buffer.concat([
    varInt(version),
    str(address),
    offlineUuid(username),
    str(username),
    varInt(0), // bez properties: bot nie ma skorki i nie potrzebuje jej do asercji
  ])
}

/**
 * Podpina odpowiedz na 'velocity:player_info'. Wolac NATYCHMIAST po createBot —
 * zadanie przychodzi w fazie logowania.
 */
function attach(client, { secret, username, address = '127.0.0.1' }) {
  // mineflayer ma WLASNY handler tego zdarzenia i odpowiada pierwszy — pusta odpowiedzia
  // (data=BRAK), czyli "nie obsluguje tego kanalu". Serwer bierze pierwsza odpowiedz na
  // dany messageId i odrzuca polaczenie ("This server requires you to connect with
  // Velocity"), zanim nasza zdazy dojsc. Widac to bylo dopiero po podejrzeniu write():
  //   -> odpowiedz: msgId=1563699001 data=BRAK
  //   -> odpowiedz: msgId=1563699001 data=72B
  // Dlatego przejmujemy zdarzenie w calosci: ponizej i tak odpowiadamy na KAZDY kanal,
  // wiec nic nie zostaje bez odpowiedzi.
  client.removeAllListeners('login_plugin_request')
  client.on('login_plugin_request', packet => {
    if (!String(packet.channel).includes('velocity')) {
      client.write('login_plugin_response', { messageId: packet.messageId, data: undefined })
      return
    }
    // Serwer podaje najwyzsza wersje, jaka obsluguje; wersja 1 wystarcza i nie wymaga kluczy.
    const requested = packet.data && packet.data.length ? packet.data.readUInt8(0) : 1
    const version = Math.min(requested, 1)
    const payload = forwardingData(version, username, address)
    const signature = crypto.createHmac('sha256', Buffer.from(secret, 'utf8')).update(payload).digest()
    client.write('login_plugin_response', {
      messageId: packet.messageId,
      data: Buffer.concat([signature, payload]),
    })
  })
}

module.exports = { attach, offlineUuid }
