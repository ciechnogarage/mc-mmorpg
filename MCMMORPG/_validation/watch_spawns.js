// Podgląd spawnów w instancji lochu na żywo. Loguje TYLKO zmiany:
// pojawienie/zniknięcie mobów MythicMobs (fale, warden, boss, addy) + pozycję gracza.
// Użycie: node watch_spawns.js [instanceDim] [player]
const { Rcon } = require("rcon-client");
const DIM = process.argv[2] || "minecraft:level_1_0";
const PLAYER = process.argv[3] || "Miau";
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const clean = (s) => s.replace(/§./g, "");

(async () => {
  const r = await Rcon.connect({ host: "127.0.0.1", port: 25575, password: process.env.RCON_PASS });
  let prev = -1;
  let prevZone = "";
  for (;;) {
    // liczba żywych mobów (bez gracza/itemów/xp/armor_stand)
    await r.send(
      `execute in ${DIM} positioned 0 80 40 store result score #c mcnt if entity @e[distance=..400,type=!minecraft:player,type=!minecraft:item,type=!minecraft:experience_orb,type=!minecraft:armor_stand]`
    );
    const c = parseInt(
      clean(await r.send("scoreboard players get #c mcnt")).replace(/[^0-9-]/g, "") || "0",
      10
    );
    // pozycja gracza -> strefa
    let z = null;
    try {
      const p = clean(await r.send(`execute as ${PLAYER} run data get entity @s Pos`));
      const m = p.match(/\[([^,]+)d,\s*([^,]+)d,\s*([^\]]+)d\]/);
      if (m) z = parseFloat(m[3]);
    } catch (e) {}
    const zone =
      z == null ? "?" :
      z > 120 ? "OBÓZ(z>120)" :
      z > 100 ? "ścieżka-A(~111)" :
      z > 85 ? "ścieżka-B(~93)" :
      z > 68 ? "FALE(~74)" :
      z > 48 ? "WARDEN(~54)" :
      z > 20 ? "most/wąwóz" :
      z > -10 ? "ARENA/boss(~0)" : "komora-nagród(z<-10)";
    if (c !== prev || zone !== prevZone) {
      const t = new Date().toLocaleTimeString("pl-PL");
      let ids = "";
      if (c > 0) {
        ids = clean(
          await r.send(
            `execute in ${DIM} positioned 0 80 40 run execute as @e[distance=..400,type=!minecraft:player,type=!minecraft:item,type=!minecraft:experience_orb,type=!minecraft:armor_stand,limit=8] run data get entity @s id`
          )
        )
          .split("\n")
          .map((l) => (l.match(/"(minecraft:[a-z_]+)"/) || [])[1])
          .filter(Boolean)
          .join(",");
      }
      console.log(`${t} | mobów=${c} | gracz z=${z == null ? "?" : z.toFixed(0)} [${zone}]${ids ? " | " + ids : ""}`);
      prev = c;
      prevZone = zone;
    }
    await sleep(2000);
  }
})().catch((e) => console.log("ERR " + e.message));
