# 01. Wejście do gry, sieć serwerów i onboarding

Data: 2026-09-23. Oznaczenia: **[F]** fakt ze źródła (plik, URL, klatka), **[W]** mój wniosek.
Nie powtarzam rund 1–4 (`../1-…` do `../4-…`), tylko je uzupełniam.

---

## 1. Próba wejścia klientem vanilla bez konta premium

### Jak to zrobiłem
- Obraz `mc-client-test:latest` (vanilla 1.21.11, Xvfb, xdotool, ImageMagick), kontener `wynn-audit-client` na zwykłej sieci `bridge`, bez podłączania do naszych serwerów.
- Własny `launch.sh` (kopia w `skrypty/launch.sh`): nick offline `WynnAuditPL`, UUID offline, `--accessToken 0 --userType legacy`, `--quickPlayMultiplayer play.wynncraft.com`.
- Katalog roboczy: `/home/dev/.claude/jobs/71303296/tmp/wynn-audit/` (instancja, log klienta, zrzuty).
- Po pracy kontener usunięty (`docker rm -f`).

### Wynik
- **[F]** Serwer odrzucił połączenie. Dokładny komunikat na ekranie:
  > **Failed to connect to the server**
  > **Failed to log in: Invalid session (Try restarting your game and the launcher)**

  Zrzut: `klatki/Z01_proba_wejscia_Invalid_session.png`.
- **[F]** Log klienta tuż przed próbą: `Connecting to play.wynncraft.com, 25565` oraz `Failed to retrieve profile key pair … MinecraftClientHttpException: Status: 401`. Ten drugi błąd to brak kluczy do podpisywania czatu, czyli normalny skutek pustego tokenu.
- **[W]** Serwer działa w trybie online. Sesję sprawdza w fazie logowania, zanim wyśle konfigurację i resource pack. Bez konta Microsoft nie da się więc zobaczyć nawet ekranu pobierania packa. Niczego nie obchodziłem: żadnych proxy, cudzych kont ani tokenów.
- **[F]** Wpis serwera na liście multiplayer działa bez konta: `klatki/Z02_lista_serwerow_MOTD_v2.2.4.png`. Widać tam ikonę z literą W na tarczy, MOTD „play.WYNNCRAFT.com / The Minecraft MMORPG”, wersję „v2.2.4 ✦” i licznik „888/1500”.

### Server list ping (bez konta, własny skrypt `skrypty/slp.py`, surowe dane: `dane/server_list_ping.jsonl`)

| Host | IP | RTT z naszego hosta | Graczy | Wersja zgłaszana |
|---|---|---|---|---|
| play.wynncraft.com | 40.223.14.134 | 96 ms | 894/1500 | `WynnProxy 1.9.x-26.3.x+` |
| lobby.wynncraft.com | 40.223.14.134 | 261 ms | 894/1500 | jw. |
| eu.wynncraft.com | 40.223.14.141 | 147 ms | 894/1500 | jw. |
| na.wynncraft.com | 40.223.14.142 | 435 ms | 892/1500 | jw. |
| as.wynncraft.com | 40.223.14.143 | 317 ms | 894/1500 | jw. |

- **[F]** Wszystkie hosty leżą w jednej podsieci /24 i zwracają ten sam globalny licznik, bo stoi za nimi jedno proxy. Pomiar jest jednorazowy, RTT to tylko orientacja.
- **[F]** Proxy odsyła w polu `protocol` dokładnie numer, który przysłał klient (sprawdzone dla 47, 767, 769–774). Nazwa wersji jest stała: „WynnProxy 1.9.x-26.3.x+”. **[W]** Dzięki temu lista serwerów pokazuje „zgodny” u każdego klienta, a o faktycznej obsłudze wersji decyduje dopiero logowanie. Runda 1 ustaliła, że realne minimum to 1.21.4.
- **[F]** Odpowiedź ma pole `"modinfo":{"type":"FML","modList":[]}`, czyli udaje serwer Forge bez modów. **[W]** Pewnie chodzi o zgodność z klientami Forge i Neo.
- **[F]** MOTD to komponent JSON ze spacjami do centrowania. Wersja gry (v2.2.4) siedzi w MOTD, a nie w polu version.
- **[F]** Favicon: `klatki/Z03_favicon_serwera.png` (64×64).

