# Profesje v2 — projekt (2026-10-12)

Źródło prawdy liczb: `tresc.py` (ten katalog). Ten dokument opisuje DECYZJE i wzory; tabele generuje `katalog.py`
do `KATALOG.md`. Każda reguła poniżej ma bramkę w `audyt.py` — zmiana treści, która ją łamie, nie przechodzi.

Punkt wyjścia: audyt 12.10 (`profesje-audyt-2026-10-12` w pamięci). Wady i odpowiedź:

| Wada v1 | Odpowiedź v2 | Bramka audytu |
|---|---|---|
| Alchemia bez mikstur, kowal/krawiec bez sprzętu, budownictwo do skupu | macierz celów: każdy produkt ma ujście (walka, sprzęt, narzędzia, Chowańce, świat) | `ujscie` |
| 10 narzędzi bez funkcji | plugin Rzemiosło: narzędzie wymagane do węzła, tier = dostęp, szybkość, urobek; zużycie i naprawa | `narzedzia` |
| rzadkie z pospolitego (8 połowów → małż) | brak receptur „surowiec → surowiec tej samej profesji"; rzadkie tylko z węzłów (szansa) i mobów | `przemiany` |
| 1254 oleje na 15 poziomów | krzywa liczona w AKCJACH: poziom L kosztuje 20+2L akcji bieżącego tieru; XP rzemiosła = surowcowy ekwiwalent | `mielenie` |
| progi 1/15/30/45, 46–50 puste | 10 tierów po 10 poziomów, coś nowego co ≤3 poziomy, do 100 | `luki` |
| pas +10 HP vs łup +1,9 HP | sprzęt z rzemiosła = bazy v2, rzadkość Magiczna + 1 gniazdo, staty liczone wzorem v2 | `sila` |
| potrawa 180 s / cd 20 s = stały buff | buffy przez plugin (grupy: posiłek / eliksir / olej, jeden aktywny w grupie), czas < sens, mikstury z cooldownem współdzielonym | `buffy` |
| XP z vanilli (statyw, stół, kowadło) | usunięte: `alchemy-experience`, `base-enchant-exp`, `repair-exp`, `enchantitem{}`, `repairitem{}` | `vanilla` |
| demo-stacje MMOItems zjadają PROF_* | `mythical-forge`, `arcane-forge`, `steel-crafting-station` wyłączone (kopia w backups) | `demo` |
| poziom profesji nic nie daje | perki co 10 poziomów (zbieranie: urobek/szybkość; rzemiosło: oszczędność/podwójny wynik) | `perki` |
| maks 50, brak mistrzostw | 100 poziomów; plugin Mistrzostwa (gotowy od 23.09): do 50 wszystko, 50→100 tylko 2 mistrzostwa, gotowanie i budownictwo ogólne | `mistrzostwa` |
| brak zadań profesji | tablica zleceń (3 dzienne na profesję) + zlecenie startowe dające narzędzie T1 | `zlecenia` |

## 1. Profesje i role

