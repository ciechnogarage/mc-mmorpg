// Fallback: walidacja przez RCON (bez bota). Generuje swiat, forceloaduje
// regiony i odczytuje bloki przez `execute if block` (probkujac kandydatow).
const { Rcon } = require("rcon-client");
const RCON_PORT=25575, RCON_PASS=process.env.RCON_PASS, WORLD="dungeon_test";
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const CAND=["grass_block","ice","granite","deepslate_tiles","deepslate"];
const ZONE={grass_block:"START",ice:"MID",granite:"END",deepslate_tiles:"EMPTY",deepslate:"rock"};

(async()=>{
  const r=await Rcon.connect({host:"127.0.0.1",port:RCON_PORT,password:RCON_PASS});
  const send=c=>r.send(c);
  console.log("[rcon] ok");
  // utworz swiat jesli nie istnieje
  const cr=await send(`iris create ${WORLD} type=empty`);
  console.log("[iris create]", cr.slice(0,160));
  // czekaj na utworzenie swiata
  for(let i=0;i<60;i++){const w=await send(`execute in minecraft:overworld run say _`).catch(()=>"");
    const ok=await send(`mvtp @e[type=player,limit=0] ${WORLD}`).catch(()=>""); // noop probe
    const wl=await send(`mv list`).catch(()=>"");
    if(/dungeon_test/i.test(wl)){console.log("[world] widoczny w mv list po",i,"s");break;} await sleep(1000);}

  async function blockAt(x,y,z){
    for(const b of CAND){
      const out=await send(`execute in iris:${WORLD} positioned ${x} ${y} ${z} if block ${x} ${y} ${z} minecraft:${b} run data get block ${x} ${y} ${z}`).catch(e=>"ERR "+e.message);
      if(!/Test failed|Cannot|Unknown|ERR|No|not loaded/i.test(out)) return b;
    }
    return null;
  }
  // surface scan przez probowanie Y 62..56
  async function surface(x,z){ for(let y=62;y>=55;y--){const b=await blockAt(x,y,z); if(b) return [b,y];} return [null,null]; }

  // forceload caly footprint i probkuj
  const hist={};
  for(let z=-240; z<=240; z+=40){
    await send(`execute in iris:${WORLD} run forceload add ${-120} ${z-20} ${120} ${z+20}`).catch(()=>{});
  }
  await sleep(3000);
  for(let z=-240; z<=240; z+=20){
    for(let x=-100;x<=100;x+=20){
      const [b]=await surface(x,z); if(!b)continue; const t=ZONE[b]||b; hist[t]=(hist[t]||0)+1;
    }
  }
  console.log("[hist]",hist);
  await send(`execute in iris:${WORLD} run forceload remove all`).catch(()=>{});
  await r.end();
})().catch(e=>{console.error("FATAL",e);process.exit(1);});
