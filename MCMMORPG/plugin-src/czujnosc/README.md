# Czujnosc (skradanie mobow)

Wtyczka Paper: moby z configu widza (stozek, swiatlo, postawa, niewidzialnosc), slysza (ruch, zbroja, ciosy, pociski,
kopanie) i przechodza SPOKOJNY -> ZANIEPOKOJONY (?) -> SZUKA (?) -> ALARM (!). Zastepuje paczke Little Room `alertsys`
(skille MM co 0,5 s na mobie), z poprawionymi bledami: brak alarmu od tylu przy kucaniu, wartosc spada bez bodzca,
cios wrecz = alarm, ikona jako pasazer (nie `mountmodel mid=<caster.name>`).

- Stan moba: PDC `czujnosc:stan` (bajt 0..3, 0 = spokojny; brak = mob bez Czujnosci) - czyta AnimacjeBroni `ukryty()` (cios z ukrycia: nieswiadomy mob z Czujnoscia = x3 z kazdej strony).
- MM: sygnaly `CZUJ_<POZIOM>` -> animacje/dzwieki moba w jego skillach (`~onSignal:CZUJ_ALARM`).
- Koszt: `/czujnosc stan` (sredni czas na tik: zmysly graczy i stany mobow osobno, pary gracz-mob, promienie od `stan zeruj`;
  obok MSPT serwera z Paper `getTickTimes()` - `spark tps` przez RCON odpowiada z opoznieniem). Pomiar 22.10 (60 mobow, 4 boty,
  klon 2 CPU): ~0,7-1 ms/tik przy 60-80 ms calego tiku; LR alertsys tyle samo lub gorzej (`narzedzia/bot/scenariusze/skradanie/koszt.js`).
- Ikona: TextDisplay `ikony.skala` (2.5), wysokosc nad hitboxem `ikony.wysokosc` lub per typ MM `ikony.wysokosc_typ` (model ME bywa wyzszy).
- Testy: `narzedzia/bot/scenariusze/skradanie/czujnosc.js` (24 przypadki: wzrok, kucanie, noc, tyl, cios z ukrycia, odwrocenie uwagi, wolanie).
- Build: `./build.sh` (kontener gradle, jary world1 tylko do odczytu), jar w `build/libs/`.
