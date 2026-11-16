Wykonałem rundę 8 bez commitów. [Szczegóły i wyniki sond](/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/naprawy/szczegoly-r8.md).

Zasłanianie przeszło 6/6 prób na końcowym buildzie. Sprawdziłem oba warianty puszczenia PPM; aktywna konfiguracja pozostaje przy domyślnym `false`. Regresje A, D, F, N_damage, STAT3 i PW przeszły. B ma 15 przekroczeń limitu `set_slot` i jeden timeout miecza oburącz — **identyczny wynik wystąpił na bazowym JAR-ze world1**.

Końcowy JAR ma MD5 `a9d4d8c5b228`. Klon działa; nie zatrzymałem go.