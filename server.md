
# New Game Server – Protocol Documentatie (2021)

**Auteurs:**
- Hilko Janssen
- Remi Reuvekamp
- Gertjan Haan
- Timo Strating

**Onder begeleiding van:** Bas Heijne  
**In opdracht van:** Lech Bialek

---

## ⚠️ Belangrijke Opmerking

Dit project is een **2de iteratie** van een oudere server.

De oude server:
- Hanteerde het protocol **zeer strikt**
- Verstuurde soms berichten in een andere volgorde dan gedocumenteerd

De nieuwe server:
- Staat een **vrijere implementatie** toe
- Begrijpt commando’s **met of zonder quotes** rond argumenten
- Is **volledig backwards compatible** met de oude, strikte versie

---

## Commando’s in Detail

**C** = Client  
**S** = Server

### Notities bij server-antwoorden
- `[ ... ]` → lijst
- `{ ... }` → map (volgorde is niet vast)

### Notities bij client-commando’s
- Commando’s zijn **niet hoofdlettergevoelig**
- Argumenten zijn **niet hoofdlettergevoelig**

⚠️ De server kan:
- Onverwachte server-berichten sturen
- De verbinding verbreken

⚠️ Commando’s kunnen door een admin worden uitgeschakeld  
→ Je krijgt dan `ERR` i.p.v. `OK`

---

## Niet-ondersteund Commando


C: <niet ondersteund commando> | <commando> <verkeerd(e) argument(en)>
S: ERR <reden>
→ Geen gevolg

----------

## Inloggen
C: login <speler>
S: OK | ERR <reden>

-   Je bent ingelogd met een **lowercase unieke spelersnaam**

-   Bestond de speler al (bijv. disconnect tijdens toernooi), dan word je opnieuw toegevoegd


----------

## Uitloggen / Verbinding verbreken
C: logout | exit | quit | disconnect | bye
S: -   Verbinding wordt verbroken
-    Speler blijft in het toernooi en kan later opnieuw inloggen
-
## Ondersteunde Spellen Opvragen

C: get gamelist
S: OK
S: SVR GAMELIST ["<speltype>", ...]

----------

## Verbonden Spelers Opvragen
C: get playerlist
S: OK
S: SVR PLAYERLIST ["<speler>", ...]

## Inschrijven voor een Match (Spelzoeker)

`C: subscribe <speltype>
S: OK | ERR <reden>`

-   Match start zodra een tweede speler zich inschrijft

-   Kan uitgeschakeld zijn door admin


----------

## Match Start (Beide Spelers)

`S: SVR GAME MATCH {
  PLAYERTOMOVE: "<naam speler1>",
  GAMETYPE: "<speltype>",
  OPPONENT: "<naam tegenstander>"
}`

----------

## Beurt Ontvangen

`S: SVR GAME YOURTURN {
  TURNMESSAGE: "<bericht>"
}`

----------

## Zet Doen

`C: move <zet>
S: OK`

----------

## Resultaat van een Zet

`S: SVR GAME MOVE {
  PLAYER: "<speler>",
  DETAILS: "<reactie>",
  MOVE: "<zet>"
}`

----------

## Resultaat van een Match

`S: SVR GAME <WIN | LOSS | DRAW> {
  PLAYERONESCORE: "<score speler1>",
  PLAYERTWOSCORE: "<score speler2>",
  COMMENT: "<commentaar>"
}`

----------

## Match Opgeven

`C: forfeit
S: OK`

----------

## Match Resultaat na Opgeven

`S: SVR GAME <WIN | LOSS | DRAW> {
  PLAYERONESCORE: "<score speler1>",
  PLAYERTWOSCORE: "<score speler2>",
  COMMENT: "Player forfeited match"
}`

----------

## Match Resultaat bij Disconnect

`S: SVR GAME <WIN | LOSS | DRAW> {
  PLAYERONESCORE: "<score speler1>",
  PLAYERTWOSCORE: "<score speler2>",
  COMMENT: "Client disconnected"
}`

----------

## Speler Uitdagen

`C: challenge "<speler>" "<speltype>"
C: challenge <speler> <speltype>
S: OK | ERR <reden>`

-   Quotes verplicht bij spaties

-   Oude uitdagingen vervallen automatisch

-   Kan uitgeschakeld zijn door admin


----------

## Uitdaging Ontvangen

`S: SVR GAME CHALLENGE {
  CHALLENGER: "Sjors",
  GAMETYPE: "Guess Game",
  CHALLENGENUMBER: "1"
}`

----------

## Uitdaging Vervallen

`S: SVR GAME CHALLENGE CANCELLED {
  CHALLENGENUMBER: "<nummer>"
}`

Mogelijke oorzaken:

-   Andere uitdaging gestart

-   Match begonnen

-   Disconnect


----------

## Uitdaging Accepteren

`C: challenge accept <uitdaging nummer>
S: OK | ERR <reden>`

----------

## Chatten

`C: message <bericht>
C: message "<bericht met spaties>"
S: OK | ERR <reden>`

-   Alleen mogelijk als chat is ingeschakeld


----------

## Help

### Algemene Help

`C: help
S: OK`

### Help voor Specifiek Commando

`C: help <commando>
S: OK`

### Help Antwoord

`S: SVR HELP <help informatie>`

----------

## Overzicht Server-Berichten

Bericht

Betekenis

OK

Commando geaccepteerd

ERR

Commando afgewezen

SVR HELP

Help informatie

SVR GAME MATCH

Match gestart

SVR GAME YOURTURN

Beurt toegewezen

SVR GAME MOVE

Zet uitgevoerd

SVR GAME CHALLENGE

Uitdaging

SVR GAME WIN

Gewonnen

SVR GAME LOSS

Verloren

SVR GAME DRAW

Gelijkspel

----------

## Overzicht Client-Commando’s

-   `login`

-   `logout | exit | quit | disconnect | bye`

-   `get gamelist`

-   `get playerlist`

-   `subscribe`

-   `move`

-   `challenge [accept]`

-   `forfeit`

-   `message`

-   `help [commando]`


----------

## 💡 Tips & Tricks (Winnaars 2019)

1.  Lees de wiki van **Othello/Reversi**

2.  Hardcode copyright **niet**

3.  Server-instellingen kunnen veranderen

4.  Test met een klasgenoot vóór het toernooi

5.  Sommige spellen vereisen een gepasseerde beurt

6.  Spam de server niet

7.  Oude clients hadden soms gevaarlijke popups

8.  Iedereen wordt automatisch in het toernooi gezet

9.  **Test. Test. Test.**

10.  De schrijver won zonder deze tips — gebruik ze 