'use strict'
/**
 * Odpowiada na zadanie resource packa w fazie CONFIGURATION.
 *
 * Bez tego bot NIGDY nie wchodzi do gry. Serwer wysyla 'add_resource_pack' i czeka na
 * status od klienta; mineflayer nie odpowiada, wiec polaczenie zostaje w fazie
 * configuration w nieskonczonosc — widac to jako strumien samych keep_alive i stan
 * klienta 'configuration' po 30 s. To dotyczy tak samo wejscia wprost, jak i przez proxy
 * (ta druga sciezka wyglada na "zawieszone przelaczanie serwera", ale przyczyna jest ta
 * sama — serwer docelowy tez wysyla swoj pack).
 *
 * Odpowiadamy jak klient, ktory packa przyjal i zaladowal: 3 = ACCEPTED, 0 = SUCCESS.
 * Bot zadnej tekstury nie widzi, ale serwer ma prawo wiedziec, ze czekanie sie skonczylo —
 * i dzieki temu testujemy sciezke gracza, ktory packa MA (czyli te normalna).
 */
const ACCEPTED = 3
const SUCCESSFULLY_LOADED = 0

function attach(client, { declined = false } = {}) {
  client.on('add_resource_pack', packet => {
    const uuid = packet.uuid
    if (declined) {
      client.write('resource_pack_receive', { uuid, result: 1 /* DECLINED */ })
      return
    }
    client.write('resource_pack_receive', { uuid, result: ACCEPTED })
    client.write('resource_pack_receive', { uuid, result: SUCCESSFULLY_LOADED })
  })
}

module.exports = { attach }
