# Klasy rasowe — rozpiska 16 klas (ZAAKCEPTOWANA i WDROŻONA 2026-09-26)

> **HISTORIA (dopisek 2026-10-03).** Nazwy, role i skille w tej rozpisce są z 26.09. Etap 0 (29.09) zmienił nazwy
> (np. Rycerz -> Miecz Korony, Bastion -> Nieugięty), etap 2-3 dał własne skille wszystkim archetypom, a archetypy mają
> zasoby (Szał, Skupienie+Znamiona, mana, Dusze). Aktualny stan = `tools/klasy-rasowe/generuj_klasy.py` (tabela KLASY),
> skille: `skille-specjalizacji-2026-10-02.md`, `burza-sygnatury-2026-10-02.md`; analiza: `analiza-spojnosci-2026-10-03.md`.

Wzór: prototyp Ork Wojownik (MMOCore/classes/ork/). Specjalizacja na 10 poz. różni się:
rolą, dodatkową biegłością (zbroja albo broń) i statystyką-podpisem (Tyran: obrona, Krwioburca: kryt).
Umiejętności na razie ze szkieletu archetypu (warrior/rogue/mage/paladin) — własne skille klas osobnym etapem.

## Zasady wspólne
Zbroja archetypu: Wojownik L+Ś, Ninja L, Mag tkanina, Akolita L. Spec frontowa: +1 stopień.
Broń: NOWE ukryte atrybuty biegłości broni (jak zbroja), broń MMOItems `required-bieglosc-<grupa>: 1`
zamiast `required-class` (to się nie skaluje na 48 klas).
Broń archetypu: Wojownik miecz (SWORD, LONG_SWORD); Ninja sztylet + łuk (DAGGER, BOW);
Mag różdżka + kostur (WAND, STAFF); Akolita buława (HAMMER). Tarcza (SHIELD) = biegłość spec obronnych (Bastion, Strażnik Cierni, Tyran, Strażnik Grobu) + rasowa Świętego Akolity.
Dopisane przy wdrożeniu (typy bez właściciela): THRUSTING_SWORD -> miecze, GAUNTLET -> sztylety,
STAVE -> kostury, MUSKET -> kusze (Agent). Wolne (przedmioty-statystyki z ręki): LUTE, CATALYST, MAIN/OFF_CATALYST, ORNAMENT.
Rasa (małe delty na bazie archetypu):
- Człowiek: zrównoważony, +5% redukcji odnowienia
- Elf: +10% prędkości ruchu, +regeneracja many, −10% max HP
- Ork: +15% max HP, +10% obrażeń ataku, −20% max many
- Nieumarły: 3% kradzieży życia (magowie/akolici: wampiryzm czarów), −5% prędkości ruchu
  (zamiast "−50% regeneracji HP": regeneracja z jedzenia jest vanillowa, statystyką się jej nie zetnie)

## Tabela
| Klasa | Zbroja | Broń: archetyp + rasowa | Spec A | Spec B |
|---|---|---|---|---|
| Człowiek Wojownik — Rycerz | L Ś | miecz + drzewcowa (HALBERD, LANCE) | Bastion: tank, +C +tarcza, obrona | Czempion: pojedynki, siła ataku |
| Człowiek Ninja — Agent | L | sztylet, łuk + kusza (CROSSBOW) | Widmo: skrytobójca wręcz, kryt | Jastrząb: strzelec kuszy, obr. pocisków |
| Człowiek Mag — Mag Akademii | tk. | różdżka, kostur + grimuar (TOME) | Elementalista: obrażenia czarów | Przywoływacz: przywołańce, redukcja odnowienia |
| Człowiek Akolita — Święty Akolita | L Ś (rasowe) | buława + tarcza (SHIELD) | Serafin: front, +C, obrona | Hierofant: leczenie/wsparcie, mana |
| Elf Wojownik — Ostrze Gaju | L Ś | miecz + elfia klinga (KATANA) | Strażnik Cierni: tank-cierń, +C, odbicie/obrona | Tancerz Ostrzy: szybkość ataku, unik |
| Elf Ninja — Strażnik Kniei | L | sztylet, łuk + łuk długi (GREATBOW) | Cień Liści: wręcz z ukrycia, kryt | Sokół: snajper, obr. pocisków |
| Elf Mag — Tkacz Arkanów | tk. | różdżka, kostur + kostur dwuręczny (GREATSTAFF) | Tkacz Żywiołów: obrażenia czarów | Strażnik Żywiołów: bariery, +L, obrona magiczna |
| Elf Akolita — Wyrocznia | L | buława + sierp księżycowy (NOWY typ SIERP) | Ostrze Księżyca: wręcz, +Ś, kryt | Opiekun Duchów: leczenie przez duchy, regen many |
| Ork Wojownik — Wódz Wojny ✔ | L Ś | miecz + topory (AXE, GREATAXE) | Tyran ✔: tank, +C, obrona | Krwioburca ✔: szał, kryt |
| Ork Ninja — Tropiciel | L Ś (rasowe) | sztylet, łuk + włócznia/oszczep (SPEAR) | Łowca Bestii: pupil-bestia, HP | Łowca Kości: dystans, obr. pocisków |
| Ork Mag — Szaman | L (rasowe) | różdżka, kostur + totem (TALISMAN) | Burzownik: pioruny, obrażenia czarów | Totemista: totemy/wsparcie, redukcja odnowienia |
| Ork Akolita — Kapłan Krwi | L | buława + młot dwuręczny (GREATHAMMER) | Awatar Wojny: front, +Ś, siła ataku | Szept Przodków: wsparcie/klątwy, mana |
| Nieumarły Wojownik — Rycerz Śmierci | L Ś | miecz + miecz dwuręczny (GREATSWORD) | Strażnik Grobu: tank, +C +tarcza, obrona | Żniwiarz: kradzież życia, obszar |
| Nieumarły Ninja — Widmo Grobu | L | sztylet, łuk + łańcuch (WHIP) | Cień: skrytobójca, kryt | Strzelec Grobu: dystans, obr. pocisków |
| Nieumarły Mag — Mag Grobu | tk. | różdżka, kostur + filakterium (NOWY typ FILAKTERIUM) | Duszożerca: wampiryzm czarów, obrażenia | Nekromanta: przywołańce umarłych, mana |
| Nieumarły Akolita — Spaczony Akolita | L | buława + kosa (NOWY typ KOSA) | Awatar Otchłani: front, +Ś, kradzież życia | Pasterz Umarłych: przywołańce/wsparcie, regen many |

