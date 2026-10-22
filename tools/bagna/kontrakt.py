"""KONTRAKT STANOW Bagien - jedno zrodlo prawdy dla modelarzy (Codex) i generatora MM (mm_bagna.py).
Zmiana tu = zmiana KONTRAKT_STANOW.md (python3 kontrakt.py) i bramki generatora.
Czasy w sekundach. hit = kiedy (od startu animacji) MM odpala efekt/pocisk/obrazenia. windup = czas zapowiedzi
z karty (KARTY.md). Modelarz moze nadpisac czasy w output/lochy/bagna/modele/<id>/czasy.json:
  {"<anim>": {"dl": s, "windup": s, "hit": s, "recover": s}}  (dl = dlugosc animacji)
Generator bierze czasy.json gdy istnieje, ale bramka pilnuje: |windup_json - windup_kontraktu| <= 0.1 s (zapowiedz z karty
jest wiazaca), hit >= windup, hit <= dl.
"""
import json, os

# typ: L = petla (loop), R = raz (play once), T = trzymaj ostatnia klatke (hold) do zdjecia stanu przez MM
# (nazwa, typ, dl, windup, hit, recover)
A = lambda n, t, dl, w=0.0, h=None, r=0.0: dict(n=n, typ=t, dl=dl, windup=w, hit=(w if h is None else h), recover=r)
OBOWIAZKOWE = ("idle", "walk", "attack", "hurt", "death", "spawn")

