# Polityka obrażeń — runda 2

Rozpakowano `world1/plugins/ksiega-postaci-0.1.0.jar` do `krytyk/jary/ksiega-postaci-0.1.0/`; md5 JAR `770f720d694d`. `java -m jdk.jdeps/com.sun.tools.javap.Main -v -p -classpath ... pl.mcmmorpg.ksiega.Bron` dało `krytyk/jary/Bron.verbose.txt`.

Metoda `Bron.damage(EntityDamageByEntityEvent)`, linie źródłowe zapisane w bajtkodzie 796–800, ma listener `HIGHEST, ignoreCancelled=false`. Jeśli sprawca jest graczem, sprawdza `getHeldItemSlot()`. Slot inny niż 0/1 prowadzi bezpośrednio do `event.setCancelled(true)` (offsety 23–43 → 64–66). Dla slotu 0/1 anuluje, jeśli `virtual(mainHand)` jest prawdziwe. Nie bada `DamageCause` ani tagów MythicLib. Dowód: `krytyk/jary/Bron.verbose.txt:6332-6392`.

To tłumaczy zwykły kamienny miecz z aktywnego slotu 5 bez obrażeń (`b-1-19`, JAR Zaklęć `1a1994fbc239`) i zerowanie `mmodamage` przypisywanego graczowi przy slocie 5 (`a-r2-16d`, bazowy JAR `7f197e701920`: Płomienie 20→17 HP tylko od ognia, Odmrożenie 20→20 HP). Nakładka `damage{dc=MAGIC;tags=SKILL,MAGIC;element=...}` omija gałąź `getDamager() instanceof Player`, dlatego `a-r2-14b` na JAR `35a17b265e15` trafia oba czary. Rozwiązanie omija filtr, ale skalowanie MythicLib wymaga osobnego testu A/B z Wiara; nie ma podstaw, by uznać tę część za poprawną przed jego wynikiem.

Źródło problemu zwykłego miecza jest w `KsiegaPostaci.Bron`, a nie w pakiecie ataku bota. Kontrola ciosu ze slotu 5 powinna FAIL; dodatnia kontrola ciosu ze slotu 0/1 i zwykłym przedmiotem nadal wymaga potwierdzenia pomiarowego.
