# US 10,926,179 B2 — zastrzeżenia (tekst oryginalny, angielski)

Źródło: Google Patents, https://patents.google.com/patent/US10926179B2/en (pobrane 2026-10-25, `curl`, sekcja `itemprop="claims"`).
PDF urzędowy: https://patentimages.storage.googleapis.com/45/aa/b5/7d07e883c25b26/US10926179.pdf (nie porównany słowo w słowo z HTML).
Do potwierdzenia przez prawnika w USPTO Patent Center / ppubs.uspto.gov (strona USPTO wymaga JS, nie pobrana).

## Dane (Google Patents, 2026-10-25)

- Tytuł: Nemesis characters, nemesis forts, social vendettas and followers in computer games
- Właściciel (assignee wg Google): Warner Bros Entertainment Inc
- Twórcy: Michael de Plater, Christopher Herman Hoge, Robert Kenyon Hull Roberts, Daniel Paul Valerius, Rocky Albert Newton, Kevin Leslie Stephens
- Zgłoszenie US15/081,732 z 2016-03-25; pierwszeństwo 2015-03-26 (prowizoryczne 62/138,920) i 2016-03-04 (62/303,738)
- Udzielony 2021-02-23. Status wg Google: Active. Przewidywany koniec (wg Google, „Adjusted expiration”): 2036-08-11
- Liczba zastrzeżeń: 36; niezależne: **1** (sposób), **19** (urządzenie: procesor + pamięć + wyświetlacz), **36** (urządzenie w formule „means for”)
- Rodzina (wg Google, tylko US): US11660540B2 (kontynuacja, udz. 2023-05-30), US12201908B2 (udz. 2025-01-21), US20250108303A1 (zgłoszenie, publ. 2025-04-03, status wg Google: Pending). Innych krajów Google nie pokazuje — do sprawdzenia w Espacenet/INPADOC.

## Zastrzeżenia niezależne

### Zastrz. 1

> 1. A method comprising: controlling, by a processor, game events in a computer-implemented game, the game events involving an avatar that is operated in response to input from a player, and a first non-player character that is controlled by the processor to respond to and automatically oppose avatars based on first character parameters defined in a computer memory; detecting, by the processor, occurrence of a predefined one of the game events involving an interaction between the avatar and the first non-player character; changing, by the processor, second character parameters defined in at least one of the computer memory or a second computer memory for control of a second non-player character in the game based on the detecting, wherein the second non-player character is controlled by the processor to respond to and automatically oppose avatars based on the second character parameters defined in the at least one of the computer memory or the second computer memory; and outputting, to an output device, an indication of the second character parameters that are changed by the changing.

### Zastrz. 19

> 19. An apparatus for controlling a video game, comprising: a processor, a memory coupled to the processor, and a display device coupled to the processor, wherein the memory holds instructions that when executed by the processor, cause the apparatus perform: controlling game events in a computer-implemented game, the game events involving an avatar that is operated in response to input from a player, and a first non-player character that is controlled by the processor to respond to and automatically oppose avatars based on a first character parameters defined in a computer memory; detecting occurrence of a predefined one of the game events involving an interaction between the avatar and the first non-player character; changing second character parameters defined in at least one of the computer memory or a second computer memory for control of a second non-player character in the game based on the detecting, wherein the second non-player character is controlled by the processor to respond to and automatically oppose avatars based on the second character parameters defined in the at least one of the computer memory or the second computer memory; and outputting, to an output device, an indication of the second character parameters that are changed by the changing.

### Zastrz. 36

> 36. A video game apparatus, comprising: means for controlling game events in a computer-implemented game, the game events involving an avatar that is operated in response to input from a player, and a first non-player character that is controlled by the processor to respond to and automatically oppose avatars based on first character parameters defined in a computer memory; means for detecting occurrence of a predefined one of the game events involving an interaction between the avatar and the first non-player character; means for changing second character parameters defined in at least one of the computer memory or a second computer memory for control of a second non-player character in the game based on the detecting, wherein the second non-player character is controlled by the processor to respond to avatars and automatically oppose based on the second character parameters defined in the at least one of the computer memory or the second computer memory; and means for outputting, to an output device, an indication of the second character parameters that are changed by the changing.

## Zastrzeżenia zależne istotne dla naszej mechaniki

