# Gildie — projekt v1 (szkic do recenzji, 19.10)

Serwer MMORPG na Paperze 1.21.x, tylko world1. Stos: MMOCore/MMOItems/MythicLib, MythicMobs, MythicDungeons (lochy), ModelEngine, Nexo (pack, glify GUI), DeluxeMenus (menu pod Q), WorldGuard, LuckPerms, CMI (ekonomia/Vault), voicechat, własne pluginy (Chowańce, Rzemiosło+Mistrzostwa, Wiary, Arena, Mapa, HUD...).
Cel właściciela: gildie mają być dużą, rozbudowaną, konkretną częścią gry.

## 0. Fakty techniczne (sprawdzone w bajtkodzie)
- Plugin Guilds 2.0.10 (itz_leoo/PixelStudios, `me.leoo.guilds`): członkowie, 4 rangi z uprawnieniami, czat, wyszukiwarka, 100 poziomów, osiągnięcia, 1 licznik zadania tygodniowego, SQLite. Zamknięty kod, angielski, pod BedWars. EXP gildii dziś tylko z hooka BedWars1058 i /guildadmin.
- API Guilds: `GuildsAPI.getGuildByPlayer/ByUuid/ByName`, `LevelProvider.addXp`, `GuildQuest.addProgress`, 22 eventy (Create, Delete, Join, Leave, Kick, Promote, LevelChange, XpChange, QuestComplete, Rename...).
- MMOCore `guild-plugin: GUILDS` = inny plugin (Glare) → nie działa. `MMOCore.plugin.guildModule` jest public, nie final; interfejs: `getGuild(PlayerData)`, `getRelationship(Player,Player)` → podmiana przez nasz plugin daje relacje MythicLib (sojusznik/wróg dla skilli i PvP).
- PvP dziś wyłączone poza `arena_pvp` przez reguły interakcji MythicLib (brak party_other/guild_enemy w pvp_on.offense).
- Kara za śmierć: EQ zostaje, -10% EXP, debuff 3 min.
- Budowle: pipeline .iob → .nbt → `place template` z konsoli.

## 1. Architektura
- Guilds = baza (członkowie, rangi, uprawnienia, czat, DB, poziom).
- Nasz plugin `Gildie`: mostek MMOCore, źródła EXP z naszych treści, cele tygodniowe (wiele), nagrody, siedziba, bank, budynki/skórki, wojny, ranking, okna (DeluxeMenus/Nexo, wszystko z Q, gracz nie wpisuje komend).
- MMOCore `guild-plugin: NONE` (koniec dubla `/guild`), potem podmiana `guildModule` na nasz.
- Dane naszego pluginu kluczowane UUID gildii; `GuildDeleteEvent` → zwolnienie działki, archiwum.
- Plan B: gdy Guilds zacznie ograniczać — własna baza za tym samym interfejsem, reszta zostaje.

## 2. Postęp gildii
- EXP gildii z: lochów (więcej za trudniejsze), bossów, mobów MM (mało), rzemiosła (zlecenia), Chowańców, celów tygodniowych. Dzienny limit wkładu na gracza (anty-farma altami).
- Cele tygodniowe: 3 naraz, losowane z puli powiązanej z naszymi treściami, skalowane liczbą AKTYWNYCH członków (gildia 3-osobowa ma realną szansę).
- Nagrody poziomów: jakościowe (odblokowanie budynku, pracowni, slotu banku, teleportu, sztandaru), nie +% do statystyk bojowych. Nie mogą zabijać lupu z lochów.
- Okres karencji po dołączeniu (np. 48 h) zanim gracz dostaje nagrody gildii — przeciw skakaniu po gildiach.

## 3. Siedziba
- Osobny świat `gildie` z siatką działek (WorldGuard region na działkę, flagi: brak PvP, budowa wg rang).
- Działka ma SLOTY budowlane (gniazda) o stałym obrysie. W gnieździe stawia się budynek funkcyjny z .nbt: ratusz, bank, kuźnia/stacje MMOItems, suszarnia zielarza, zagroda Chowańców, kapliczka, sala trofeów, portal.
- Poziom budynku (namiot → dwór → twierdza) odblokowuje funkcje; koszt: waluta + surowce z naszych treści (pochłaniacz ekonomii).
- Funkcje budynku przypięte do KOTWIC (markery w .nbt), nie do bloków — po zmianie skórki stacje/NPC odtwarzają się w kotwicach.
- Bank i magazyny wirtualne (nie skrzynie w budowli) — zmiana skórki/budynku niczego nie niszczy. Log banku: kto, co, kiedy. Uprawnienia wg rangi (wpłata/wypłata/limit dzienny).
- Teleport do siedziby z menu Q i szybkiej podróży.
- Wolna strefa ogrodu na działce: gildia buduje sama (survival), poza gniazdami.

