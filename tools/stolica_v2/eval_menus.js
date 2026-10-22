'use strict'
const H = require('/e/lib/harness')
const {rconClient} = require('/h/rcon')
async function main(){
  const name='EvCykl40232'
  const bot=await H.connect(name,{timeout:90000})
  const rc=await rconClient()
  try{
    await H.sleep(2000)
    for(const menu of ['stolica_v2_tablica','stolica_v2_podroze']){
      console.log('OPEN',menu,await rc.cmd(`dm open ${menu} ${name}`))
      await H.sleep(1500)
      console.log('WINDOW',menu,H.windowTitle(bot.currentWindow))
      if(bot.currentWindow) console.log('ITEMS',H.windowSlots(bot.currentWindow).filter(x=>x.slot<27).map(x=>x.label).join('|'))
    }
    for(const [service,command] of [
      ['naprawa',`core-station repair_station ${name}`],
      ['rozbiorka',`core-station deconstruct_station ${name}`],
      ['sakwa',`core-playervault sakwa ${name}`],
      ['kowal',`mi stations open profession-smithing ${name}`],
      ['chowance',`dm open chowance_menazeria ${name}`],
    ]){
      console.log('OPEN',service,await rc.cmd(command))
      await H.sleep(1000)
      console.log('WINDOW',service,H.windowTitle(bot.currentWindow))
    }
  } finally{rc.close();await H.quit(bot)}
}
main().catch(e=>{console.error(e.stack||e);process.exitCode=1})