Pełna lista 2–18 (od 1) i 20–35 (od 19) jest na stronie źródłowej. Tu te, które dotyczą śmierci, frakcji/hierarchii, przekazu do innej instancji i rodzaju zmienianych parametrów.

### Zastrz. 2

> 2. The method of claim 1 , further comprising triggering initiation of the changing based on occurrence of the predefined one of the game events selected from the group consisting of the avatar death or entry of the avatar into a new zone of play.

### Zastrz. 3

> 3. The method of claim 1 , wherein the changing the second character parameters further comprises determining a change of status of the second non-player character in a faction of non-player characters, based at least in part on the one of the game events.

### Zastrz. 4

> 4. The method of claim 3 , wherein the faction is a ranked hierarchy of the non-player characters in which ranks are positively correlated with game playing power, and changing the second character parameters comprises at least one of: changing a rank of the second non-player character in the hierarchy, or designating the second non-player character one of a ranked member of the hierarchy, or a non-member of the ranked hierarchy.

### Zastrz. 5

> 5. The method of claim 4 , further comprising determining, based on at least one of a game event or a user selection, to provide data characterizing the faction configured for use in an independently operating game instance to a remote server.

### Zastrz. 6

> 6. The method of claim 5 , further comprising providing the data characterizing the faction to a second processor for use in the independently operating game instance operated by another user.

### Zastrz. 7

> 7. The method of claim 6 , further comprising tracking whether a highest-ranking member of the faction is defeated by the another user in the independently operating game instance.

### Zastrz. 8

> 8. The method of claim 7 , further comprising sending data providing an in-game benefit to the independently operating game instance, based on the tracking.

### Zastrz. 9

> 9. The method of claim 7 , further comprising sending notice data to the processor executing the computer-implemented game, indicating that the another user has defeated the highest-ranking member.

### Zastrz. 11

> 11. The method of claim 1 , wherein the changing the second character parameters comprises at least one of: changing a parameter controlling a rendered appearance of the second non-player character, changing a parameter controlling a behavioral personality trait of the second non-player character, changing a parameter controlling a game-playing ability of the second non-player character, changing a player interaction score that increases a probability that the second non-player character will succeed in future interaction with the player's avatar, or changing a power center attribute associated with the second character parameters.

### Zastrz. 13

> 13. The method of claim 1 , further comprising selecting dialog for at least one of the first or second non-player character, based on the detecting the occurrence of the predefined one of the game events.

### Zastrz. 14

> 14. The method of claim 1 , further comprising changing, by the processor, the first character parameters based on the detecting; and outputting, to at least one of the output device or to a second output device, an indication of the first character parameters that are changed by the changing the first character parameters.

Odpowiedniki w grupie urządzenia (od 19): 20 (=2), 21 (=3), 22 (=4), 24–28 (=5–9), 29 (=11), 31 (=13), 32 (=14).

## Rodzina: zastrzeżenia niezależne (Google Patents, pobrane 2026-10-25)

### US11660540B2 — https://patents.google.com/patent/US11660540B2/en

**Zastrz. 1**

> 1. A method comprising: controlling, by at least one processor executing a computer game, one or more interactions between an avatar operated in response to input from a player and a first non-player character controlled by the at least one processor to respond to and automatically oppose the avatar based on first character parameters defined in a computer memory; detecting, by the at least one processor, one or more outcomes of the interactions meeting a pre-defined condition; changing, by the at least one processor, second character parameters defined in at least one of the computer memory or a second computer memory for control of a second non-player character based on the detecting, wherein the second non-player character is controlled by the processor to respond to and automatically oppose the avatar based on the second character parameters defined in the at least one of the computer memory or the second computer memory; and controlling the second non-player character based on the second character parameters that are changed by the changing.

**Zastrz. 14**

> 14. An apparatus for controlling a video game, comprising: at least one processor, a memory and a display device coupled to the at least one processor, wherein the memory holds instructions that when executed by the processor, cause the apparatus perform: controlling in a computer-implemented game one or more interactions between an avatar operated in response to input from a player, and a first non-player character that is controlled by the at least one processor to respond to and automatically oppose the avatar based on first character parameters defined in a computer memory; detecting one or more outcomes of the interactions satisfying a pre-defined condition; changing second character parameters defined in at least one of the computer memory or a second computer memory for control of a second non-player character based on the detecting, wherein the second non-player character is controlled by the processor to respond to and automatically oppose the avatar based on the second character parameters defined in the at least one of the computer memory or the second computer memory; and controlling the second non-player character based on the second character parameters that are changed by the changing.

