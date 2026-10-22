"""Dzwieki Bagien: wybor .ogg z paczki LittleRoom (po nazwach i uzyciu w mobach LR - odsluchu nie bylo) -> staging
output/lochy/bagna/dzwieki/{pack/assets/bagna/sounds/<mob>/*.ogg, sounds.yml.bagna, sounds.json.zapas}.
Zdarzenie = bagna:<mob>.<nazwa>; plik = bagna:<mob>/<plik> (bez .ogg w sounds.json, z .ogg w Nexo sounds.yml).
Uzycie w MM: sound{s=bagna:<mob>.<nazwa>;v=..;p=..;sc=HOSTILE}. NIE kopiuje do plugins (wdrozenie osobno)."""
import json, os, shutil, sys
LR = "/opt/docker/mc/modele/OUTPUT — kopia (2)/resource pack/assets/minecraft/sounds/littleroom"
OUT = "/opt/docker/mc/data/world1/output/lochy/bagna/dzwieki"
# zdarzenie: (zrodlo LR, [pliki]) - kilka plikow = losowy wariant
ZD = {
 "pijawka.bable":        ("merloc",   ["bubbles_1", "bubbles_2", "bubbles_3"]),       # zapowiedz wyskoku (warstwa)
 "pijawka.klap":         ("merloc",   ["pfish_pop_1", "pfish_pop_2", "pfish_pop_3"]), # klapniecie przyssawki
 "mulowiec.zamach":      ("dreadroot", ["swing_1"]),
 "mulowiec.trafienie":   ("dreadroot", ["mudmass_hit"]),
 "mulowiec.jek":         ("dreadroot", ["groan"]),                                   # zapowiedz rzutu (warstwa)
 "mulowiec.krok":        ("dreadroot", ["ground_1", "ground_2", "ground_3", "ground_4"]),
 "kokoniara.kokon":      ("voras",    ["voras_webs"]),
 "kokoniara.wyleg":      ("voras",    ["eggsac_break"]),
 "kokoniara.ladowanie":  ("voras",    ["leg_impact1", "leg_impact2", "leg_impact3"]),
 "kokoniara.krzyk":      ("voras",    ["voras_scream1", "voras_scream2"]),
 "kokoniara.smierc":     ("voras",    ["voras_death_scream"]),
 "rechotnik.jezyk":      ("bufobufo", ["tongue01", "tongue02", "tongue03"]),
 "rechotnik.trafienie":  ("bufobufo", ["tongue_impact01"]),
 "rechotnik.rechot":     ("bufobufo", ["pop01", "pop02", "pop03"]),
 "rechotnik.wynurzenie": ("bufobufo", ["splash01", "splash02", "splash03"]),
 "rechotnik.smierc":     ("bufobufo", ["death"]),
 "krol_ropuch.ladowanie":("bufobufo", ["ground_impact01", "ground_impact02"]),
 "krol_ropuch.wyplucie": ("bufobufo", ["poof"]),
 "krol_ropuch.bulgot":   ("bufobufo", ["bubbles01", "bubbles02", "bubbles03"]),
 "krol_ropuch.klapniecie":("kraken",  ["bite"]),
 "krol_ropuch.ugryzienie":("kraken",  ["bite_hit"]),
}
ZDARZENIA = {f"bagna:{k}" for k in ZD}
# 03.10: Mokradla odlozone - do packa tylko dzwieki aktywnych mobow (wpisy zostaja w ZD na pozniej)
ODLOZONE = ("rechotnik.", "krol_ropuch.")

def main():
    pack = f"{OUT}/pack/assets/bagna/sounds"
    shutil.rmtree(OUT, ignore_errors=True); os.makedirs(pack)
    sj, yml, bajty = {}, ["# dopisac do plugins/Nexo/sounds.yml pod 'sounds:' (zastapic 'sounds: []'); potem /nexo reload pack",
                          "# DO SPRAWDZENIA NA KLONIE: czy Nexo przyjmuje namespace inny niz nexo; inaczej uzyc sounds.json.zapas", "sounds:"], 0
    for zd, (src, pliki) in ZD.items():
        if zd.startswith(ODLOZONE): continue
        mob = zd.split(".")[0]
        os.makedirs(f"{pack}/{mob}", exist_ok=True)
        refs = []
        for f in pliki:
            s = f"{LR}/{src}/{f}.ogg"
            assert os.path.exists(s), s
            shutil.copy(s, f"{pack}/{mob}/{f}.ogg"); bajty += os.path.getsize(s)
            refs.append(f"bagna:{mob}/{f}")
        sj[zd] = {"sounds": refs, "subtitle": f"subtitles.bagna.{zd}"}
        yml += [f"  - id: bagna:{zd}", "    sounds:"] + [f"      - {r}.ogg" for r in refs]
    open(f"{OUT}/sounds.yml.bagna", "w").write("\n".join(yml) + "\n")
    json.dump(sj, open(f"{OUT}/sounds.json.zapas", "w"), indent=1, ensure_ascii=False)
    print(f"zdarzen {len(ZD)}, plikow {sum(len(p) for _, p in ZD.values())}, {bajty/1e6:.2f} MB -> {OUT}")

if __name__ == "__main__":
    main()
