# Skalowanie obrażeń po zamianie `mmodamage` → `damage`

`b-2-03`, JAR A `e3075231a491`, nakładka Płomienie/Odmrożenie z `damage{dc=MAGIC;tags=SKILL,MAGIC;element=FIRE/ICE}`. Sonda `STAT` nadała MagTest 40 punktów Wiary przez `mmocore admin attribute give` i potwierdziła placeholderami zmianę `wiara 0→40`, `skill_damage 0→60%`; `magic_damage=0` bez zmiany. Po pomiarze `take` przywróciło Wiara=0 (`STAT-RESET` w `.out`). Cel: osobny husk bez AI, 20 HP; ten sam czar w lewej dłoni, PPM przez 850 ms, mana20.

| Czar | Wiara 0 | Wiara 40 / skill_damage +60% |
| --- | --- | --- |
| Płomienie | 4 ważne próby, każda 20→12 HP (8 obrażeń); 1 BLAD przypisania przez GUI | 5/5 prób, każda 20→12 HP (8 obrażeń) |
| Odmrożenie | 5/5 prób, każda 20→13 HP (7 obrażeń) | 5/5 prób, każda 20→13 HP (7 obrażeń) |

W obrębie wersji A brak jakiegokolwiek skalowania z Wiary mimo +60% rzeczywistej statystyki. To wystarcza do **ODRZUCENIA** zamiany jako pełnej naprawy: przywraca obrażenia na slocie 5, ale usuwa progresję jednej ze statystyk zdefiniowanych w `world1/plugins/MMOCore/attributes/default_attributes.yml:35-41`. Mechanizm jest zgodny z bajtkodem: `MMODamageMechanic` tworzy `AttackMetadata` z `StatProvider` (`krytyk/jary/MythicLib-MMODamageMechanic.javap.txt`), natomiast MythicMobs `damage` wywołuje własny `DamagingMechanic.doDamage` bez tego kroku (`krytyk/jary/MythicMobs-DamagingMechanic.javap.txt`).

`b-2-08`, bazowy JAR `7f197e701920` z przywróconymi YAML `mmodamage`, początkowo dał tylko zapłon Płomieni i 0 HP Odmrożenia, bo nośnik w głównej dłoni uruchamiał filtr `KsiegaPostaci.Bron`; to nieważny kontrast. Poprawiona sonda `STAT2` ustawia normalny kamienny miecz w slocie 0 i czar w lewej dłoni. `b-2-09`, ten sam bazowy JAR i oryginalne YAML, dał:

| Czar | Wiara 0 / skill_damage 0% | Wiara 40 / skill_damage 60% | Stosunek szkody |
| --- | --- | --- | ---: |
| Płomienie | 20→12,127999 HP, 7,872001 szkody (4 ważne próby; 1 FAIL opóźnionego wyboru slotu) | 20→7,4048 HP, 12,5952 szkody (5/5) | 1,60× |
| Odmrożenie | 20→13,1119995 HP, 6,8880005 szkody (5/5) | 20→8,979198 HP, 11,020802 szkody (5/5) | 1,60× |

To jest dodatnia kontrola mechanizmu skalowania na bazie: Wiara działa przy `mmodamage`. `b-2-10`, najnowszy JAR A `dcf29adb5290` i nakładka `damage`, wykonał identyczne `STAT2`: Płomienie 8 obrażeń przy Wierze 0 i 40 (4/5 oraz 5/5 ważnych prób), Odmrożenie 7 obrażeń przy Wierze 0 i 40 (5/5 w obu seriach). Jedyny FAIL wynikał z opóźnionego wyboru kamiennego miecza w pierwszej próbie; odczyt HP nadal wynosił 8. **A/B potwierdza regresję skalowania 1,60× → 1,00×.** Dodatkowo `magic_damage`, element FIRE/ICE i obrona celu wymagają osobnych wariantów statystyki/celu, zanim potwierdzi się zachowanie wszystkich mnożników.
