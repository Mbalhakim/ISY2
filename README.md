
# Over het project
Dit project wordt uitgevoerd in opdracht van ISYGames, een fictieve opdrachtgever die nieuwe online bordspellen wil toevoegen aan hun platform.
We werken hier aan client/server game-applicatie waarin meerdere spellen gespeeld kunnen worden.

Het eindresultaat doet later mee in een toernooi waarin AI tegen AI speelt.

# 1. Doel van het spel
Aan het einde van het spel moet jij meer stenen van jouw kleur op het bord hebben dan de tegenstander.

# 2. Bord & Startpositie
Bord: 8 × 8 vakjes

Elke speler heeft schijven met 2 kleuren: zwart/wit

Startopstelling:

D4: wit

E5: wit

D5: zwart

E4: zwart

# 3. Beurtverloop
Zwart begint altijd
Een beurt = je plaatst één steen op een leeg vakje

# 4. Hoe werkt een geldige zet?
### Een zet is geldig als:

- Jij een steen plaatst
- Daardoor sluit je één of meerdere rijen van tegenstander-stenen in

- Insluiten = een rij waar:

- Jouw nieuwe steen aan het begin staat

- Aan het einde van die rij een steen van jouw kleur staat

- Daartussen alleen stenen van de tegenstander liggen

### Richtingen:

- horizontaal

- verticaal

- diagonaal

# Omgedraaide stenen

Alle stenen die ingesloten zijn, moeten worden omgedraaid naar jouw kleur.

# 6. Geen zet mogelijk?

- Als je geen geldige zet hebt → passen

- De beurt gaat naar de tegenstander

- Als beide spelers geen zet meer hebben → spel is voorbij

# 7. Einde van het spel

### Het spel stopt als:

- Het bord vol is

- OF beide spelers geen zet meer kunnen doen

**Dan wordt geteld**

- Aantal zwarte stenen

- Aantal witte stenen

-  De speler met de meeste wint.
