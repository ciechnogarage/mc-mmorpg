const {rcon}=require('/s/rcon.js');(async()=>{for(const c of process.argv.slice(2))console.log('> '+c+'\n'+await rcon(c))})()
