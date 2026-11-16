# Serwery Docker

`start.sh` jest wylaczony (`start.sh.disabled`). Uruchamianie runtime odbywa sie przez Docker + Velocity.

```bash
cd docker
./mc up world
./mc up items
./mc up hub
./mc down <dom>
./mc rcon world "md list"
./mc ps
./mc stats
./mc logs
```

Klient laczy sie przez Velocity: `localhost:25565`.

Backendy funkcjonalne:

- `world`: lochy, swiat, MythicDungeons, moby, modele.
- `items`: itemy, ekwipunek, klasy, login/profil postaci.
- `hub`: social/hub.

Nie odpalaj naraz wiecej ciezkich backendow niz trzeba; host potrafi dobic do OOM przy okolo 7 GB.

## QA Pluginow I Profile SLO

- Nie uruchamiaj profili botow dla roznych pluginow na tym samym backendzie.
- `world`, `items` i `hub` sa backendami funkcjonalnymi, nie miejscem na rownolegle testy obciazeniowe wielu systemow.
- Dla pluginu utworz osobny backend QA z minimalnym zestawem zaleznosci i osobnym katalogiem `/data`.
- Dopiero na backendzie QA zmieniaj `max-players`, throttle, `test-profile.yml`, limity i TTL.
- Przed testem potwierdz: brak cudzych botow/procesow, 0 graczy, znany rollback.
- Po tescie zatrzymaj backend QA i usun tymczasowe profile oraz zasoby.