### US12201908B2 — https://patents.google.com/patent/US12201908B2/en

**Zastrz. 1**

> 1. A method comprising: determining, by at least one processor executing a computer game, an outcome of an interaction between an avatar operated in response to input from a player and at least one non-player character member of a faction of non-player characters, wherein the faction comprises a hierarchy of non-player characters and the at least one non-player character is controlled by the at least one processor to respond to and automatically interact with the avatar based at least in part on status of the at least one non-player character in the hierarchy of the faction; changing, by the at least one processor, the status of the at least one non-player character in the faction based on the outcome, wherein changing the status affects control parameters for at least one second non-player character in the faction; and controlling the second non-player character based on the control parameters after the changing.

**Zastrz. 13**

> 13. An apparatus for controlling a video game, comprising: at least one processor, a memory and a display device coupled to the at least one processor, wherein the memory holds instructions that when executed by the processor, cause the apparatus to perform: determining for the video game an outcome of an interaction between an avatar operated in response to input from a player and at least one non-player character member of a faction of non-player characters, wherein the faction comprises a hierarchy of non-player characters and the at least one non-player character is controlled by the at least one processor to respond to and automatically interact with the avatar based at least in part on status of the at least one non-player character in the hierarchy of the faction; changing the status of the at least one non-player character in the faction based on the outcome, wherein changing the status affects control parameters for at least one second non-player character in the faction; and controlling the second non-player character based on the control parameters after the changing.

**Zastrz. 20**

> 20. A non-transitory computer readable medium holding program instructions that when executed by at least one processor cause an apparatus to perform: determining for a video game an outcome of an interaction between an avatar operated in response to input from a player and at least one non-player character member of a faction of non-player characters, wherein the faction comprises a hierarchy of non-player characters and the at least one non-player character is controlled by the at least one processor to respond to and automatically interact with the avatar based at least in part on status of the at least one non-player character in the hierarchy of the faction; changing the status of the at least one non-player character in the faction based on the outcome, wherein changing the status affects control parameters for at least one second non-player character in the faction; and controlling the second non-player character based on the control parameters after the changing.

### US20250108303A1 — https://patents.google.com/patent/US20250108303A1/en

**Zastrz. 37**

> 37 . A computer-implemented method for controlling a video game, the computer-implemented method comprising: receiving, by at least one processor executing a computer game, input from a player to operate an avatar to interact with at least one non-player character, wherein the at least one non-player character is controlled by the at least one processor to respond to and automatically interact with the avatar; determining, by the at least one processor, an outcome of the interaction between the avatar and the at least one non-player character; changing, by the at least one processor, a status of the at least one non-player character based on the outcome, wherein changing the status affects one or more control parameters for at least one second non-player character; and controlling, by the at least one processor, the at least one second non-player character based on the one or more control parameters after the changing.

**Zastrz. 44**

> 44 . An apparatus for controlling a video game, comprising: at least one processor, a memory and a display device coupled to the at least one processor, wherein the memory holds instructions that when executed by the at least one processor, cause the apparatus to perform: receiving input from a player to operate an avatar to interact with at least one non-player character, wherein the at least one non-player character is controlled by the at least one processor to respond to and automatically interact with the avatar; determining an outcome of the interaction between the avatar and the at least one non-player character; changing a status of the at least one non-player character based on the outcome, wherein changing the status affects one or more control parameters for at least one second non-player character; and controlling the at least one second non-player character based on the one or more control parameters after the changing.

**Zastrz. 51**

> 51 . A non-transitory computer readable medium holding program instructions that when executed by at least one processor cause an apparatus to perform: receiving input from a player to operate an avatar to interact with at least one non-player character, wherein the at least one non-player character is controlled by the at least one processor to respond to and automatically interact with the avatar; determining an outcome of the interaction between the avatar and the at least one non-player character; changing a status of the at least one non-player character based on the outcome, wherein changing the status affects one or more control parameters for at least one second non-player character; and controlling the at least one second non-player character based on the one or more control parameters after the changing.

Uwaga: US20250108303A1 to opublikowane ZGŁOSZENIE — zastrzeżenia mogą się zmienić przed udzieleniem.
