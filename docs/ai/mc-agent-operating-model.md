# MC Agent Operating Model

Ten dokument zamyka chaos wokol pytan:
- czym jest `Codex` / `Claude`
- czym jest `agent`
- czym jest `skill`
- co znaczy, ze agent "uczy sie" z corpusu
- jak to ma dzialac przy kilku profilach `codex1` / `codex2` / `codex3`

## Krotka wersja

- `Codex` albo `Claude` = silnik wykonawczy.
- `agent` = rola w pipeline, nie osobny magiczny mozg.
- `skill` = procedura pracy dla danej roli.
- `corpus` = material referencyjny.
- `learning` w tym repo = zamiana corpusu na jawne zasady, checklisty, antywzorce, baseline i artefakty review.
- To NIE jest fine-tuning wag modelu.

## Jedno zdanie, ktore ma zostac

Nie uczymy jednego Codexa "na stale" w jego wagach; budujemy wspolny system pracy, dzieki ktoremu kazdy Codex albo Claude moze wykonac dana role po tych samych zasadach.

## Co jest czym

### 1. Silnik wykonawczy

Silnikiem jest klient/model uruchamiany lokalnie:
- `codex1`
- `codex2`
- `codex3`
- `claude`

One roznia sie kontem, limitem, czasem predkoscia albo backendem, ale nie powinny roznic sie podstawowymi zasadami pracy.

### 2. Agent

Agent to rola wykonawcza w waskim kontrakcie, np.:
- `art`
- `mobs`
- `dungeon`
- `rpg`
- `qa`

To nie znaczy, ze istnieje jeden odrebny super-byt `art`. To znaczy tylko, ze dany run dostaje ograniczony scope, wejscia, wyjscia i odpowiedzialnosc.

### 3. Skill

Skill to instrukcja dla roli:
- co przeczytac najpierw
- czego nie ruszac
- jakie evidence zwrocic
- jakiego formatu raportu uzyc
- jakiej walidacji oczekiwac

Skill nie zastępuje agenta. Skill prowadzi agenta.

### 4. Corpus

Corpus to zrodlo referencyjne. Dla mobow/modeli glowny corpus jest tutaj:
- `/home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2)`

Corpus sluzy do:
- wyboru referencji
- wyciagania zasad
- budowy slownika animacji, helper bones, hitboxow i integracji
- antywzorcow i benchmarkow

Corpus NIE jest sam z siebie policy engine.

## Co znaczy, ze agent "uczy sie"

W tym repo slowo `learning` oznacza cos konkretnego:

1. agent czyta corpus i lokalne aktywne przyklady
2. agent wyciaga jawne zasady
3. agent zapisuje forbidden traits i anti-patterny
4. agent proponuje baseline
5. agent ocenia wynik rubricą
6. agent przechodzi dalej dopiero po artifact gate

To nie jest:
- trwałe retraining/fine-tuning modelu
- automatyczne zapamietanie wszystkiego po jednym pokazaniu folderu
- gwarancja, ze pusty prompt sam z siebie odtworzy house style

## Gdzie ta "nauka" jest zapisana naprawde

Nie w ukrytej pamieci modelu, tylko w repo-local artefaktach:

- `docs/ai/modelengine-learning-ledger.md`
- `docs/ai/modelengine-reference-corpus.md`
- `docs/ai/modelengine-creation-atlas.md`
- `docs/ai/mc-model-mob-learning-spec.md`
- `docs/ai/mc-model-mob-critique-rubric.md`
- `docs/ai/mc-model-mob-anti-pattern-catalog.md`
- `docs/ai/mc-mob-model-agent-pipeline.md`
- `MCMMORPG/_validation/work_packets/*.md`

To jest prawdziwa warstwa wiedzy dla kolejnych runow.

## Dlaczego nie jeden agent do wszystkiego

Jedna wielka komenda typu:
- zrob model
- zintegruj moba
- ustaw spawny
- dodaj rewardy
- zwaliduj wszystko

prawie zawsze miesza 5 roznych problemow naraz:
- reference study
- visual design
- model/rig/texture
- gameplay integration
- QA judgment

Efekt to zwykle:
- dryf jakosci
- gubienie baseline
- fake confidence
- brak jasnego winowajcy, gdy cos siadzie