MOBY = {
 "pijawka": dict(rola="roj", miejsce="L002", hitbox=(0.6, 0.45), atlas=128, anim=[
    A("idle", "L", 2.0), A("walk", "L", 0.8),
    A("attack", "R", 0.5, 0.0, 0.15, 0.3),
    A("wyskok", "R", 1.1, 0.6, 0.9, 0.2),          # zwiniecie 0.6 s -> skok, ladowanie/klapniecie 0.9
    A("hurt", "R", 0.3), A("death", "R", 1.0, 0.0, 0.8, 0.0), A("spawn", "R", 0.8)],
    kosci=["tag_name", "hitbox", "swiatlo_paszcza", "p_paszcza_pkt"]),
 "mulowiec": dict(rola="bruiser", miejsce="L002", hitbox=(1.3, 2.2), atlas=128, anim=[
    A("idle", "L", 2.4), A("walk", "L", 1.4),
    A("attack", "R", 1.4, 0.8, 0.9, 0.5),          # ZAMACH LOPATA: lopata za plecy 0.8 s -> luk
    A("throw", "R", 1.4, 1.0, 1.05, 0.35),         # nabranie mulu 1.0 s -> wyrzut
    A("hurt", "R", 0.35), A("death", "R", 1.0, 0.0, 0.8, 0.0), A("spawn", "R", 1.5)],
    kosci=["tag_name", "hitbox", "swiatlo_rana", "p_lopata_pkt", "p_dlon", "fx_mul"]),
 "kosciej": dict(rola="dystans", miejsce="L002", hitbox=(0.7, 2.0), atlas=128, anim=[
    A("idle", "L", 2.0), A("walk", "L", 1.2),
    A("attack", "R", 0.6, 0.0, 0.2, 0.4),
    A("shoot", "R", 1.6, 1.2, 1.25, 0.35),         # namierzanie 1.2 s (drzenie) -> puszczenie
    A("sink", "R", 0.5, 0.5, 0.5, 0.0),            # zapadniecie (zapowiedz odskoku)
    A("emerge", "R", 0.6),
    A("hurt", "R", 0.3), A("death", "R", 1.0, 0.0, 0.8, 0.0), A("spawn", "R", 1.2)],
    kosci=["tag_name", "hitbox", "swiatlo_oczy", "swiatlo_grot", "p_oczy", "p_cieciwa", "fx_strzala"]),
 "szeptucha": dict(rola="zaklinacz", miejsce="L002", hitbox=(0.8, 1.8), atlas=128, anim=[
    A("idle", "L", 2.4), A("walk", "L", 1.4),
    A("attack", "R", 0.6, 0.0, 0.2, 0.4),
    A("channel", "L", 1.0, 3.0, 3.0, 0.0),         # petla mieszania; kanal trwa 3 s (MM zdejmuje stan)
    A("throw", "R", 1.2, 0.8, 0.85, 0.35),         # flaszka
    A("push", "R", 0.9, 0.6, 0.65, 0.25),          # szept (pochylenie 0.6 s)
    A("interrupted", "R", 1.0, 0.0, 0.3, 0.7),     # kociol sie przewraca
    A("hurt", "R", 0.3), A("death", "R", 1.0, 0.0, 0.8, 0.0), A("spawn", "R", 1.0)],
    kosci=["tag_name", "hitbox", "swiatlo_kociol", "swiatlo_oczy", "p_kociol_pkt", "p_dlon", "fx_flaszka"]),
 "kokoniara": dict(rola="elita", miejsce="L002", hitbox=(2.0, 1.6), atlas=256, anim=[
    A("idle", "L", 2.0), A("walk", "L", 1.0),
    A("attack", "R", 0.6, 0.0, 0.2, 0.4),
    A("spin", "R", 1.4, 1.0, 1.05, 0.35),          # KOKON: odwlok do przodu, przedza swieci 1 s -> strzal
    A("leap_prep", "T", 0.8, 0.8, 0.8, 0.0),       # przysiad 0.8 s (trzymaj do leap)
    A("leap", "R", 0.7, 0.0, 0.4, 0.3),            # lot + ladowanie (fala w 0.4)
    A("hatch", "R", 1.5, 0.0, 0.9, 0.6),           # rozpruty odwlok, mlode w 0.9
    A("hurt", "R", 0.3), A("death", "R", 1.0, 0.0, 0.8, 0.0), A("spawn", "R", 1.5)],
    kosci=["tag_name", "hitbox", "swiatlo_oczy", "swiatlo_kokon", "p_kadziel", "p_odwlok_pkt", "fx_kokon"]),
 "kokoniatko": dict(rola="roj", miejsce="L002", hitbox=(0.6, 0.5), atlas=128, anim=[
    A("idle", "L", 1.2), A("walk", "L", 0.5),
    A("attack", "R", 0.4, 0.0, 0.12, 0.28),
    A("leap", "R", 0.9, 0.5, 0.7, 0.2),            # przysiad 0.5 s -> doskok
    A("hurt", "R", 0.25), A("death", "R", 1.0, 0.0, 0.8, 0.0), A("spawn", "R", 0.8)],
    kosci=["tag_name", "hitbox", "swiatlo_worek"]),
 "rechotnik": dict(rola="zabolud", miejsce="Mokradla", hitbox=(1.0, 1.4), atlas=128, anim=[
    A("idle", "L", 2.0), A("walk", "L", 0.9),
    A("attack", "R", 0.7, 0.0, 0.25, 0.45),        # dzgniecie wlocznia (melee vanilla)
    A("croak", "R", 1.0, 1.0, 1.0, 0.0),           # nadecie worka 1 s = zapowiedz jezyka
    A("tongue", "R", 0.6, 0.0, 0.15, 0.45),        # wyrzut (fx_jezyk skala) i sciagniecie; przyciagniecie w 0.15
    A("slam", "R", 0.9, 0.4, 0.55, 0.35),          # plasniecie po przyciagnieciu
    A("hurt", "R", 0.3), A("death", "R", 1.2), A("spawn", "R", 1.0)],
    kosci=["tag_name", "hitbox", "swiatlo_worek", "swiatlo_oczy", "p_paszcza", "fx_jezyk"]),
 "slimak": dict(rola="tank", miejsce="Mokradla", hitbox=(1.6, 1.6), atlas=128, anim=[
    A("idle", "L", 3.0), A("walk", "L", 2.0),
    A("attack", "R", 0.8, 0.0, 0.3, 0.5),
    A("hide", "T", 0.5, 0.0, 0.5, 0.0),            # wciagniecie -> trzymaj
    A("hidden", "L", 1.0),                         # petla w skorupie (3 s, MM zdejmuje)
    A("emerge_spit", "R", 1.3, 0.8, 0.85, 0.45),   # drzenie skorupy 0.8 s -> wyrzut sluzu
    A("hurt", "R", 0.35), A("death", "R", 1.5), A("spawn", "R", 1.2)],
    kosci=["tag_name", "hitbox", "swiatlo_oko_l", "swiatlo_oko_r", "swiatlo_pekniecie", "p_glowa", "p_pekniecie"]),
 "ognik": dict(rola="wabik", miejsce="Mokradla", hitbox=(0.7, 0.9), atlas=128, anim=[
    A("idle", "L", 2.0), A("walk", "L", 1.0),      # walk = idle z pochyleniem (lata)
    A("attack", "R", 0.5, 0.0, 0.2, 0.3),
    A("flee", "R", 0.6),
    A("charge", "R", 1.0, 1.0, 1.0, 0.0),          # puchniecie rdzenia 1 s
    A("explode", "R", 0.6, 0.0, 0.1, 0.5),
    A("blink", "R", 0.4, 0.0, 0.2, 0.2),
    A("hurt", "R", 0.3), A("death", "R", 1.0), A("spawn", "R", 0.8)],
    kosci=["tag_name", "hitbox", "swiatlo_rdzen", "swiatlo_plomien", "p_rdzen"]),
 "krol_ropuch": dict(rola="elita/boss", miejsce="Mokradla", hitbox=(2.6, 3.0), atlas=256, anim=[
    A("idle", "L", 3.0), A("walk", "L", 1.6),
    A("attack", "R", 1.0, 0.0, 0.35, 0.65),        # plasniecie lapa (melee vanilla)
    A("swallow_prep", "T", 1.2, 1.2, 1.2, 0.0),    # rozwarcie paszczy 1.2 s
    A("swallow", "R", 0.6, 0.0, 0.2, 0.4),         # klapniecie (polkniecie w 0.2)
    A("spit", "R", 0.8, 0.0, 0.35, 0.45),          # wyplucie
    A("tongue", "R", 1.6, 1.0, 1.15, 0.45),        # jezyk krolewski: zapowiedz 1 s
    A("leap_prep", "T", 1.0, 1.0, 1.0, 0.0),
    A("leap", "R", 0.8, 0.0, 0.6, 0.0),
    A("land", "R", 0.8, 0.0, 0.05, 0.75),
    A("summon", "R", 1.6, 0.0, 1.0, 0.6),          # rechot wezwania, rechotniki w 1.0
    A("hurt", "R", 0.35), A("death", "R", 2.0), A("spawn", "R", 2.0)],
    kosci=["tag_name", "hitbox", "swiatlo_worek", "swiatlo_oczy", "swiatlo_brodawki", "swiatlo_korona", "p_paszcza", "fx_jezyk", "fx_korona"]),
}