| Id (bez zmian) | Nazwa | Rodzaj | Rola (co z niej ma gracz i rynek) |
|---|---|---|---|
| mining | Górnictwo | zbieractwo | rudy i kryształy → kowal, jubiler, zaklinacz, budowniczy |
| woodworking | Drwalstwo i stolarstwo | zbieractwo + rzemiosło | drewno; deski, trzonki, siekiery, wędki, broń drewniana (łuki, kostury) |
| herbalism | Zielarstwo | zbieractwo | zioła → alchemik, kucharz (przyprawy), Chowańce |
| farming | Farmerstwo | zbieractwo | plony i włókna → kucharz, krawiec, Chowańce |
| fishing | Rybactwo | zbieractwo | ryby i dary wody → kucharz, alchemik, jubiler (perły) |
| hunting | Myślistwo | zbieractwo | skóry, kości, trofea (oprawianie zwłok) → krawiec, kowal, alchemik |
| smithing | Kowalstwo | rzemiosło | sztabki, narzędzia metalowe, broń i zbroja metalowa (v2), osełka, zestawy naprawcze |
| tailoring | Krawiectwo | rzemiosło | tkanina, skóra wyprawiona, cięciwa, zbroja tkana i lekka (v2), łata |
| jewelcrafting | Jubilerstwo | rzemiosło | oszlifowane kamienie, ozdoby (v2), klejnoty do gniazd (obrona i użyteczność), pył jubilerski |
| alchemy | Alchemia | rzemiosło | ekstrakty, mikstury (leczenie, mana, odtrutka), eliksiry, oleje do broni |
| enchanting | Zaklinanie | rzemiosło | pył zaklęć, runy do gniazd (atak), zwój wyjęcia gniazda, pieczęć przebudzenia (Chowańce) |
| cooking | Gotowanie | ogólna | posiłki przed wyprawą (grupa „posiłek"), uczty drużynowe |
| building | Budownictwo | ogólna | obozowisko (strefa odpoczynku), zestawy przepraw — klucze do skrótów w lochach i na wyspach |
| poskramiacz | Poskramiacz | Chowańce | bez zmian (osobny system, ma perki i 29 receptur); id materiałów, których używa, zostają |

Profesje zbieractwa NIE mają warsztatów (v1 miała w nich przemiany). Ich kafel w menu pokazuje tier, wymagane
narzędzie i gdzie szukać węzłów. Woodworking ma warsztat, bo jest hybrydą.

## 2. Tiery, poziomy, krzywa

- Tier T (1–10) = Akt T planu 100 = poziomy profesji `10T−9 … 10T` = poziom klasy aktu. Maks. poziom profesji 100.
- XP akcji tieru: `g(T) = 10 · 1,25^(T−1)` → 10, 12,5, 15,6, 19,5, 24,4, 30,5, 38,1, 47,7, 59,6, 74,5.
- Koszt poziomu L→L+1: `(20 + 2L) · g(tier(L))` → poziom 1 = 220 XP, 49 = 2 891, 99 = 16 019.
  Liczba akcji 1→50 = 3 550, 50→100 = 8 550 (akcja zbieracka ≈ 4–6 s z dojściem → ok. 5 h do 50, 12 h do 100).
- Akcja niższego tieru: XP × `0,5^(różnica tierów)` — nie opłaca się mielić T1 na poziomie 40.
- Rzemiosło: XP receptury = Σ (surowcowy ekwiwalent składników) × g(tier receptury) × 0,9. Surowcowy ekwiwalent
  surowca = 1, półproduktu = suma jego wsadu. Rzemieślnik awansuje w tym samym tempie co zbieracz, który go zaopatruje.
- Krzywa to plik `exp-curves/profesje_v2.txt` (100 linii, nie formuła — patrz pułapka {level} w pamięci).
- Mistrzostwa: plugin `mistrzostwa` (próg 50, limit 2, ogólne: cooking, building; zmiana 2500 i 168 h).

## 3. Zbieranie (plugin Rzemiosło)

Węzeł = blok Nexo z listy węzłów (`wezly:` w configu pluginu, generowany). Interakcja:
1. Gracz klika PPM w węzeł. Plugin anuluje kliknięcie (zaklęcie w lewej ręce i garda broni nie odpalają).
2. Sprawdza poziom profesji ≥ `10T−9` i narzędzie właściwego rodzaju o tierze ≥ T w ekwipunku (dowolny slot plecaka/paska).
   Brak = komunikat na pasku („Potrzebujesz kilofa tieru II — Kowal w Stolicy").
3. Pokazuje narzędzie w dłoni (tylko pakietem do klienta i obserwatorów; prawdziwy ekwipunek się nie zmienia),
   macha ręką co 0,35 s, pokazuje pękanie bloku (stadia 0–9), dźwięk i cząsteczki rodzaju węzła, pasek postępu.
4. Czas: `3,0 s · (1 − 0,08·(tier narzędzia − T)) · (1 − 0,01·(poziom w tierze))`, min. 1,2 s.
   Ruch > 1,5 bloku, zmiana slotu, obrażenia = przerwanie bez nagrody.
5. Nagroda: 1 surowiec główny (+1 z szansą perku), 15% surowiec poboczny tieru, XP `g(T)` (z karą za niższy tier),
   zużycie 1 pkt wytrzymałości narzędzia.
6. Węzeł wyczerpuje się TYLKO dla tego gracza (podmiana bloku pakietem na wariant wyczerpany) na 45 s.
   Inni gracze widzą i zbierają normalnie — brak podbierania.

Rodzaje: górnictwo = kilof, drwalstwo = siekiera, zielarstwo = sierp, farmerstwo = motyka. Rybactwo i myślistwo niżej.

**Narzędzia** (6 rodzajów × 10 tierów): tier w nazwie i lore, wytrzymałość `150 + 60·T`, naprawa zestawem
naprawczym (kowal) — przywraca 40% wytrzymałości. Narzędzia T1 wydaje zlecenie startowe w Stolicy; od T2 robi je
rzemiosło (kowal: głowica + trzonek stolarza; stolarz: siekiera i wędka z okuciem kowala). Narzędzie nie jest bronią
(obrażenia 1, brak statystyk walki).

**Rybactwo**: dowolna woda = łowisko T1. Wyższe tiery = woda w promieniu 8 bloków od znacznika łowiska (blok Nexo
`lowisko_t<T>` stawiany przez projektantów lokacji). Wędka tieru ≥ T wymagana; tier wędki skraca czas brania
(`FishHook#setWaitTime`). Połów: surowiec główny/poboczny tieru (jak węzeł), XP `g(T)`. Własne łowienie MMOCore
(`on-fish`) wyłączone — plugin daje nagrodę.

**Myślistwo**: bestia z listy `zwierzyna:` (id MythicMobs → tier) po śmierci zostawia zwłoki na 30 s (widoczne dla
zabójcy i drużyny). PPM ze nożem tieru ≥ T = oprawianie (ta sama sekwencja co węzeł, 2,5 s). Zwykłe zabicie nie daje
XP myślistwa. Bestie Bestiariusza i Lochu 001 to T1; nowe akty dopisują swoich mobów do listy.

## 4. Surowce

Na tier i profesję zbieracką: surowiec główny (węzeł/łowisko/zwłoki) i poboczny (15% z tego samego źródła).
10 tierów × 6 profesji × 2 = 120 surowców. Nazwy i motywy z krain aktów (tabela w `KATALOG.md`).
Istniejące id T1 zostają (np. PROF_ORE_CHUNK, PROF_OAK_TIMBER) — ekwipunki graczy i receptury Chowańców działają dalej.

**Rzadkie z treści** (pkt 3 audytu — moby jeszcze nie gotowe): na akt 4 rzadkie składniki
(`PROF_A<T>_*`: esencja, trofeum elity, serce bossa, pył krainy) w droptabelach MythicMobs
`PROF_RZADKIE_A<T>_ELITA` i `PROF_RZADKIE_A<T>_BOSS`. Moby aktu podpinają tabelę jedną linią `Drops:`.
Dla aktu 1 tabele są od razu podpięte do istniejących mobów Lochu 001 i Bestiariusza. Rzadkie wchodzą w receptury
szczytowe tieru (ostatnie 2–3 poziomy tieru), więc najlepsze rzemiosło wymaga lochów (zasada z dokumentu fundamentów).

## 5. Rzemiosło

Receptury w tierze na poziomach `10T−9, −7, −5, −3, −1` (co 2 poziomy). Każdy produkt ma ujście:

| Profesja | Półprodukty (rafinacja) | Produkty końcowe i ich ujście |
|---|---|---|
| Kowalstwo | sztabka T (3 rudy) | głowice → narzędzia (kilof, sierp, motyka, nóż) T; broń i zbroja metalowa v2 (Magiczne + gniazdo); osełka (jakość broni v2); zestaw naprawczy T |
| Drwalstwo i stolarstwo | deska T (3 drewna), trzonek T | siekiera T, wędka T, broń drewniana v2 (łuk, kusza, kostur, różdżka, tarcza); trzonki → kowal |
| Krawiectwo | tkanina T (3 włókna), skóra wyprawiona T (2 skóry), cięciwa T | zbroja tkana/lekka v2; łata (jakość zbroi v2); cięciwa → stolarz |
| Jubilerstwo | kamień oszlifowany T (2 kryształy) | ozdoby v2; klejnoty do gniazd T (zdrowie, redukcja obrażeń, regeneracja, szybkość ruchu, krytyczne); pył jubilerski (jakość ozdób) |
| Alchemia | ekstrakt T (3 zioła) | mikstura lecznicza T, mikstura many T, odtrutka T, eliksiry T (siła, umysł, żelazna skóra, zwinność), olej do broni T |
| Zaklinanie | pył zaklęć T (kryształ + rzadki) | runy do gniazd T (obrażenia fizyczne, magiczne, umiejętności, siła krytyczna, skrócenie odnowienia); zwój wyjęcia klejnotu; pieczęć przebudzenia (Chowańce) |
| Gotowanie | — | posiłki T (zdrowie maks., regeneracja, stamina), uczta drużynowa (T3+) |
| Budownictwo | zaprawa T, belka T | obozowisko T (10 min, regeneracja poza walką dla drużyny w 8 blokach); zestawy przepraw (most, drabina, podpora) — klucze skrótów w lochach/na wyspach |

Sprzęt z rzemiosła istnieje tylko dla tierów, w których system przedmiotów v2 ma bazy (dziś T1 = Akt 1). Generator
dobiera bazy z `tools/przedmioty/katalog.json` — nowe akty w v2 automatycznie dostają receptury.

## 6. Buffy (plugin, statystyki MythicLib — nie efekty vanilli)

| Grupa | Źródło | Zasada |
|---|---|---|
| mikstura | alchemia: lecznicza, many, odtrutka | natychmiast; wspólny cooldown 30 s |
| eliksir | alchemia | 1 aktywny; 5 min; zastępuje poprzedni |
| olej | alchemia | 1 aktywny; 10 min |
| posiłek | gotowanie | 1 aktywny; 20 min; słabszy od eliksiru, inna rodzina statystyk (przetrwanie) |

Wartości rosną z tierem i są rzędu jednego affiksu T1–T2 v2 tego poziomu (tabela w `KATALOG.md`).
Mikstura lecznicza: `6 + 1,8·(T−1)` HP (≈25% HP postaci aktu). Buff zdejmuje się przy wyjściu z gry; śmierć go nie zdejmuje.

## 7. Perki poziomu (plugin)

Co 10 poziomów: zbieractwo +4% szansy na podwójny surowiec i −1% czasu (łącznie do +40% / −10% na 100);
rzemiosło +2% szansy na zwrot najdroższego składnika (do 20%). Mistrzowie (60/75/90/100) — tytuły z pluginu Mistrzostwa.

## 8. Zlecenia

Tablica zleceń w Stolicy (menu Q → Profesje → Zlecenia i NPC): na profesję 3 zlecenia dziennie z bieżącego tieru
gracza: „dostarcz N × półprodukt/produkt". Nagroda: XP profesji (= ekwiwalent ×1,5), pieniądze (= cena skupu ×1,3).
Zlecenie startowe każdej profesji zbieractwa: „Weź narzędzie i zbierz 5" → narzędzie T1 + 50 XP.
To jest główne ujście półproduktów poza dalszym rzemiosłem i skupem.

## 9. Usunięte i migracja

- Receptury v1 w warsztatach profesji zbieractwa (przemiany) — usunięte; warsztaty mining/herbalism/farming/fishing/hunting wyłączone.
- Węzły MythicMobs (WezlyZasobow, WezlySkills, spawner demo) — zastąpione blokami Nexo; definicje zostają w kopii.
- Nexo bloki `pakiet3` — zostają jako węzły górnicze T1–T4 (mechanika XP `custom` usunięta, XP daje plugin).
- Przedmioty v1 bez ujścia: zostają zdefiniowane (nie psujemy ekwipunków), wypadają z receptur; skup je przyjmuje.
- Akcesoria i kostury PROF_ v1 (stare tiery): zostają jako relikty, receptury usunięte.
- Chowańce: ich receptury i przysmaki zostają; id, których używają, istnieją dalej.

## 10. Co podpinają lochy i wyspy (później)

- Węzły: postaw blok Nexo węzła tieru aktu (`place` lub paleta Iris) — plugin obsłuży resztę.
- Łowiska: blok `lowisko_t<T>` pod wodą.
- Zwierzyna: dopisz id moba do `zwierzyna:` z tierem aktu.
- Rzadkie: `Drops: - PROF_RZADKIE_A<T>_ELITA` / `_BOSS` w mobie.
- Zestawy przepraw: warunek `hasitem`/skrypt w lochu zużywa `PROF_PRZEPRAWA_*`.
