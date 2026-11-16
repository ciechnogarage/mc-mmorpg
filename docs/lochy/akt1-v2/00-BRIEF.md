# Akt 1 — 10 lochów od nowa: BRIEF zespołu (2026-10-03)

Zlecenie właściciela (dosłownie): „stwórzcie 10 pełnych, różnorodnych, pasujących do klimatu lochów. Mają się różnić,
mieć sens, być angażujące, wymagające, interesujące, piękne, zapierające dech. Pilnujcie się, nie pozwalajcie na łatwiznę.
Dyskutujcie, blokujcie się, pytajcie, sprawdzajcie, poprawiajcie.” Do sprawdzenia: czy dotychczasowe 001–010
było przemyślane do 100 i czy się nie gryzie.

Zespół: **Prowadzący (Claude)** — brief, synteza, bramka, loch 010, spójność. **Projektant C (agent Claude)**,
**Projektant X (Codex 1)**, **Projektant Y (Codex 2)**. Każdy może ZABLOKOWAĆ cudzą pracę — blokada musi mieć
konkretny powód i propozycję naprawy. Blokada bez propozycji jest nieważna. Pochwała bez dowodu też.

Wszystko po polsku. Pliki tylko w `docs/lochy/akt1-v2/`. Nie dotykać serwera, configów, pluginów.

## 1. Kanon, który obowiązuje (nie wolno sprzeczności)

- **Gra**: MMORPG na Minecraft (Paper 1.21.x). 4 rasy: Człowiek, Elf (Przymierze Latarni i Księżyca), Ork, Nieumarły
  (Pakt Krwi i Kości). Orkowie i nieumarli są GRYWALNI — nie mogą być „hordą zła” Aktu 1. 4 archetypy:
  Wojownik (zasób Szał), Ninja (Skupienie + Znamiona), Mag (mana), Akolita (Dusze; summoner/front/wsparcie) — 16 klas
  rasowych, specjalizacja na poz. 10. Gracz ma: LPM lekka seria, PPM ładowany mocny cios, Shift+PPM blok, zaklęcia
  w rękach (Księga), Chowańca (pet walczący). Brak uniku jako przycisku — kontry to ruch, pozycja, blok, przerwanie.
- **Panteon** (`docs/wiary/panteon.md`): Trzej Starsi Ordhun (Pierwszy Korzeń: ziemia, rośliny), Nysara (Tkaczka Losu),
  Vharun (Ten, Który Milczy: śmierć, sąd); bóstwa ras. Solenna (Pani Latarni) i Aelyra (Księżyc) to siostry.
  Decyzja 29.09: **Akt 1 bez wiązania z wiarami** — można aluzją (kapliczka, legenda), nie fabułą obowiązkową.
- **Świat**: Stolica Wyspy (hub, Nexus z wejściem do lochów). Każdy loch = osobna instancja MythicDungeons
  (wyspa w pustce lub zamknięta przestrzeń), wejście z menu/NPC — gracz nigdy nie wpisuje komendy.
- **Akt 1 = poziomy 1–10** (decyzja D5). 10 lochów → mniej więcej loch N na poziom N (otwarcie od N−1).
  Spec postaci dopiero na 10 → lochy 001–009 nie mogą zakładać roli tank/heal; muszą być do przejścia SOLO
  (drużyna ma łatwiej, ale nie wymagana) każdym z 4 archetypów.
- **Dwie ścieżki** (korekta właściciela 30.09): lochy i wyspa świata otwartego Aktu 1 to RÓŻNE treści o równej
  wartości progresu. Wspólne: kraina, klimat, rodzaj zagrożenia, rodziny wrogów (krewniacy), rodzaje materiałów.
  Inne: konkretni wrogowie, bossowie, historia, forma. NIE projektujemy par loch↔strefa 1:1.
