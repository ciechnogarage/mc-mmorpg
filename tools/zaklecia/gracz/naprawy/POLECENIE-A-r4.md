# Polecenie Claude dla Agenta A — runda 4

## Co się stało w rundzie 3
Twoje zlecenia nie ruszyły, bo padł runner kolejki (moja strona, nie Twoja). Potem maszyna miała brak RAM-u i przerwałem
`a-r3-04` w połowie. `a-r3-01..03` wykonane (bazowy test + build obu pluginów). Runner ucinał filtr do 60 znaków —
`STAT2` zamieniło się w `S`; poprawione (300 znaków).

## Recenzja Claude rundy 3
- **KsiegaPostaci `Bron.damage` — POPRAW logikę.** Twoja wersja stosuje zasadę broni tylko, gdy zarejestrowane metadane to
  `MeleeAttackMetadata`. MythicLib rejestruje cios wręcz we własnym listenerze HIGHEST — tym samym priorytecie co nasz;
  kolejność między pluginami zależy od kolejności ładowania. Jeśli nasz biegnie pierwszy, metadanych jeszcze NIE MA →
  `return` → miecz ze slotu 5 zadaje obrażenia = złamana zasada broni. Odwróć: zasadę broni stosuj ZAWSZE, CHYBA że są
  zarejestrowane metadane i NIE są one ciosem wręcz (to jest właśnie `mmodamage` — rejestrowane przed zdarzeniem Bukkit).
  Sonda musi pokazać oba: zwykły miecz ze slotu 5 = 0 obrażeń, ze slotu 0 = obrażenia, zaklęcia każdego typu z mieczem w
  slocie 5 = obrażenia, `STAT2` Wiara 1,60×.
- Wybor (zamknięcie paska przy śmierci/wyjściu/zmianie świata, strażnik sesji LPM), RzutyOdrzutowe (mana po sukcesie),
  Nosnik (walidacja id), Strumien (wąski strumień dla rzucającego za flagą) — PRZYJĘTE warunkowo, do potwierdzenia testem.
- **N5 decyzja właściciela: leczenie graczy spoza drużyny jest OK** (`support.party_other: true` w nakładce zostaje).
- Kod nadal BEZ commitów — commituję ja po zielonych testach.

## Runda 4 — zakres
1. Popraw `Bron.damage` jak wyżej.
2. `build bazowy` → `test ABCDEF,N_damage,N_invalid,N_melee,N_r1,N_r2,N_heal,STAT2` → `build` → ten sam test. Tabela A/B
   (te same identyfikatory testów). Każdy nowy FAIL względem bazowego = regresja, dopóki nie udowodnisz, że harness.
3. N3 (zwykły przedmiot wraca do nośnika): `test C` 3× pod rząd z debug włączonym tylko na tę sondę (nakładka), potem
   debug z powrotem `false`; przyczyna z logów `ZAKDBG slot/pusta`. Poprawka tylko przy pewnej przyczynie.
4. Ostatni `build` = Twoja wersja z `debug: false`. Szczegóły `naprawy/szczegoly-r4.md`, krótki raport -o.
Kolejka: przy kodzie 8 (mało dysku) STOP. Obciążenie maszyny bywa wysokie — nie wyciągaj wniosków o czasach/wydajności.
STOP po raporcie.
