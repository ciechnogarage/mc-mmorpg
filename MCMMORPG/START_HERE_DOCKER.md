# Serwery przeniesione na Docker

`start.sh` wyłączony (`start.sh.disabled`). Uruchamianie przez Docker + Velocity:

    cd docker
    ./mc up world      # loch/build/mythicdungeons (Xmx 4G)   :25565 przez proxy
    ./mc up items      # itemy/eq/klasy/login (Xmx 2G)
    ./mc up hub        # social/hub (Xmx 1.5G)
    ./mc down <dom>    # zgaś backend (velocity zostaje)
    ./mc rcon world "md list"
    ./mc ps / ./mc stats / ./mc logs world

Wejście dla klienta: localhost:25565 (Velocity kieruje na aktywny backend).
UWAGA: nie odpalaj naraz live-servera i ciężkiego backendu — host OOM-uje przy ~7G.
