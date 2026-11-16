# Regulamin serwera: SZKIC do akceptacji (2026-09-23)

Źródła: zasady Wynncrafta (`docs/research/wynncraft/audyt/05-…`, sekcja zasad), nasza reguła AG-05
(`/home/dev/Desktop/Przemek/MC/docs/product/00-tier-0-product-identity.md:343-348`: AFK i powtarzanie
jak bot nie mogą się opłacać). Zastępuje angielski `plugins/CMI/CustomText/rules.txt` z paczki CMI.
Wejście z menu (hub Q → Społeczność → Regulamin), nie tylko przez `/rules`.

Do decyzji przed wdrożeniem: punkty oznaczone **[?]**.

---

## Strona 1: Zachowanie

1. Szanuj innych graczy. Bez obrażania, nękania, rasizmu, seksizmu i gróźb.
2. Bez spamu, reklam innych serwerów i pisania samymi WIELKIMI LITERAMI.
3. Nie proś administracji o rangi, przedmioty ani uprawnienia.
4. Nie podszywaj się pod administrację ani innych graczy.
5. Oszustwa w handlu są zakazane. Umowa zawarta na czacie obowiązuje.
6. Decyzja administracji jest ostateczna. Odwołać się możesz na [?] (Discord / forum).

## Strona 2: Gra uczciwa

7. Zakaz cheatów, X-raya, auto-klikerów i modów dających przewagę.
8. **Dozwolone mody:** minimapy bez radaru mobów i graczy, Simple Voice Chat, mody wydajności
   (Sodium, Lithium, Iris), zmiana wyglądu HUD. W razie wątpliwości zapytaj administrację.
9. **Makra:** jedno naciśnięcie = jedna akcja. Makra, które same rzucają kombinacje umiejętności,
   są zakazane. [?] (zależy od decyzji o sposobie rzucania umiejętności)
10. **AFK i boty:** zakaz farmienia w nieobecności (AFK), skryptów i botów zajmujących miejsce.
    Postać bez ruchu nie zdobywa doświadczenia ani łupów.
11. Wykorzystywanie błędów (bugów, duplikacji przedmiotów) jest zakazane. Zgłoś błąd, a dostaniesz
    nagrodę. [?] (czy nagradzamy zgłoszenia)
12. Jedno konto = jeden gracz. Multikonta do handlu samemu ze sobą lub omijania limitów są zakazane.
    [?] (czy dopuszczamy drugie konto w ogóle)

## Strona 3: Świat i handel

13. Nie niszcz i nie blokuj wspólnych miejsc (miasta, wejścia do lochów, stacje).
14. Nie blokuj innym dostępu do mobów, bossów ani surowców.
15. Handel prawdziwymi pieniędzmi (sprzedaż przedmiotów, złota lub kont poza serwerem) jest zakazany.
16. Nazwy postaci, gildii i przedmiotów nie mogą być obraźliwe.

## Kary (orientacyjnie)

Ostrzeżenie → wyciszenie / wyrzucenie → ban czasowy → ban stały. Cheaty, duplikacja i handel za
prawdziwe pieniądze: od razu ban czasowy lub stały.

---

## Uwagi techniczne do wdrożenia

- Plik: `plugins/CMI/CustomText/rules.txt` (format CMI: `<AutoPage>`, `<NextPage>`, kolory `&`).
- AFK: CMI ma dziś `Afk.Enabled: false` (`plugins/CMI/Settings/Afk.yml:3`). Punkt 10 wymaga włączenia
  wykrywania AFK albo innego egzekwowania (np. brak XP bez ruchu). Osobna decyzja.
- Kafelek „Regulamin” w hubie Q (reguła „menu zamiast komend”).