# animacje, ktorych uderzenie MM moze (po tescie na klonie) odpalac skryptem klatki ME
KLATKA = lambda mob, anim: f"BAGNA_{mob.upper()}_{anim.upper()}_KLATKA"

def ma_klatke(a):
    """uderzenie w tej animacji (nie sama zapowiedz/petla): typ R, odslona > 0"""
    return a["typ"] == "R" and a["recover"] > 0 and a["hit"] > 0

def czasy(mob, baza="/opt/docker/mc/data/world1/output/lochy/bagna/modele"):
    """kontrakt + nadpisania z czasy.json modelarza (z bramka)."""
    out = {a["n"]: dict(a) for a in MOBY[mob]["anim"]}
    p = f"{baza}/{mob}/czasy.json"
    if os.path.exists(p):
        js = json.load(open(p))
        for n, v in js.items():
            assert n in out, f"{p}: animacja {n} spoza kontraktu"
            k = out[n]
            if k["windup"] > 0 and "windup" in v:
                assert abs(v["windup"] - k["windup"]) <= 0.1, f"{mob}/{n}: windup {v['windup']} != karta {k['windup']}"
            k.update({x: v[x] for x in ("dl", "windup", "hit", "recover") if x in v})
            assert k["hit"] <= k["dl"] + 1e-6 or k["typ"] == "L", f"{mob}/{n}: hit > dl"
    return out