### Gotowa procedura pełnego przeglądu kontem właściciela (NIE uruchamiana)

Wymaga konta Microsoft z kupioną Javą. Logowanie idzie **kodem urządzenia (device code)**: właściciel wpisuje kod na microsoft.com/link na swoim telefonie, więc hasło nigdy nie trafia do kontenera.

- **[F]** PortableMC 5.0.5 (09.09.2026, Rust, https://github.com/theorzr/portablemc) w `auth login` wywołuje `request_device_code()` (plik `portablemc-cli/src/cmd/auth.rs`). Umie też `start --join-server HOST`.

```bash
# 1. katalog roboczy tylko w tmp (token nie może trafić do repo)
R=/home/dev/.claude/jobs/<job>/tmp/wynn-live; mkdir -p $R/pmc $R/captures
curl -L -o $R/pmc.tgz https://github.com/theorzr/portablemc/releases/download/v5.0.5/portablemc-5.0.5-linux-x86_64-gnu.tar.gz
# (opcjonalnie: weryfikacja podpisu .sig kluczem f659b0f0b84a26cac635d72948caee8dc3456b2f)
tar -xzf $R/pmc.tgz -C $R/pmc

# 2. kontener z naszego obrazu, sieć bridge, NIE mc_mmorpg-net
docker run -d --name wynn-live --network bridge -u 1000:1000 \
  -e PMC_MAIN_DIR=/r/main -e PMC_MSA_DB_FILE=/r/msa.json \
  -v $R:/r mc-client-test:latest sleep infinity

# 3. logowanie kodem urządzenia: właściciel odczytuje kod z terminala
docker exec -it wynn-live /r/pmc/portablemc auth login --no-browser
#    -> "Open https://www.microsoft.com/link and enter code XXXX-XXXX"

# 4. start z Xvfb i wejściem na serwer
docker exec -d -e DISPLAY=:99 wynn-live bash -c 'Xvfb :99 -screen 0 1280x720x24 & sleep 2; \
  /r/pmc/portablemc start 1.21.11 -u <nick> -a --join-server play.wynncraft.com'

# 5. sterowanie i zrzuty jak w naszym QA
docker exec -e DISPLAY=:99 wynn-live xdotool mousemove X Y click --delay 200 1
docker exec -e DISPLAY=:99 wynn-live import -window root /r/captures/NNN.png

# 6. sprzątanie: wylogowanie, usunięcie msa.json i kontenera
docker exec wynn-live /r/pmc/portablemc auth forget <email>; docker rm -f wynn-live; rm -f $R/msa.json
```

Uwagi:
- Zasady Wynna zabraniają botów zajmujących sloty i farmienia AFK (sekcja 20 i 19 zasad, `05-…`). Sesję powinien prowadzić człowiek, a nie skrypt. Najprościej: doinstalować w kontenerze `x11vnc`, żeby właściciel grał przez VNC, a my tylko robimy zrzuty.
- Pack pobierze się automatycznie. Kopia trafi do `$R/main/…/resourcepacks` albo do `downloads/`. **Tylko wtedy** zdobędziemy aktualny oficjalny pack 2.2.4 (patrz `08-…`, §1).
- Lista ekranów do zrzutów, w tej kolejności:
  1. pobieranie packa,
  2. kreator (5 klas, tryby, ikona, opis),
  3. cutscenka,
  4. samouczek King's Recruit,
  5. HUD,
  6. kompas / Character Info,
  7. Ability Tree z aspektami i ultimate,
  8. Content Book z filtrami i historią dialogów,
  9. ekwipunek E z akcesoriami, Ingredient Pouch,
  10. profesje,
  11. bank (6 rodzajów),
  12. Store & Wardrobe (bez kupowania),
  13. `/toggle`,
  14. dialog z wyborem,
  15. pierwszy quest,
  16. spelle,
  17. Party Finder,
  18. mapa (tylko Wynntils, vanilla nie ma mapy),
  19. teleport scrollem,
  20. menu gildii (tylko podgląd).

---

## 2. Sieć światów (dane z API, bez logowania)

- **[F]** `GET /v3/player` (23.09.2026 ok. 07:20 UTC): `total: 886`. Widocznych z nazwą świata jest 726 graczy (reszta ukrywa status). Rozkład na regiony: **NA 303, AS 278, EU 145**. Aktywnych światów z graczami: **52**. Graczy na świecie: min 1, **mediana 13**, max 36 (`dane/gracze_na_swiatach.csv`).
- **[F]** Nazwy światów to region z numerem (NA1…NA22, EU2…EU16, AS2…AS21). Wynntils rozpoznaje świat po fałszywym graczu „Global [NA32]” na liście Tab (runda 2).
- **[W]** Globalny limit 1500 przy medianie 13 osób na świat oznacza małe zagęszczenie. Wynn woli wiele małych instancji niż jeden duży świat, a to przekłada się na wydajność (zob. `08-…`). U nas, przy jednym world1, nie ma czego kopiować, dopóki nie przyjdzie skala.
- **[F]** Od rangi HERO jest `/switch` (zmiana świata bez lobby) i kolejka „Priority”, a CHAMPION ma „Super Priority” (wiki Ranks, `06-…`).

---

## 3. Onboarding krok po kroku (nowe względem rundy 2)

Przepływ z rundy 2: pack → od razu kreator → cutscenka → King's Recruit → Ragni → stoisko sklepu. Uzupełnienia:

1. **Plakaty aktualizacji i festiwali przy wejściu.** **[F]** WynnEdits 3.5 (08.2026, nadpisuje ścieżki oficjalnego packa) ma okna-plakaty z tekstem wypisanym w teksturze. Wiszą obok siebie w `textures/font/interface/update/fruma/{info,beta,release}.png`, `update/rekindled/{beta,release}.png` i `interface/event/{heroes,bonfire,spirits,blizzard}/join.png`. Plakat Fruma wylicza nowości, a festiwalowy zaprasza na minigrę i 3 darmowe skrzynki „w sklepie” (`klatki/P_m_interface_b.png`). **[W]** Tekst jest wypalony w teksturze, bo tytuł skrzyni nie zawija linii. Zmiana ogłoszenia wymaga więc zmiany packa.
2. **Łączenie konta WWW.** **[F]** Okno `account_link/code.png`: „Here's a token you can use to link your account on our website”, 6 kafelków z kodem. **[F]** Nagrody za połączenie: 24 h podwójnego XP za Discord (wiki Experience Points) i darmowy Wyvern za konto WWW (ogłoszenie 2.2.4, `api/article_582.json`). **[W]** To tani lejek z gry do społeczności.
3. **Content Book jako „przewodnik”.** **[F]** Filmy-poradniki nazywają to wprost: „#2 Follow the Book” (Olinus10, 31.01.2025, https://youtu.be/gptf8c3QKLY). Tooltip questa w księdze pokazuje: etap, wymagania ze znakami ✔/✘, długość, trudność, nagrody (XP, emeraldy, „Access to …”) i „Click to Track” (`klatki/V14_…`). Śledzony quest daje zielony romb z wiązką i dystansem w metrach (`klatki/V19_…`, `V27_…`).
4. **Sidebar prowadzi gracza.** **[F]** Stałe sekcje z nagrań 2026: „Tracked Quest” (cel i koordynaty), „Daily Objective”, „Guild Obj.”, a w raidzie „Party” i „Raid: Time Left, Challenges x/4” (`klatki/V21_…`).
5. **Podpowiedzi ekranowe.** **[F]** Wielkie tytuły „SLAY! / Kill mobs to continue”, „Complete! / You've succeeded!” i „Warping in 3 / DO NOT MOVE” przed teleportem (`klatki/V28_…`, `V16_…`).
6. **Hologramy z wymaganiami.** **[F]** Nad węzłem zbierackim: „Oak / ✔ Woodcutting Lv Min: 1 / ✘ Equipped Tool: Axe” (`klatki/V09_…`). Nad stacją: „[ikona myszy] Click to Open Feeder / Feed, breed, and manage mounts” (`klatki/V08_…`).
7. **Pierwsze kroki poza samouczkiem.**
   - **[F]** Party dzieli XP: „another 20% of the experience gained will go to nearby party members” (wiki Experience Points).
   - **[F]** Punkty umiejętności resetuje się za darmo w mieście, a drzewko za Ability Shardy (Reddit 1wajxrx).
   - **[F]** Twórca serii dla nowych graczy wytyka brak objaśnienia: „Stats, skill points, and well, how the skill tree actually works… it was a little overwhelming” (Selek, 16.02.2026, https://youtu.be/EjTXUh1yK9I).

## 4. Mechanizmy retencji (co trzyma gracza)

| Mechanizm | Fakty | Źródło |
|---|---|---|
| Codzienne i tygodniowe cele | sidebar „Daily Objective” i „Guild Obj.”, okno `Daily Rewards`, bonus dzienny lootrunu („Daily Bonuses Available! +100 Lootrun Experience, +10 Reward Pulls, +1 Reward Reroll”) | Wynntils `reward/DailyRewardContainer.java`, `klatki/V29_…` |
| 4 festiwale rocznie | Heroes, Bonfire, Spirits, Blizzard; każdy ma minigrę, skrzynki czasowe, kosmetyki i **tło menu postaci** do zdobycia | Wynntils `FestivalType.java`, wiki (4 strony po 47–73 tys. znaków) |
| Sezony gildii | 33 sezony (0–32) od 10.06.2021, ostatnie trwają 6–11 tygodni (sezon 32: 24.07–27.09.2026), nagrody progowe za SR (30 progów) | `api/guild_seasons.json`, `dane/sezon_gildii_nagrody.csv` |
| Rotacje | pula mythików lootrunu zmienia się co tydzień; nagrody raidów zależą od wybranych gambitów | wiki Mythic Items, Lootrunning |
| Rankingi | **88 typów** rankingów, m.in. per profesja, per raid, per tryb specjalny i archiwalne sezony | `api/v3_leaderboards_types.json` |
| Statystyki „shiny” | 12 liczników (Mobs Killed, Raids Won, Wars Won…) doklejanych do mythików shiny | Wynntils `shiny_stats.json` |
| Double XP | weekendy z XP ×2 (sprzedaż rang w tym samym poście), bomby XP od graczy | `api/latest_news.json` |
| Tryby wyzwań | Hardcore, Ironman, UIM, Craftsman, Hunted (HUICH) z własnymi rankingami | runda 1, `v3_leaderboards_types.json` |
| Treść „do odkrycia” | 98 sekretnych odkryć, 130 jaskiń, 70 world eventów, ~303 questy i mini-questy | `04-…` |

- **[F]** Wypowiedź Salteda o liczbie postaci (runda 2): 43% graczy ma 1 postać, 14% 2, 8% 3.
- **[W]** Większość gra jedną postacią, więc retencja opiera się na treści pionowej (questy, lootruny, raidy, sezony), a nie na alt-ach.

## 5. Wnioski dla nas [W]

1. Nie wejdziemy do Wynna bez konta. Właściciel może to zrobić bezpiecznie procedurą z §1. Najwięcej da to przy zdobyciu **aktualnego packa 2.2.4** oraz zrzutów kreatora 2026 i Wardrobe.
2. Plakat nowości przy wejściu jest tani: okno DeluxeMenus z glifem Nexo, otwierane raz po aktualizacji (flaga w PAPI/LuckPerms meta). Treść wypalona w teksturze wymaga nowej grafiki na każdą aktualizację, więc dla nas lepszy jest glif tła z tekstem wpisanym w lore albo dialog Papera (mamy go w logowaniu, `okno-logowania-dialog`).
3. Sidebar z celem, śledzenie questa z wiązką i dystansem oraz hologramy z ✔/✘ wymagań to najtańsze elementy prowadzenia gracza. Da się je zrobić na naszym stacku (MythicHUD, DecentHolograms, MMOCore quests) bez własnego kodu, poza wiązką (sprawdzić: BlockDisplay albo beacon beam).