- **Istniejąca treść**: Loch 001 Kwietna Polana (`../loch-001-kwietna-polana.md`, na world1, ocena właściciela
  2/10 po v7: „puste przestrzenie, moby w paczkach bez przemyślenia, za proste mechanicznie, logicznie
  i umiejętnościowo”) i Loch 002 Bagna Zgniłych Korzeni (`../loch-002-bagna-zgnilych-korzeni.md`, v0.2 zbudowany na
  klonie, niewdrożony; motyw śluz i Topielicy). Wolno je zachować, przerobić albo zastąpić — z uzasadnieniem.
  Gotowe przeciwniczki z areny: GAJ_WILK, GAJ_KORZENIAK, GAJ_LUCZNIK (przetestowane, zapowiedź → zamknięty cel).
- Metoda (przyjęta 30.09, `/opt/docker/mc/forge/tools/przeciwnicy/README.md`): każdy przeciwnik przez arenę
  (bot nieuważny ma tracić dużo, uważny prawie nic), beat chart przed budową, CIASNY blockout.

## 2. Co już podejrzewamy w starym planie (sprawdźcie, potwierdźcie albo obalcie)

Plan: `../plan-100-lochow-10-wysp.md` (siatka 10 aktów × 10 kolumn K1–K10).
1. Drabina Aktu 1 jest **próbnikiem całej gry**: 003 zatopione ruiny ≈ Akt 2 (morze), 004 kryształowe jaskinie ≈ Akt 4
   (Głębia), 007 Mroźna Przełęcz ≈ Akt 5 (Mroźna Korona), 008 Popielne Sanktuarium ≈ Akt 8 (Popielna Korona),
   009 Pustkowia Kości z Anubisem/mumiami/obeliskami ≈ Akt 3 (Złote Piaski). „Zielona Granica” traci tożsamość,
   późniejsze akty tracą świeżość.
2. Stałe kolumny K1–K10 powtórzone 10× (każdy akt ma „loch ognia” K8, nawet Mroźna Korona) = formuła, nie świat.
   100 lochów ze schematem „kolumna = lekcja” grozi powtarzalnością, którą krytykowano u Hypixela/Wynna.
3. „Prawo” Aktu 1 to „podstawy” — żadne prawo. Inne akty mają mechanikę świata (przypływ, żar, ciemność…).
4. Brak spoiwa fabularnego aktu: czemu te 10 miejsc, co je łączy, kto/co jest zagrożeniem aktu, co się zmienia
   w świecie po każdym lochu.
5. Plan v0.1 jest oparty na odrzuconych parach loch↔strefa (korekta 30.09).

## 3. Czym jest „pełny loch” (każdy projekt MUSI to mieć)

1. **Tożsamość**: nazwa, poziom, czas (Normalny solo 12–20 min), jedno zdanie fantazji gracza, emocja przewodnia.
2. **Mechanika lochu** (Zelda/kishōtenketsu): JEDEN motyw, który loch uczy: wstęp bez ryzyka → rozwinięcie →
   ZWROT (mechanika odwrócona/odebrana/użyta przeciw graczowi) → boss = egzamin z motywu. Trash uczy mechanik bossa.
   Mechanika nie może się powtarzać między lochami.
3. **Historia środowiskiem**: co tu się stało, kto tu był, co gracz zmienia. Bez ścian tekstu; NPC max 2–3 kwestie.
   Związek z wątkiem całego aktu.
4. **Układ (topologia)**: szkic mapy w ASCII + opis; 6–9 sal/stref 20–40 bloków, zero pustych łąk; pętla i skrót
   z powrotem do ogniska; min. jeden wybór „co, nie jak” (2 cele w dowolnej kolejności albo rozwidlenie);
   brama bossa widoczna wcześnie (weenie); pionowość tam, gdzie ma sens. Topologia ma się RÓŻNIĆ od pozostałych
   lochów (liniowa nad pustką, hub z ramionami, pionowa wieża, pętla, warstwy, ruchoma mapa…).
5. **Beat chart**: tabela sala po sali — rola (nauka/test/odpoczynek/wow/wybór), czas, skład wrogów z
   uzasadnieniem, jak zaczyna się starcie (gracz zaskakuje / wróg zaskakuje / widok + wejście bez powrotu),
   tempo (wysokie/niskie na przemian), co poza walką.