def md():
    L = ["# KONTRAKT STANÓW — Bagna (wiążący dla modeli i MM)",
         "Generowane z `MC/tools/bagna/kontrakt.py` (nie edytować ręcznie; zmiana = w kontrakt.py + `python3 kontrakt.py`).",
         "",
         "## Zasady wspólne",
         "- Plik modelu: `output/lochy/bagna/modele/<id>/bagna_<id>.bbmodel` → na serwerze `plugins/ModelEngine/blueprints/bagna/bagna_<id>.bbmodel`; **id modelu ME = `bagna_<id>`**.",
         "- Nazwy animacji DOKŁADNIE jak w tabeli (ASCII, małe litery). Literówka = MM cicho nic nie gra. Generator MM sprawdza każde `s=` z tą tabelą, a potem z .bbmodel.",
         "- `idle`, `walk`, `spawn`, `death` ME gra sam (config Default-Animations). Reszta tylko ze `state` z MM. death: kluczowa poza do 0,8 s, dł. ≤ 1,0 s (ME zdejmuje model ok. 1 s po śmierci; zmierzone 03.10).",
         "- Typ: **L** pętla (loop), **R** raz (once), **T** raz i trzymaj ostatnią klatkę (hold) — MM zdejmuje stan.",
         "- `windup` = zapowiedź z karty (wiążąca, ±0.1 s). `hit` = sekunda od startu animacji, w której MM zadaje obrażenia / wypuszcza pocisk — poza ciała w tej klatce MUSI pokazywać uderzenie. `recover` = odsłona po uderzeniu (okno na kontrę).",
         "- Kości obowiązkowe: `tag_name` (nad głową, nazwa), `hitbox` (prostopadłościan X==Z, wymiary z kolumny hitbox). `swiatlo_*` = turkus (MM daje brightness 15 po prefiksie). `p_*` = puste kości-emitery cząsteczek/dźwięku (pivot w miejscu efektu). `fx_*` = efekty ukryte skalą 0 poza swoją animacją.",
         "- Klatki w konwencji BB4/ME (rot −x,−y,z; poz −x,y,z) — `tools/modele/animacje.py`.",
         "- Czasy wolno nadpisać w `output/lochy/bagna/modele/<id>/czasy.json`: `{\"<anim>\": {\"dl\": s, \"windup\": s, \"hit\": s, \"recover\": s}}` — generator MM wczytuje; windup musi zgadzać się z kartą ±0.1 s.",
         "",
         "## Skrypt w klatce (ME Scriptable Keyframes) — STATUS: DZIAŁA (klon bagna-test 03.10: skill odpalił w wątku głównym, także bez graczy obok); dokładność czasu do zmierzenia na modelu Bagien",
         "ME 4.0.9 czyta z .bbmodel animator `effects` → kanał `timeline` → `data_points[0].script` (BlockbenchParser), format `<czytnik>:<treść>`;",
         "czytnik `mm` = `MythicScriptReader` → `castSkill(<mob>, <SKILL>)` (bez czytnika = `meg`: partvis/tint/enchant/tag/remap/changeparent).",
         "Wpis w .bbmodel (BB 4.x), w `animations[i].animators`:",
         "```json",
         "\"effects\": {\"name\": \"Effects\", \"type\": \"effect\", \"keyframes\": [",
         "  {\"channel\": \"timeline\", \"time\": <hit>, \"interpolation\": \"linear\", \"data_points\": [{\"script\": \"mm:BAGNA_<ID>_<ANIM>_KLATKA\"}]}]}",
         "```",
         "Modelarz WSTAWIA klatkę skryptu w czasie `hit` dla każdej animacji z kolumną „skrypt w klatce”. Dziś skill *_KLATKA gra tylko dźwięk/cząsteczki",
         "uderzenia (jeśli nie odpali — w teście słychać ciszę), obrażenia idą `delay`. Po teście na klonie (wątek synchroniczny, dokładność czasu) generator",
         "przełączy obrażenia na klatkę (`UDERZENIE=klatka`).",
         ""]
    for mob, d in MOBY.items():
        nazwy = [a["n"] for a in d["anim"]]
        brak = [x for x in OBOWIAZKOWE if x not in nazwy]
        assert not brak, (mob, brak)
        L += [f"## {mob} — {d['rola']} ({d['miejsce']}) · model `bagna_{mob}` · hitbox {d['hitbox'][0]}×{d['hitbox'][1]} (szer×wys) · atlas {d['atlas']}",
              "| animacja | typ | dł. s | zapowiedź s | uderzenie s | odsłona s | skrypt w klatce |", "|---|---|---|---|---|---|---|"]
        for a in d["anim"]:
            sk = f"`mm:{KLATKA(mob, a['n'])}` @ {a['hit']}" if ma_klatke(a) else "—"
            L.append(f"| `{a['n']}` | {a['typ']} | {a['dl']} | {a['windup'] or '—'} | {a['hit'] or '—'} | {a['recover'] or '—'} | {sk} |")
        L += [f"Kości specjalne: {', '.join('`'+k+'`' for k in d['kosci'])}", ""]
    return "\n".join(L) + "\n"

if __name__ == "__main__":
    out = "/opt/docker/mc/data/world1/output/lochy/bagna/_zespol/KONTRAKT_STANOW.md"
    open(out, "w").write(md())
    print("zapisano", out)
