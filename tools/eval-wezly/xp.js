'use strict'
/**
 * Odczyt XP profesji - wspolny dla wszystkich evalow.
 *
 * PULAPKA, na ktorej juz raz polegl ten test: odpowiedz MMOCore ma kody koloru
 * MIEDZY slowem "has" a liczba ("has §e12§7 EXP in §eGórnictwo"), wiec naiwne
 * /has\s+(\d+)\s+EXP/ nie lapie nic i zwraca NaN. Najpierw zdejmujemy formatowanie
 * (§x oraz sekwencje ANSI), dopiero potem parsujemy.
 *
 * Zwraca liczbe albo NaN. NIGDY nie zwraca 0 przy bledzie odczytu - 0 to poprawna
 * wartosc XP i pomylenie jej z bledem juz raz przepuscilo falszywy PASS.
 */
const oczysc = s => String(s).replace(/§./g, '').replace(/\u001b\[[0-9;]*m/g, '')

async function odczytXp(bot, nick, profesja, { wait = 2000, debug = true } = {}) {
  bot.evalLog.length = 0
  bot.chat(`/mmocore admin exp check ${nick} ${profesja}`)
  await new Promise(r => setTimeout(r, wait))
  const txt = oczysc(bot.evalLog.join(' | '))
  const m = txt.match(/has\s+([0-9]+(?:[.,][0-9]+)?)\s+EXP/i)
  if (!m && debug) console.log(`  [xp] nie sparsowano odpowiedzi dla ${profesja}: ${JSON.stringify(txt.slice(-300))}`)
  return m ? parseFloat(m[1].replace(',', '.')) : NaN
}

module.exports = { odczytXp, oczysc }
