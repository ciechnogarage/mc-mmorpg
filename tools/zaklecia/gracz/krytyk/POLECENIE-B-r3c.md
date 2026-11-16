# Polecenie Claude dla Agenta B — dokończenie rundy 3
Klon DZIAŁA (od 10:57), runner działa. Twoje 13 przebiegów `b-3-*` zwolniłem (.hold → .req) — idą teraz na OSTATECZNEJ
wersji A: Zaklecia commit `b8a73aa` (md5 jara `b4520dd5ddcd`), KsiegaPostaci commit `1433dfb` (md5 `9b0f9ac658c6`),
nakładka z `debug:false`. Czekaj na `.done`, analizuj, w razie potrzeby zlecaj kolejne (`b-3-14`…).
Uwagi:
- W przebiegach na STARYM jarze sonda N1 (wilk+PvP) kładzie klon — nie mieszaj jej z innymi grupami na bazowym.
- A ustalił N3: slot 0 jest zarezerwowany na broń przez KsiegaPostaci (`Bron.refresh` 316–323 wyrzuca nie-broń do
  plecaka), dopiero wtedy nośnik zajmuje pusty slot. Zweryfikuj i oceń, czy to dla gracza zrozumiałe (feeling).
- A zgłasza FAIL-e w B tylko na progach czasu/setSlotPerSec przy load ~22 — rozstrzygnij: powtórz przy load < 8 albo
  oznacz jako niewiarygodne. Load z `/proc/loadavg` przy każdym przebiegu.
- Recenzja końcowa diffów: `git -C /home/dev/Desktop/Przemek/MC/.claude/worktrees/zaklecia-naprawy diff af49d5b b8a73aa`
  i `git -C /home/dev/Desktop/Przemek/MC/.claude/worktrees/ksiega-naprawy-zaklecia diff 1e1c108 1433dfb`.
Raport: `krytyk/raport-r3.md` (nadpisz), -o `koniec-r3.txt`. STOP po raporcie.
