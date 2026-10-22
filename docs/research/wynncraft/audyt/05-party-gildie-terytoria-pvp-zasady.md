# 05. Party, gildie, terytoria i wojny, PvP, tryby, zasady i moderacja

Źródła:
- API (`/guild/list/territory`, `/guild/list/guild`, `/guild/seasons`, `/leaderboards/*`, `/player/{nick}`),
- wiki (Guilds, Territories, Guild Seasons, Party Finder, Special Gamemodes),
- kod Wynntils (`models/territories`, `models/war`),
- film TheNelston „The ULTIMATE Guild War Guide” (18.02.2024, https://youtu.be/PJj3qY3Gkb0, napisy),
- zasady gry (https://forums.wynncraft.com/threads/game-forum-rules.111874/),
- changelog 2.2.4.

---

## 1. Party i Party Finder

- **[F]** Party zakłada się komendami (runda 2). **Party Finder** to NPC w miastach, dungeonach i raidach oraz komenda `/partyfinder` (wiki Party Finder):
  - opis do 140 znaków,
  - minimalny poziom 1–106,
  - rozmiar 2–10 graczy, **do 16 z rangą**,
  - 7 typów: Grinding Mobs, Grinding Professions, Boss Altars, Quests, Guild Recruitment, Social, Others.
- **[F]** **Party Queue** to kolejka do raidów i skorumpowanych dungeonów z auto-dopełnianiem. Okna: search / create / join / queue / match_found. „Match found” to animacja z 16+ klatek (`party_finder/match_found.png` 256×4352, glify `\uE041–\uE051` w Wynntils).
- **[F]** Party dostaje 20% XP od zabójstw bliskich członków. Członkowie mają HP na sidebarze i żółty kontur (runda 2).
- **[W]** Kolejka z dopełnianiem do raidów pozwala grać solo w treść grupową. To główna przyczyna, dla której raidy żyją przy medianie 13 osób na świat.

## 2. Gildie

- **[F]** W API jest **18 301 gildii** (`/guild/list/guild`).
- **[F]** Zakładanie **od 2.2.4 (11.09.2026)**: „The Guild Master can now be found in Ragni to create guilds starting from Combat level 5. Guilds now cost 4 Emerald blocks to create, but are not eligible to start wars… Guild Wars can be unlocked for 20 Liquified Emeralds from Guild Level 50 onwards” (`api/article_581.json`). Wcześniej: poziom 75 i 20 LE (wiki Guilds). **[W]** Wynn obniżył próg, żeby gildie łapały graczy wcześnie. Wojny zostały progiem.
- **[F]** 6 rang: Owner, Chief, Strategist, Captain, Recruiter, Recruit, z tabelą uprawnień (bank, zaproszenia, kolejka wojen, awanse). Nowy członek ma **3 dni karencji** na bank, wojny i kolejkę oraz **7 dni** na nagrody (wiki Guilds).
- **[F]** Nazwa gildii ma 4–30 znaków, tag 3–4. Nie może udawać ról (Moderator, Developer, Wynn).
- **[F]** Gildia ma poziom (gracz oddaje swoje XP: `/guild xp`), bank publiczny i prywatny, tomy gildyjne, odznaki, cele tygodniowe („Guild Obj.” w sidebarze), dyplomację (sojusze), log i wyspę housingu. Okna w Wynntils: Manage, Members, Bank, Territories, Badges, Log, Diplomacy (`dane/wynntils_okna.csv`).
- **[F]** Gildia może odpalić **Guild Raid**, trudniejszy wariant raidu dla 4 członków (`04-…`).

## 3. Terytoria (mapa strategiczna)

**[F]** Stan z 23.09.2026 (`api/v3_guild_list_territory.json`, 437 terytoriów):

- **437 terytoriów**, z czego **22 to HQ**. Terytoria posiada **23 gildie**.
- Top posiadacze: Sequoia 92, Avicia 59, The Nameless Samurai 56, Aequitas 52, Titans Valor 31, KongoBoys 31, Empire of TKW 18, Cyanide Legion 15, Paladins United 15, Pavilion 12. **[W]** 4 gildie mają ~59% mapy (259/437).
- Terytoria tworzą **graf**: średnio 3,17 połączenia, maksimum 6 (`links`). Atakuje się tylko po ścieżce od HQ.
- Każde terytorium generuje 5 zasobów:
  - **EMERALD 9 000/h** (393 terytoria), 18 000/h (22) albo 1 800/h (22),
  - ORE, WOOD, FISH, CROP po 3 600/h (~411 terytoriów), 7 200/h (2–4) albo 900/h (22).

  Magazyny mają limity (np. emeraldy 75 000, zasoby 22 500).
- Skarbiec (treasury) rośnie z czasem posiadania: VERY_LOW 91, LOW 272, MEDIUM 71, HIGH 3. Wyższy skarbiec daje więcej zasobów przy przejęciu („A higher treasury will give more territory resources upon capture”, `klatki/V25_…`).
- Obrona: VERY_HIGH 24, HIGH 169, MEDIUM 171, LOW 6, VERY_LOW 67.
- **[F]** 21 typów ulepszeń terytorium (Wynntils `TerritoryUpgrade.java`):
  - wieża: DAMAGE, ATTACK, HEALTH, DEFENCE, STRONGER_MINIONS, TOWER_MULTI_ATTACKS, TOWER_AURA, TOWER_VOLLEY,
  - bonusy dla członków: GATHERING_EXPERIENCE, MOB_EXPERIENCE, MOB_DAMAGE, PVP_DAMAGE, XP_SEEKING, TOME_SEEKING, EMERALD_SEEKING,
  - ekonomia: RESOURCE_STORAGE, EMERALD_STORAGE, EFFICIENT_RESOURCES, RESOURCE_RATE, EFFICIENT_EMERALDS, EMERALD_RATE.

## 4. Wojny gildii

**[F]** (film TheNelston, 18.02.2024; zasady 2024, mogły się zmienić)
- Na terytorium wpisujesz `/guild attack`. Okno pokazuje obronę, skarbiec, trasę kolejki od HQ, cenę i odliczanie (`klatki/V25_…`).
- Koszt kolejnych ataków w emeraldach gildii: 0 / 200 / 800 / 2 000 / 4 000.
- Do 5 graczy w granicach terytorium przenosi się na arenę. 25 s grace, potem **wieża** atakuje najbliższego, a po chwili pojawiają się miniony.
- Pasek wieży jest na bossbarze. Efektywne HP to HP / (1 − defence).
- Obrażenia wieży rosną co minutę: +20%, +16,7%, +21,4%, +17,6%, czyli **podwajają się co 4 minuty**. „most wars will last no longer than 1 minute”.
- Ataki specjalne: **Tower Volley** (AoE w tylnej 1/3 areny) i **Tower Aura** (fala po ziemi, trzeba przeskoczyć). Oba zadają true damage, który zwykle zabija.
- Lag przy wojnach to częsty problem („unless there's server lag which is actually more of a common occurrence”).

### Sezony
- **[F]** 33 sezony od 10.06.2021. Ostatnie trwają 6–11 tygodni. **120 SR/h za trzymanie** terytorium (ze skalowaniem według liczby posiadanych terytoriów i ręcznego mnożnika), **40 SR za wojnę** (`api/guild_seasons.json`, wiki Guild Seasons).
- **[F]** Na starcie sezonu wszystkie terytoria wracają do „Nobody [None]”, a obrony i ulepszenia się zerują.
- **[F]** Sezon 32 ma 30 progów nagród za SR (`dane/sezon_gildii_nagrody.csv`):
  - od 1 SR odznaka „Contender”,
  - emeraldy (2 048 → 102 400),
  - sloty banku publicznego i prywatnego,
  - tomy gildyjne,
  - odznaki Bronze/Silver/Gold/Platinum/Diamond (Diamond przy 5 000 000 SR).

  Ranking sezonu daje dodatkowo XP boosty dla członków (top 200/25/15/5), odznaki top 100/10/3/2/1, strukturę „iceBanner2” (top 20) i kosmetyk **GUILD_CROWN** dla zwycięzcy.
- **[F]** Zasada „Territory Stalling”: zakaz manipulowania cooldownami przez gildie-córki (zasady, pkt 18).
- **[W]** Terytoria to warstwa meta nad całą mapą. Nagrody rozkładają się na małe gildie (próg 1 SR już daje odznakę), a wielkie gildie grają o ranking. U nas plugin `Guilds` nie ma terytoriów. To byłby osobny, duży projekt (graf, zasoby, wieże jako bossy MythicMobs, areny instancjami).

## 5. PvP i tryby specjalne

- **[F]** PvP w otwartym świecie jest tylko w trybie **Hunted**: PvP poza miastami i +50% XP, odblokowany questem „A Hunter's Calling” (runda 1). Poza tym są pojedynki `/duel`. W profilu API są pola `pvp.kills/deaths`.
- **[F]** Śmierć w Hardcore nie liczy się m.in. w: world evencie, gdy ktoś z party przeżył; w raidzie; w lootrunie; w `/duel` (ale śmierć od DoT po duelu już tak) (wiki Special Gamemodes).
- **[F]** Rankingi osobno dla trybów: huntedContent, craftsmanContent, ironmanContent, ultimateIronmanContent, hardcoreContent, hardcoreLegacyLevel, huicContent, huichContent, hicContent, hichContent (`api/v3_leaderboards_types.json`).
- **[W]** Wynn trzyma PvP na marginesie (opt-in przy tworzeniu postaci). Dla PvE-owego MMO to rozsądne. U nas: WorldGuard flag `pvp` + tryb postaci w MMOProfiles/LuckPerms.

## 6. Zasady gry i moderacja

**[F]** 20 punktów zasad (https://forums.wynncraft.com/threads/game-forum-rules.111874/, streszczenie przez WebFetch):
1. Hacki i zmodyfikowane klienty dające przewagę są zakazane. Mody wydajnościowe (Sodium, Optifine) są dozwolone.
2. **Makra**: tylko jedna 3-klikowa kombinacja spella na wciśnięcie albo komendy czatu. Ciągłe przytrzymanie ataku jest OK.
3. Wykorzystywanie bugów zakazane, także dzielenie się informacją o nich.
4. Nękanie. Przeklinanie dozwolone, jeśli nie jest skierowane w kogoś.
5. Spam.
6. Nadużywanie `/report`.
7. Scam, w tym „selling grossly overpriced items”.
8. Przedmioty vanilla, UI i zdupowane: nie wolno trzymać ani handlować.
9. Reklama treści Wynna: raz na godzinę, maks. 3 razy dziennie.
10. Podszywanie się.
11. Griefing, w tym „Intentionally stalling a raid party”.
12. **RMT zakazane**.
13. Alty dozwolone, ale kara obejmuje wszystkie konta.
14. Leaki niewydanej treści.
15. Brak zwrotów za przedmioty w grze.
16. Nieodpowiednie treści i skiny.
17. Minimapy dozwolone. Radary mobów i skrzyń oraz mapy jaskiń zakazane.
18. Territory stalling.
19. **AFK farming** zakazany, także „weights on input devices”.
20. Boty zajmujące sloty.

- **[F]** Lista modów dozwolonych i zakazanych. Zakazane m.in.: loot beams, powiadomienia o dropie, X-ray, freecam, usuwanie blindness, auto-loot, sortowanie ekwipunku, auto-picie mikstur. Dozwolone m.in.: autoclickery według zasad makr, Litematica (tylko nakładka), ReplayMod, Mouse Tweaks, proximity voice chat. „If a mod or a feature is not in this list, it does not automatically mean it's allowed”.
- **[F]** Wynntils jest de facto standardem społeczności (poradniki: „Be sure to use the wyntills modpack”, Reddit 1hvpm5q). Jest też modpack Wynncraft Plus i mod Voices of Wynn z głosami postaci (Reddit 1qs4rl6).
- **[W]** Wynn toleruje mody informacyjne i kosmetyczne, a zakazuje automatyzacji i przewagi informacyjnej (radar, xray). Dla nas: spisać listę modów od razu. Mamy voicechat, więc proximity voice jest nasz. Sprawdzić, czy chcemy pozwalać na mapy (Xaero) i czy wyłączamy radar jaskiń przez „fair play” w Xaero.