6. **Wrogowie**: 4–6 typów + elita, każdy z: rolą, zapowiedzią (dźwięk/animacja/cząsteczki ≥ 0,5 s), atakiem karzącym
   za ignorowanie, kontrą dostępną KAŻDEMU archetypowi, różnicą nieuważny/uważny gracz. Paleta w starciu: 1 typ =
   lekcja, 2 = norma, 3 = trudne. Krewniacy rodzin z innych lochów aktu dozwoleni, kopie nie.
7. **Boss**: funkcja → haczyk (1 zdanie, zmienia walkę) → historia → wygląd (sylwetka, punkt słaby = najjaśniejszy).
   2–3 fazy, 4–9 zdolności z zapowiedzią, arena z elementem, który się zmienia, mechanika anty-kitowania,
   jak boss sprawdza motyw lochu. Kandydat modelu (paczka LittleRoom — lista w `../akt1-wyspa-i-lochy.md` §2;
   nazwy modeli tylko jako kandydaci) albo „model własny”.
8. **Moment WOW** (jeden, drogi): widok, przebudzenie, zmiana świata. Opisany konkretnie: co gracz WIDZI, z jakiego
   miejsca, w której minucie. Plus paleta barw i bloków lochu (5–8 bloków dominujących, światło, pora, pogoda,
   dźwięk) — tak, żeby screenshot z każdego lochu był rozpoznawalny bez podpisu.
9. **Poza walką**: sekrety (3–5, każdy ze wskazówką w świecie), zagadka/zależność, wybór z konsekwencją.
10. **Powtarzalność**: co losuje się przy wejściu i jak zmienia TAKTYKĘ (nie kolor); ocena przejścia.
11. **Nagroda**: EXP postaci, łup (rodzina przedmiotów lochu, materiał profesji, tylko-tu), nagroda za sekrety,
    dla całej drużyny. Co loch odblokowuje / zmienia (skrót w świecie, wątek do kolejnego lochu).
12. **Wykonalność na naszym stosie**: MythicMobs 5.11.2 (darmowy — bez warunków na celu premium), MythicDungeons
    (funkcje, triggery, BlockEditor, checkpointy), ModelEngine 4, teren pieczony offline i zamrożony (Iris tylko
    w kuźni), budowle `place template`, Nexo (własne bloki/przedmioty), MythicHUD, dialogi Papera. Wskazać 1–3
    najtrudniejsze technicznie elementy i plan B dla każdego. Nic, czego vanilla/stos nie umie (np. własne
    klawisze, przechwycenie E/Tab — niemożliwe).
13. **Trudność**: co zmienia Łatwy/Normalny/Trudny (mechanika, nie tylko HP).

## 4. Bramka jakości (każdy recenzent ocenia punkt po punkcie, 0–10)

Fun walki · Mechanika lochu i zwrot · Czytelność · Topologia/nieliniowość · Piękno i WOW · Sens fabularny ·
Powtarzalność · Nagroda/progres · Wykonalność · Różnorodność względem pozostałych 9.
**Poniżej 7 w którymkolwiek = BLOKUJĘ.** Plus lista „suche i bez sensu”: wskazówki prawdziwe (MC: +Z = południe),
max 1 komunikat naraz w walce, tekst nie obiecuje czegoś, czego nie ma, nagroda warta wysiłku, gracz wie co robić,
walka karze za ignorowanie mechaniki, nic nie da się bezkarnie skitować.

Łatwizny do zablokowania od ręki: „boss + 3 fale mobów”, kopia mechaniki innego lochu z nowym kolorem, „zabij X”
jako cała treść, sekrety = skrzynka w rogu bez wskazówki, losowość tylko kosmetyczna, puste przestrzenie, moby
w paczkach bez powodu, WOW opisane przymiotnikami zamiast obrazu, „technicznie zrobimy później”.
