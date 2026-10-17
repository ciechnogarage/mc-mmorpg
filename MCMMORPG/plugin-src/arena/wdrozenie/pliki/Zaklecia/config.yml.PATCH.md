# Zmiana w plugins/Zaklecia/config.yml (robi ją wdrożenie)

W sekcji `klawisz-q` zmień wyłącznie klucz `zwykle`:

```yaml
klawisz-q:
  zwykle: 'menugracza q %gracz%'
```

Pozostałe klucze, w tym obsługa Shift+Q i otwartego paska zaklęć, pozostają bez zmian.
`menugracza` wywołuje MenuKey/Zaklecia z konsoli; bez celu plugin arena otworzy
dotychczasowe `dm open hub_glowne %gracz%` z `menu-gracza.zwykle`.
