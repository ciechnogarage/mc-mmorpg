# Poczta — przekazanie do integracji

## Decyzja

CMI `mail send` uruchamia **gracz** przez `asPlayer!`, więc odbiorca zobaczy rzeczywistego nadawcę. Nie wysyłać przez `[console]`: lokalne `CMI/config.yml:834-837` ustawia `ConsoleMailSender: server`. Składnia CMI: `cmi mail send <nick> <treść>`. W dialogach CMI wartość wpisana w pole jest dostępna jako `[value]` w `Commands` tego pola; `$input` nie jest udokumentowaną zmienną CMI. Dla dwóch wartości zastosowano dwa kolejne okna i zmienną dialogu `[odbiorca]`, przekazywaną komendą `cmi dialogs`. To jest **projekt do weryfikacji w grze**, bo dokumentacja nie rozstrzyga kolejności ponownego otwarcia okna po `Save: true` i otwarcia następnego okna.

## Podłączenie przez kolejny etap DeluxeMenus

- Skopiować `cmi_dialogs/Poczta.yml` do `plugins/CMI/Dialogs/Poczta.yml`; uruchomić zgodnie z procedurą CMI.
- Kafel **Napisz list**: `[console] cmi dialogs poczta_napisz %player_name%`. Gracz klika menu; niczego nie wpisuje do czatu.
- Kafel **Odebrane**: `[player] cmi mail read`. CMI wyświetli listy **na czacie**. Lore może pokazywać `Listy: %cmi_user_mail_count%` — dokumentacja mówi o liczbie *wszystkich* listów, nie potwierdza liczby nieprzeczytanych. `%cmi_user_mails%` nie jest na oficjalnej liście. Nie ma udokumentowanego placeholdera treści pojedynczego listu ani gotowego GUI skrzynki w CMI Dialogs.
- Nie dodawać `mail clear` do zwykłego kafla; usuwa pocztę.

## LuckPerms: grupa default

Nadać `cmi.command`, `cmi.command.dialogs`, `cmi.command.mail`, `cmi.command.mail.send`, `cmi.command.mail.read`. Przykład nadania: `lp group default permission set <węzeł> true` dla każdego z pięciu węzłów. `cmi.command.mail.clear`, `.sendtemp`, `.others` nie są potrzebne. Przy usunięciu funkcji cofnąć tylko węzły przyznane na potrzeby tej integracji.

## Weryfikacja w grze

1. Dwoma zwykłymi kontami otworzyć kafel, wpisać poprawny nick i treść z odstępami. Sprawdzić, czy `Dalej` otwiera drugi dialog i czy `[odbiorca]` jest podmienione, a `Wyślij` wysyła dokładnie jeden list podpisany nickiem nadawcy. Sprawdzić `Anuluj`/Esc na obu ekranach oraz pusty i błędny nick.
2. Kliknąć **Odebrane** bez wpisywania komendy; sprawdzić listę w czacie i porównać `%cmi_user_mail_count%` z liczbą listów po odczycie. Jeśli pierwszy dialog wraca po `Dalej` albo zmienna nie dociera, wstrzymać wdrożenie wysyłki — sama dokumentacja CMI nie daje gwarancji dla tego przejścia.
3. Sprawdzić uprawnienia konta default komendą administratora `cmi checkperm` lub `lp user <nick> permission check <węzeł>`; sprawdzić błędy CMI po wczytaniu dialogu.

## Źródła

- Lokalne: `plugins/CMI/Dialogs/ExampleFirst.yml`, `plugins/CMI/Translations/Locale_EN.yml:2547-2563`, `plugins/CMI/config.yml:826-837` (pod `/opt/docker/mc/data/world1`).
- [CMI Dialogs](https://www.zrips.net/cmi/commands/dialogs/), [CMI Commands](https://www.zrips.net/cmi/commands/), [CMI Permissions](https://www.zrips.net/cmi/permissions/), [CMI Placeholders](https://www.zrips.net/cmi/placeholders/), [DeluxeMenus Actions](https://wiki.helpch.at/helpchat-plugins/deluxemenus/options-and-configurations/actions).
