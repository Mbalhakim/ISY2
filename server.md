# Reversi Game Server – How It Works & Protocol

This document explains **how the Reversi (Othello) game server works**, how to start it, and **which commands you can expect** as a client.

> ⚠️ **Important**
> Command names, keywords, and protocol messages are **NOT changed**. Only explanations are translated and clarified.

---

## 1. Game Server Overview

The game server is a Java-based server that supports multiple turn-based games, including:

* **Reversi (Othello)**
* **Tic-Tac-Toe**

The server handles:

* Player connections
* Matchmaking
* Turn handling
* Game state updates
* Match results

---

## 2. Installation & Startup

1. Download the game server zip:

   ```text
   newgameserver-release-V1.zip
   ```

2. Extract the zip into a logical directory on your system.

3. Start the server from the command line:

   ```bash
   java -jar newgamesver-release-V1.jar
   ```

### Server Details

* **Game server port:** `7789`
* **Web interface:** [http://127.0.0.1:8081](http://127.0.0.1:8081)

---

## 3. Protocol Basics

This server uses the **ISY-Games protocol**.

### General Rules

* All **client commands are case-insensitive**
* All **arguments are case-insensitive**
* The server may send messages **at any time**
* The server may **close the connection** without warning

### Notation

* `[ ... ]` → List
* `{ ... }` → Map / key-value structure (order not guaranteed)

---

## 4. Error Handling

### Unsupported Command

**Client**

```text
<unknown command>
```

**Server**

```text
ERR <reason>
```

* No further effect

---

## 5. Authentication

### Login

**Client**

```text
login <speler>
```

**Server**

```text
OK
```

or

```text
ERR <reden>
```

* Player name is converted to **lowercase**
* Player names must be **unique**
* If a player reconnects with the same name, they rejoin the tournament

---

### Logout / Disconnect

**Client**

```text
logout | exit | quit | disconnect | bye
```

**Server**

```text
-
```

* Connection is closed
* Player remains registered and can reconnect

---

## 6. Information Requests

### Get Supported Games

**Client**

```text
get gamelist
```

**Server**

```text
OK
SVR GAMELIST ["<speltype>", ...]
```

---

### Get Connected Players

**Client**

```text
get playerlist
```

**Server**

```text
OK
SVR PLAYERLIST ["<speler>", ...]
```

---

## 7. Matchmaking (Automatic)

### Subscribe to a Game Type

**Client**

```text
subscribe <speltype>
```

**Server**

```text
OK
```

* When another player subscribes to the same game type, a match starts automatically

---

### Match Found

**Server → Both Players**

```text
SVR GAME MATCH {
  PLAYERTOMOVE: "<naam speler1>",
  GAMETYPE: "<speltype>",
  OPPONENT: "<naam tegenstander>"
}
```

* Match has started
* Subscription is cancelled

---

## 8. Turn Handling

### Your Turn

**Server**

```text
SVR GAME YOURTURN {TURNMESSAGE: "<bericht>"}
```

* Client is now allowed to make a move

---

### Making a Move

#### For Placement Games (Reversi, Tic-Tac-Toe)

**Client**

```text
choose <move>
```

**Server**

```text
OK
```

#### Move Ranges

* **Tic-Tac-Toe:** `0 – 8`
* **Reversi (Othello):** `0 – 63`

Board numbering is **row-wise**, from top-left to bottom-right.

---

#### For Movement Games (Chess, Stratego, etc.)

**Client**

```text
move <from> <to>
```

**Server**

```text
OK
```

---

### Move Result

**Server → Both Players**

```text
SVR GAME MOVE {
  PLAYER: "<speler>",
  MOVE: "<zet>",
  DETAILS: "<reactie spel op zet>"
}
```

---

## 9. Match Results

### Normal End

**Server**

```text
SVR GAME <WIN | LOSS | DRAW> {
  PLAYERONESCORE: "<score speler1>",
  PLAYERTWOSCORE: "<score speler2>",
  COMMENT: "<commentaar>"
}
```

---

### Forfeit (Give Up)

**Client**

```text
forfeit
```

**Server**

```text
OK
```

**Server → Both Players**

```text
SVR GAME <WIN | LOSS> {
  PLAYERONESCORE: "<score speler1>",
  PLAYERTWOSCORE: "<score speler2>",
  COMMENT: "Player forfeited match"
}
```

---

### Disconnect During Match

**Server**

```text
SVR GAME <WIN | LOSS> {
  PLAYERONESCORE: "<score speler1>",
  PLAYERTWOSCORE: "<score speler2>",
  COMMENT: "Client disconnected"
}
```

---

## 10. Challenges (Direct Match)

### Challenge a Player

**Client**

```text
challenge <speler> <speltype>
```

or

```text
challenge "<speler>" "<speltype>"
```

**Server**

```text
OK
```

---

### Receive Challenge

**Server**

```text
SVR GAME CHALLENGE {
  CHALLENGER: "Sjors",
  GAMETYPE: "Guess Game",
  CHALLENGENUMBER: "1"
}
```

---

### Accept Challenge

**Client**

```text
challenge accept <uitdaging nummer>
```

**Server**

```text
OK
```

---

### Challenge Cancelled

**Server**

```text
SVR GAME CHALLENGE CANCELLED {CHALLENGENUMBER: "<nummer>"}
```

---

## 11. Chat (Optional)

**Client**

```text
message <bericht>
message "<bericht met spaties>"
```

**Server**

```text
OK
```

or

```text
ERR <reden>
```

* Only works if chat is enabled by the admin

---

## 12. Help System

### General Help

**Client**

```text
help
```

**Server**

```text
OK
SVR HELP <help informatie>
```

---

### Command-Specific Help

**Client**

```text
help <commando>
```

---

## 13. Server Message Overview

```text
OK
ERR
SVR HELP
SVR GAME MATCH
SVR GAME YOURTURN
SVR GAME MOVE
SVR GAME CHALLENGE
SVR GAME WIN
SVR GAME LOSS
SVR GAME DRAW
```

---

## 14. Client Command Overview

```text
login
logout | exit | quit | disconnect | bye
get gamelist
get playerlist
subscribe
choose       (for placement games: Reversi, Tic-Tac-Toe)
move         (for movement games: Chess, Stratego)
challenge
challenge accept
forfeit
message
help
```

---

## 15. Reversi-Specific Notes

* Board size: **8×8**
* Moves range from `0` to `63`
* **Use `choose` command** (not `move`)
* Server validates legal moves
* Turn order is enforced strictly

**Example:**
```text
Client: choose 19
Server: OK
Server: SVR GAME MOVE {PLAYER: "username", MOVE: "19", DETAILS: ""}
```

---

✅ This document can be used directly as a **Markdown (.md)** file for documentation, reports, or projects.