Poprawny model to lane-based pipeline:
1. brief
2. art
3. mobs
4. optional dungeon/rpg
5. qa

## Jak to ma dzialac przy 3 Codexach

Wszystkie profile powinny miec ten sam operating layer:
- wspolne kanoniczne reguly w `/home/przemek/AGENTS.md` i `/home/przemek/SELF_IMPROVEMENT.md`
- wspolne repo docs i skills
- wspolny `profiles:sync` / `profiles:doctor`

Nie musza miec:
- tego samego konta
- tego samego limitu
- identycznego runtime costu
- identycznego backendowego zachowania

Parity oznacza:
- te same zasady pracy
- te same guardraile
- ten sam sposob raportowania
- ten sam sposob korzystania z corpusu i review gates

Parity NIE oznacza:
- identyczne konto
- identyczny billing
- identyczny usage window

## Source of truth dla MC mob/model work

Gdy task dotyczy nowego moba/modelu, source of truth jest następujacy:

1. lokalne aktywne pliki projektu (`MCMMORPG/...`)
2. exact `.bbmodel` i powiazane assety bezposrednio z `/home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2)`
3. corpus docs wygenerowane z `OUTPUT — kopia (2)` tylko jako indeks/pomoc do nawigacji, nie primary source
4. pipeline docs i skills
5. external vendors tylko jako principle references

Vendorzy sa po to, by pozyczyc zasady:
- Littleroom -> encounter completeness
- SamusDev -> role clarity
- Toro Toro -> silhouette pressure

Nigdy po to, by kopiowac 1:1 asset.

## Obowiazkowy model pracy dla nowego moba

1. read-only brief
2. extracted principles
3. forbidden traits
4. silhouette directions
5. read-only reference decomposition of bryla, landmarkow, joint-chain i motion constraints
6. neutral shell reconstruction
7. reference parity review
8. Layer A / Layer B split
   - Layer A = reference shell constraints, ktorych nie wolno zlamac
   - Layer B = nasza translacja klimatu, materialu i akcentow
9. accepted shell baseline
10. art/theme implementation
11. texture pass
12. critique rubric
13. anti-pattern check
14. mobs integration
15. qa verdict

Jesli po 2 iteracjach pass jest slabszy albo dalej chaotyczny, wracamy do silhouette/massing studies zamiast dalej mutowac model.

## Jak myslec o limitach

Jesli `codex1` albo `codex2` dostanie limit, to jest external blocker, a nie dowod, ze pipeline jest zly.

Wtedy:
- packet zostaje
- rules zostaja
- skill zostaje
- inny profil moze przejac te sama role
- praca nie przepada, bo wiedza siedzi w artefaktach

To jest wlasnie zaleta poprawnego pipeline'u.

## Minimalna regula operacyjna

Jesli jakikolwiek agent dostaje task typu "ucz sie z tych modeli i zrob nowego moba", to nie moze skoczyc od razu do geometrii. Najpierw musi zwrocic:
- 3+ lokalne referencje z exact path do realnych `.bbmodel` / assetow z corpusu
- co pozyczamy
- czego nie kopiujemy
- 5-10 explicit principles
- 3-5 forbidden traits
- 2-3 silhouette directions
- 1 recommended baseline
- exact structural landmarks, proportion logic, articulation chain i motion-critical separations
- rozbicie na:
  - co odtwarzamy 1:1 jako constraint creature-read
  - co tlumaczymy na nasz klimat jako original style layer

Jesli referencje sa tylko opisane przez derived docs, a nie otwarte jako realne pliki z corpusu, task jest zle wykonany juz na etapie briefu.

"Inspired by" bez tego splitu jest zle wykonanym briefem, nie briefem zaakceptowanym.

Dopiero potem wolno przejsc do implementacji.

## Powiazane pliki

- `docs/ai/how-to-use-mc-agents.md`
- `docs/ai/mc-mob-model-agent-pipeline.md`
- `docs/ai/mc-model-mob-learning-spec.md`
- `docs/ai/mc-model-mob-critique-rubric.md`
- `docs/ai/mc-model-mob-anti-pattern-catalog.md`
- `docs/ai/modelengine-learning-ledger.md`
- `docs/ai/modelengine-reference-corpus.md`
- `docs/ai/modelengine-creation-atlas.md`