### 3a. Skórki i własne budynki (prośba właściciela)
- Każdy budynek funkcyjny ma kilka skórek (warianty .nbt o tym samym obrysie i kotwicach): np. elficka, krasnoludzka, gotycka, sadowa (Kraj Sadów). Kupowane za walutę gry / zasoby gildii.
- „Własny budynek”: gildia zgłasza projekt (zbudowany w strefie ogrodu albo na serwerze-kreatywnym), admin zatwierdza, konwersja do .nbt z wymaganymi kotwicami i obrysem, opłata w grze. Ręczna moderacja (treść, lag: liczba encji/bloków ze stanem).
- Skórki = wyłącznie kosmetyka (zero przewagi). Jeśli kiedyś za prawdziwe pieniądze — tylko kosmetyka (EULA Minecrafta).
- Zmiana skórki: podgląd (hologram/duch obrysu albo zrzut w oknie), potwierdzenie, place template, odtworzenie kotwic.
- Sztandar/herb gildii wieszany automatycznie na budynkach.

## 4. Rywalizacja
### 4a. Otwarta strefa PvP (region na mapie)
- Wejście jawne: granica, ostrzeżenie (tytuł + dialog przy pierwszym wejściu), HUD pokazuje stan PvP.
- Wszyscy mogą walczyć (nie tylko gildie) — ale przejmować punkty mogą tylko gildie. Członkowie tej samej gildii i sojusznicy nie ranią się (relacja z mostka MMOCore).
- W strefie: rzadkie surowce/węzły zbieractwa dla Rzemiosła, punkty kontrolne (terytoria). Punkt daje gildii surowce budowlane/EXP gildii, NIE moc gracza.
- Ochrona: brak PvP dla gracza poniżej poziomu X; gracz z debuffem śmierci nie może być celem przez chwilę przy odrodzeniu; anty spawn-camp (odrodzenie poza strefą).
- Kara za śmierć w strefie: jak zawsze (EQ zostaje) + utrata zebranych w strefie surowców z „worka strefy” (łup dla zabójcy) — ryzyko bez niszczenia EQ.

### 4b. Wojny gildii
- Wypowiedzenie wojny o punkt: zapowiedziane okno czasowe (np. 24 h wcześniej, wybierane przez obrońcę z puli godzin) — koniec przejmowania o 3 w nocy.
- Oblężenie: limit uczestników na stronę, licznik przejęcia, BossBar.
- Utrzymanie terytoriów kosztuje (żeby jedna gildia nie zjadła mapy); im więcej punktów, tym drożej.

### 4c. Arena gildii
- Na istniejącej Otwartej Arenie: mecze 3v3/5v5 gildia na gildię, ranking sezonowy (ELO), turniej co sezon.
- Anty-wintrading: limit meczów z tą samą gildią, ranking liczony z różnych przeciwników.
- Nagrody sezonu: kosmetyki, tytuły, sztandary, skórki budynków — zostają na zawsze.

## 5. Wspólna gra (PvE)
- Lochy gildyjne (MythicDungeons): trudniejszy wariant, wejście dla członków, ranking czasów.
- Boss świata co X godzin: ranking obrażeń per gildia.
- Zlecenia rzemieślnicze gildii (wykuć/zebrać N rzeczy z naszych PROF_*).

## 6. Tożsamość i społeczność
- Sztandar/herb, tag w HUD (karta gracza) przez PAPI.
- Dyplomacja: sojusz / wróg / neutral (wpływa na relacje MMOCore).
- Patron gildii z Wiar (łaska tylko w siedzibie) — UWAGA: Wiary mają już patrona per postać.
- Kronika (log zdarzeń) + odznaki.
- Wyszukiwarka/rekrutacja z opisem, poziomem, językiem.
- Grupa głosowa gildii (voicechat) tworzona automatycznie.

## 7. Kolejność
1. Mostek MMOCore + NONE + polskie okna pod Q + EXP z lochów/mobów.
2. Cele tygodniowe + nagrody poziomów.
3. Siedziba: działki, gniazda, bank, budynki, skórki.
4. Arena gildii, potem otwarta strefa PvP i terytoria/wojny.