## Decyzje (2026-09-26)
1. Nowe typy SIERP i KOSA — TAK. Dodatkowo FILAKTERIUM: CATALYST to 12 ogólnych przedmiotów-statystyk
   dla każdego, zablokowanie całego typu dla Maga Grobu odebrałoby je innym.
2. Biegłości broni jako ukryte atrybuty — TAK (21 atrybutów, 87 przedmiotów z wymaganiem).
   required-class zdjęty ze WSZYSTKICH przedmiotów (także zestawów TEST_): wskazywał klasy archetypów,
   których po zmianie nikt nie ma.
3. Rasowe dodatki zbroi — bez zmian (Święty Akolita Ś, Tropiciel Ś, Szaman L).

## Wdrożenie
Źródło prawdy: `tools/klasy-rasowe/generuj_klasy.py` (klasy, atrybuty, okno specjalizacji, mapowanie CoreTools,
`oczekiwane.json` dla evali) + `latacz_broni.py` (typy i broń MMOItems). Zmiana klasy = edycja tabeli + oba skrypty
+ `mmocore reload` / `mi reload` / `ct reload`; NOWY atrybut = restart. Kopia sprzed: `backups/klasy-rasowe-16-20260926/`.

Zmierzone przy wdrożeniu:
- klucz statystyki w pliku klasy ZASTĘPUJE domyślną MMOCore (mana 20, regen many 0.166, regen HP 0.1), nie dodaje się
  (sonda-domyslne.js: Tropiciel miał -4 many) — generator startuje brakujące klucze od DOMYSLNE,
- biegłość broni działa jak zbroja: brak = cios 0 obrażeń + "Nie możesz tego użyć — brakuje: Biegłość: <grupa>."
- materiał CHAIN w tej wersji to IRON_CHAIN (inaczej MMOCore nie wczytuje klasy).

Evale (`tools/klasy-rasowe/run.sh`, SCEN=...): mapowanie-16 (34 PASS), konfiguracja-48 (86), bron-bieglosci (5),
specjalizacja-okno (7), ork-wojownik (20), relog-klasa (6), dwie-rasy (4), stare-klasy-ukryte (3).
zestawy-klas przeniesiony do `wycofane/`.
Ekwipunek (2026-09-26, drugi przebieg): eq-sloty (27 PASS: 4 sloty zbroi × waga, druga ręka tarcza/grimuar,
łuk, 9 slotów akcesoriów) i bron-wszystkie (44 PASS: każda z 21 grup broni z biegłością i bez, blok tarczą).
Zmierzone: tarcza bez biegłości NIE blokuje ciosu (sam blok, nie tylko pancerz); katalizatory (grimuar, totem,
filakterium) bez biegłości da się trzymać, ale nie dają statystyk; akcesoria nie mają blokad klas.
Dodane przedmioty testowe: TEST_ZBROJA_{LEKKA,SREDNIA,CIEZKA}_{HELM,NOGI,BUTY}, TEST_RAPIER.

## Domknięcie dziur (2026-09-26, noc) — `tools/klasy-rasowe/tresc_startowa.py`
1. Vanillowa zbroja/broń omijała biegłości (MMOItems nie zna vanilli): zamiennik CoreTools (`item-replacer.yml`,
   pickup/klik/skrzynia/craft/kowadło/wejście + skan co 2 min) zamienia ją w nasz odpowiednik z wagą/grupą;
   vanillowe receptury zbroi i broni wyłączone (`disable-vanilla-recipes`, 35 materiałów). Siekiery zostają
   vanillowe (narzędzie do drewna).
2. Zbroje z wagą: 7 zestawów × 4 (lniana szata = tkanina, skórzana = lekka, kolczuga i złocona = średnia,
   żelazna, diamentowa, netherytowa = ciężka) + żółwi hełm; 43 zbroje-przykłady dostały wagę wg materiału
   (głowy i skrzydła bez wagi — kosmetyka).
3. Broń startowa dla każdej z 21 grup (poziom 1) + miecze-odpowiedniki vanilli; łuk długi istnieje.
4. Opis przedmiotu pokazuje "Wymaga biegłości: <grupa>" (lore-format.yml + stats.yml).
5. Zestaw startowy klasy: przy nadaniu klasy rasowej broń grup klasy (+32 strzały dla łuczników) i zbroja
   najcięższej wagi klasy (tkanina/lekka/średnia) ZAŁOŻONA w puste sloty; zestaw CMI "Newbie" (kamień z
   Cierniami V) wyłączony.
6. Stare postacie z klasą archetypu (np. 11 postaci właściciela): kreator wybiera im tę samą klasę i MMOCore
   nie woła zmiany klasy — mapowanie dopięte do PlayerDataLoadedEvent, dostają klasę rasową i zestaw przy wejściu.
Eval: `baza-dziury.js`. NIEZMIERZONE botem: blokada receptury (mineflayer nie otwiera stołu na tym serwerze).

## Otwarte (nie w tym etapie)
- zmiana specjalizacji przez gracza (projekt: "jakoś") — dziś tylko konsola `mmocore class force`,
- modele/tekstury Nexo dla nowych przedmiotów (dziś vanillowe materiały),
- przedmioty TEST_* do usunięcia przed grą,
- umiejętności: wszystkie klasy mają skille szkieletu archetypu — własne skille klas osobnym etapem,
- broń startowa nowych typów ma vanillowe materiały (IRON_HOE, NETHERITE_HOE, ECHO_SHARD) — modele Nexo później,
- spawn po kreatorze (149.5/98/197.5) to kamienna kieszeń na zboczu, gracz patrzy w ścianę (yaw -90) —
  czeka na decyzję właściciela: wyrównać teren 7×7 albo przenieść wyjście (3 configi, patrz notatka spawnu).

## Poprawki po teście właściciela (2026-09-26, popołudnie)
1. **4 klasy i 4 zestawy naraz** (konto z OP): `%luckperms_check_permission_race.X%` pyta hasPermission,
   więc OP "miał" każdą rasę (nawet `race.xyz`) — Człowiek Mag przeszedł Mag Akademii → Tkacz Arkanów →
   Szaman → Mag Grobu i dostał 4 zestawy. Generator używa teraz `%luckperms_has_permission_race.X%`
   (sam wpis LuckPerms). Evale bez OP tego nie widziały — nowy eval `op-postac` (OP=1 domyślnie).
2. **Kreator pokazuje klasę rasową**: plugin od `55d88f5` czyta `classes.<archetyp>.races.<rasa>`
   (display/description/bonus); sekcje pisze `generuj_klasy.py` (funkcja `kreator`). Po wyborze Orka kafelek
   mówi "Tropiciel", opis, zbroję, broń i "Na 10 poz.: Łowca Bestii albo Łowca Kości". Źródła pluginu:
   gałąź `cc-klasy-rasowe` w repo MC (pierwszy commit = stan wdrożonego jara z kopii roboczej).
3. **"Postać chodzi tylko skacząc"** — to NIE blokada ruchu: wyjście z kreatora stoi w kamiennej kieszeni
   na zboczu (ściany z trzech stron, gracz patrzy w stopień). Zmierzone prawdziwym klientem QA.
   Pułapka pomiaru: przy włączonym `skins.apply-to-player` serwer w ogóle nie przyjmuje ruchu bota
   mineflayer (pozycja serwera stoi, zero korekt), a prawdziwy klient chodzi. Przyczyna niepotwierdzona
   (hipoteza: sztuczny respawn z setPlayerProfile, na który bot nie odpowiada). Ruchu nie mierzyć botem.

## Prędkość 0 u elfiego i nieumarłego maga (2026-09-27)
Nowy gracz (Nieumarły Mag = Mag Grobu) stał w miejscu z przybliżonym obrazem: `movement-speed` 0.
Szkielet `mage.yml` nie ma klucza, a rasowe ±% ruchu mnożyło zero (klucz ZASTĘPUJE domyślne 0.1).
Dotyczyło 6 klas: Tkacz Arkanów, Tkacz/Strażnik Żywiołów, Mag Grobu, Duszożerca, Nekromanta.
Poprawka: `DOMYSLNE['movement-speed'] = 0.1` + `bramka_statystyk` w generatorze (granice niezależne
od tabel, bo eval konfiguracja-48 porównuje z tym samym generatorem i przepuścił zero).
