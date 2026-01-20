package controller;

import core.Game;
import enums.Color;
import interfaces.Strategy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import network.ServerClient;
import network.ServerListener;
import network.ServerMessage;
import view.BoardView;
import view.GameView;

public class OnlineGameController {
    private ServerClient client;
    private BoardView boardView;
    private GameView gameView;
    private Game game;
    private Strategy strategy;
    private String playerName;
    private String opponentName;
    private Color myColor;
    private String firstPlayerName;
    private boolean isMyTurn;
    private boolean gameActive;
    private boolean inMatch = false;
    private boolean myTurn = false;

    // Single input thread routing (prevents lobby/menu from stealing move input)
    private enum LobbyState { MAIN, CHALLENGE_PICK, PENDING_DECISION }
    private volatile LobbyState lobbyState = LobbyState.MAIN;
    private volatile java.util.List<String> lobbyCandidates = new java.util.ArrayList<>();

    // Current turn context
    private volatile java.util.List<int[]> currentLegalMoves = java.util.Collections.emptyList();
    private final java.util.concurrent.atomic.AtomicBoolean autoMoveMade = new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile java.util.Timer turnTimer;

    // Lobby state
    private volatile java.util.List<String> lastPlayerList = new java.util.ArrayList<>();
    private final java.util.Map<String, PendingChallenge> pendingChallenges = new java.util.concurrent.ConcurrentHashMap<>();

    private static class PendingChallenge {
        final String number;
        final String challenger;
        final String gameType;

        PendingChallenge(String number, String challenger, String gameType) {
            this.number = number;
            this.challenger = challenger;
            this.gameType = gameType;
        }
    }

    public OnlineGameController(Game game, BoardView boardView, GameView gameView, Strategy strategy) {
        this.game = game;
        this.boardView = boardView;
        this.gameView = gameView;
        this.strategy = strategy;
        this.client = new ServerClient();
        this.isMyTurn = false;
        this.gameActive = true;
    }

    public void play() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            // Setup listener
            client.addListener(new ServerListener() {
                @Override
                public void onMessage(String message) {
                    handleServerMessage(message);
                }

                @Override
                public void onError(Exception e) {
                    System.out.println("[FOUT] " + e.getMessage());
                }

                @Override
                public void onClose() {
                    System.out.println("[VERBINDING GESLOTEN]");
                    gameActive = false;
                }
            });

            // Connect
            System.out.println("Verbinden met gameserver op localhost:7789...");
            client.connect("127.0.0.1", 7789);
            Thread.sleep(500);

            System.out.println("\n" + "=".repeat(60));
            System.out.println("BELANGRIJK: Elke beurt heeft een tijdslimiet van ~10 seconden!");
            System.out.println("   Wees klaar om snel te spelen als het jouw beurt is.");
            System.out.println("=".repeat(60) + "\n");

            // Login
            System.out.print("Voer je spelersnaam in: ");
            playerName = br.readLine();
            client.send("login " + playerName);
            Thread.sleep(500);

            // Show lobby + start single input loop
            printLobbyMenu();
            client.send("get playerlist");

            Thread inputThread = new Thread(() -> inputLoop(br), "online-input-loop");
            inputThread.setDaemon(true);
            inputThread.start();

            while (gameActive && client.isConnected()) {
                Thread.sleep(100);
            }

            System.out.println("\nSpel afgelopen. Druk op Enter om af te sluiten...");
            br.readLine();

        } catch (Exception e) {
            System.out.println("Fout: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.close();
        }
    }

    private void inputLoop(BufferedReader br) {
        try {
            while (gameActive && client.isConnected()) {
                String line = br.readLine();
                if (line == null) return;
                line = line.trim();
                if (line.isEmpty()) continue;

                // Route based on current state
                if (!inMatch) {
                    handleLobbyInput(line);
                } else if (isMyTurn) {
                    handleMoveInput(line);
                } else {
                    // In a match, but not your turn (or AI turn). Ignore menu input.
                    System.out.println("(Wachten op tegenstander...)");
                }
            }
        } catch (Exception e) {
            if (gameActive) {
                System.out.println("[FOUT] Input loop gestopt: " + e.getMessage());
            }
        }
    }

    private void handleServerMessage(String rawMessage) {
        System.out.println("[SERVER] " + rawMessage);
        ServerMessage msg = new ServerMessage(rawMessage);

        switch (msg.getType()) {
            case "MATCH":
                handleMatch(msg);
                break;
            case "CHALLENGE":
                handleChallenge(msg);
                break;
            case "CHALLENGE_CANCELLED":
                handleChallengeCancelled(msg);
                break;
            case "PLAYERLIST":
                handlePlayerList(msg);
                break;
            case "YOURTURN":
                handleYourTurn(msg);
                break;
            case "MOVE":
                handleMove(msg);
                break;
            case "WIN":
                handleGameEnd("WIN", msg);
                break;
            case "LOSS":
                handleGameEnd("LOSS", msg);
                break;
            case "DRAW":
                handleGameEnd("DRAW", msg);
                break;
        }
    }

    private void printLobbyMenu() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ONLINE LOBBY");
        System.out.println("=".repeat(60));
        System.out.println("Je kunt:\n" +
                "  1) Matchmaking \n" +
                "  2) Een speler uitdagen \n" +
                "  3) Openstaande uitdagingen bekijken\n" +
                "  4) Playerlist opnieuw ophalen\n" +
                "  5) Afsluiten\n");
        System.out.print("Kies optie (1-5): ");
    }

    private void handleLobbyInput(String line) {
        try {
            switch (lobbyState) {
                case MAIN:
                    handleLobbyMain(line);
                    break;
                case CHALLENGE_PICK:
                    handleLobbyChallengePick(line);
                    break;
                case PENDING_DECISION:
                    handleLobbyPendingDecision(line);
                    break;
            }
        } catch (Exception e) {
            System.out.println("[FOUT] " + e.getMessage());
        }
    }

    private void handleLobbyMain(String choice) throws Exception {
        switch (choice) {
            case "1":
                System.out.println("Inschrijven voor Othello (reversi-8)...");
                client.send("subscribe reversi-8");
                System.out.println("Wachten op tegenstander...");
                return;
            case "2":
                client.send("get playerlist");
                Thread.sleep(250);
                lobbyCandidates = new java.util.ArrayList<>();
                for (String p : lastPlayerList) {
                    if (p == null) continue;
                    if (!p.equalsIgnoreCase(playerName)) lobbyCandidates.add(p);
                }
                if (lobbyCandidates.isEmpty()) {
                    System.out.println("Geen andere spelers online om uit te dagen.");
                    printLobbyMenu();
                    return;
                }
                System.out.println("\nBeschikbare spelers:");
                for (int i = 0; i < lobbyCandidates.size(); i++) {
                    System.out.println("  " + (i + 1) + ". " + lobbyCandidates.get(i));
                }
                System.out.print("Kies speler nummer (of Enter om te annuleren): ");
                lobbyState = LobbyState.CHALLENGE_PICK;
                return;
            case "3":
                if (pendingChallenges.isEmpty()) {
                    System.out.println("Geen openstaande uitdagingen.");
                    printLobbyMenu();
                    return;
                }
                System.out.println("\nOpenstaande uitdagingen:");
                java.util.List<PendingChallenge> list = new java.util.ArrayList<>(pendingChallenges.values());
                list.sort(java.util.Comparator.comparing(pc -> pc.number));
                for (PendingChallenge pc : list) {
                    System.out.println("  #" + pc.number + " van " + pc.challenger + " (" + pc.gameType + ")");
                }
                System.out.print("Typ 'a <nr>' om te accepteren of 'n <nr>' om af te wijzen (Enter=terug): ");
                lobbyState = LobbyState.PENDING_DECISION;
                return;
            case "4":
                client.send("get playerlist");
                Thread.sleep(250);
                printPlayerList();
                printLobbyMenu();
                return;
            case "5":
                gameActive = false;
                client.send("logout");
                return;
            default:
                System.out.println("Ongeldige keuze.");
                printLobbyMenu();
        }
    }

    private void handleLobbyChallengePick(String input) throws Exception {
        // Enter cancels
        if (input == null || input.trim().isEmpty()) {
            lobbyState = LobbyState.MAIN;
            printLobbyMenu();
            return;
        }
        int idx;
        try {
            idx = Integer.parseInt(input.trim()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Ongeldige invoer.");
            System.out.print("Kies speler nummer (of Enter om te annuleren): ");
            return;
        }
        if (idx < 0 || idx >= lobbyCandidates.size()) {
            System.out.println("Nummer buiten bereik.");
            System.out.print("Kies speler nummer (of Enter om te annuleren): ");
            return;
        }

        String target = lobbyCandidates.get(idx);
        String gameType = "reversi-8";
        System.out.println("Uitdaging versturen naar '" + target + "' voor " + gameType + "...");
        client.send("challenge " + target + " " + gameType);
        System.out.println("Uitdaging verstuurd. Wacht op acceptatie.\n");

        lobbyState = LobbyState.MAIN;
        printLobbyMenu();
    }

    private void handleLobbyPendingDecision(String cmd) throws Exception {
        if (cmd == null || cmd.trim().isEmpty()) {
            lobbyState = LobbyState.MAIN;
            printLobbyMenu();
            return;
        }

        String[] parts = cmd.trim().split("\\s+");
        if (parts.length != 2) {
            System.out.println("Gebruik: a <nr> of n <nr>");
            System.out.print("Typ 'a <nr>' om te accepteren of 'n <nr>' om af te wijzen (Enter=terug): ");
            return;
        }

        String action = parts[0].toLowerCase();
        String nr = parts[1];
        PendingChallenge pc = pendingChallenges.get(nr);
        if (pc == null) {
            System.out.println("Onbekend challenge-nummer: " + nr);
            System.out.print("Typ 'a <nr>' om te accepteren of 'n <nr>' om af te wijzen (Enter=terug): ");
            return;
        }

        if (action.equals("a") || action.equals("accept")) {
            System.out.println("Uitdaging #" + nr + " accepteren...");
            client.send("challenge accept " + nr);
            pendingChallenges.remove(nr);
        } else if (action.equals("n") || action.equals("nee") || action.equals("decline") || action.equals("reject")) {
            System.out.println("Uitdaging #" + nr + " afgewezen.");
            pendingChallenges.remove(nr);
            client.send("message \"" + pc.challenger + ", ik wijs je uitdaging (#" + nr + ") even af.\"");
        } else {
            System.out.println("Onbekende actie. Gebruik a of n.");
            System.out.print("Typ 'a <nr>' om te accepteren of 'n <nr>' om af te wijzen (Enter=terug): ");
            return;
        }

        lobbyState = LobbyState.MAIN;
        printLobbyMenu();
    }

    private void challengeFlow(BufferedReader br) throws Exception {
        client.send("get playerlist");
        Thread.sleep(400);

        java.util.List<String> candidates = new java.util.ArrayList<>();
        for (String p : lastPlayerList) {
            if (p == null) continue;
            if (!p.equalsIgnoreCase(playerName)) candidates.add(p);
        }

        if (candidates.isEmpty()) {
            System.out.println("Geen andere spelers online om uit te dagen.");
            return;
        }

        System.out.println("\nBeschikbare spelers:");
        for (int i = 0; i < candidates.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + candidates.get(i));
        }

        System.out.print("Kies speler nummer (of Enter om te annuleren): ");
        String input = br.readLine();
        if (input == null || input.trim().isEmpty()) return;

        int idx;
        try {
            idx = Integer.parseInt(input.trim()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Ongeldige invoer.");
            return;
        }
        if (idx < 0 || idx >= candidates.size()) {
            System.out.println("Nummer buiten bereik.");
            return;
        }

        String target = candidates.get(idx);

        String gameType = "reversi-8";
        System.out.println("Uitdaging versturen naar '" + target + "' voor " + gameType + "...");
        client.send("challenge " + target + " " + gameType);
        System.out.println("Uitdaging verstuurd. Wacht op acceptatie (of kies matchmaking).\n");
    }

    private void handlePendingChallenges(BufferedReader br) throws Exception {
        if (pendingChallenges.isEmpty()) {
            System.out.println("Geen openstaande uitdagingen.");
            return;
        }

        System.out.println("\nOpenstaande uitdagingen:");
        java.util.List<PendingChallenge> list = new java.util.ArrayList<>(pendingChallenges.values());
        list.sort(java.util.Comparator.comparing(pc -> pc.number));
        for (PendingChallenge pc : list) {
            System.out.println("  #" + pc.number + " van " + pc.challenger + " (" + pc.gameType + ")");
        }

        System.out.print("Typ 'a <nr>' om te accepteren of 'n <nr>' om af te wijzen (Enter=terug): ");
        String cmd = br.readLine();
        if (cmd == null || cmd.trim().isEmpty()) return;

        String[] parts = cmd.trim().split("\\s+");
        if (parts.length != 2) {
            System.out.println("Gebruik: a <nr> of n <nr>");
            return;
        }

        String action = parts[0].toLowerCase();
        String nr = parts[1];

        PendingChallenge pc = pendingChallenges.get(nr);
        if (pc == null) {
            System.out.println("Onbekend challenge-nummer: " + nr);
            return;
        }

        if (action.equals("a") || action.equals("accept")) {
            System.out.println("Uitdaging #" + nr + " accepteren...");
            client.send("challenge accept " + nr);
            pendingChallenges.remove(nr);
        } else if (action.equals("n") || action.equals("nee") || action.equals("decline") || action.equals("reject")) {
            System.out.println("Uitdaging #" + nr + " afgewezen.");
            pendingChallenges.remove(nr);
            client.send("message \"" + pc.challenger + ", ik wijs je uitdaging (#" + nr + ") even af.\"");
        } else {
            System.out.println("Onbekende actie. Gebruik a of n.");
        }
    }

    private void handleChallenge(ServerMessage msg) {
        String challenger = msg.get("CHALLENGER");
        String gameType = msg.get("GAMETYPE");
        String number = msg.get("CHALLENGENUMBER");

        if (challenger == null || number == null) {
            return;
        }

        pendingChallenges.put(number, new PendingChallenge(number, challenger, gameType));

        System.out.println("\n" + "!".repeat(60));
        System.out.println("JE HEBT EEN UITDAGING ONTVANGEN!");
        System.out.println("Van: " + challenger);
        System.out.println("Spel: " + gameType);
        System.out.println("Nummer: " + number);
        System.out.println("Ga in de lobby naar optie 3 om te accepteren of af te wijzen.");
        System.out.println("!".repeat(60) + "\n");
    }

    private void handleChallengeCancelled(ServerMessage msg) {
        String number = msg.get("CHALLENGENUMBER");
        if (number != null) {
            pendingChallenges.remove(number);
            System.out.println("Uitdaging geannuleerd door server/challenger. Nummer: " + number);
        }
    }

    private void handlePlayerList(ServerMessage msg) {
        String raw = msg.get("raw");
        if (raw == null) return;
        lastPlayerList = parseStringList(raw);
    }

    private void printPlayerList() {
        System.out.println("\nSpelers online: " + lastPlayerList);
    }

    private java.util.List<String> parseStringList(String raw) {
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start < 0 || end <= start) return java.util.Collections.emptyList();
        String inside = raw.substring(start + 1, end).trim();
        if (inside.isEmpty()) return java.util.Collections.emptyList();

        java.util.List<String> out = new java.util.ArrayList<>();
        String[] parts = inside.split(",");
        for (String p : parts) {
            String name = p.trim();
            if (name.startsWith("\"") && name.endsWith("\"")) {
                name = name.substring(1, name.length() - 1);
            }
            if (!name.isEmpty()) out.add(name);
        }
        return out;
    }

    private void handleMatch(ServerMessage msg) {
        opponentName = msg.get("OPPONENT");
        firstPlayerName = msg.get("PLAYERTOMOVE");
        inMatch = true;
        myTurn = false;
        lobbyState = LobbyState.MAIN;

        // First player is BLACK, second is WHITE
        if (firstPlayerName.equalsIgnoreCase(playerName)) {
            myColor = Color.BLACK;
            game.setCurrentPlayer(Color.BLACK);
        } else {
            myColor = Color.WHITE;
            game.setCurrentPlayer(Color.BLACK); // Game starts with BLACK
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("              WEDSTRIJD GESTART!");
        System.out.println("=".repeat(60));
        System.out.println("Jij: " + playerName + " (Speelt als " + myColor + ")");
        System.out.println("Tegenstander: " + opponentName);
        System.out.println("Spel: " + msg.get("GAMETYPE"));
        System.out.println("Eerste zet: " + firstPlayerName);
        System.out.println("=".repeat(60));

        // Show initial board
        java.util.List<int[]> legalMoves = game.getBoard().getLegalMoves(Color.BLACK);
        boardView.printBoard(game.getBoard(), legalMoves);
    }

    private void handleYourTurn(ServerMessage msg) {
        myTurn = true;
        autoMoveMade.set(false);

        System.out.println("\n" + "=".repeat(50));
        System.out.println(" JOUW BEURT - JE HEBT ~10 SECONDEN!");
        System.out.println("=".repeat(50));
        System.out.println(msg.get("TURNMESSAGE"));

        // Show legal moves for MY color
        java.util.List<int[]> legalMoves = game.getBoard().getLegalMoves(myColor);
        currentLegalMoves = legalMoves;
        boardView.printBoard(game.getBoard(), legalMoves);

        if (legalMoves.isEmpty()) {
            System.out.println("Geen legale zetten beschikbaar!");
            client.send("choose -1");
            myTurn = false;
            return;
        } else {
            System.out.println("\nSNEL! Kies een zet (typ het nummer):");
            for (int i = 0; i < legalMoves.size(); i++) {
                int[] move = legalMoves.get(i);
                int position = move[0] * 8 + move[1];
                System.out.println("  " + (i+1) + ". Rij " + (move[0]+1) + ", Kolom " + (move[1]+1) + " (positie " + position + ")");
            }
            System.out.println();
        }

        // AI makes move immediately
        if (!(strategy instanceof strategies.HumanStrategy)) {
            System.out.println("\nAI denkt na...");
            int[] move = strategy.chooseMove(myColor, game.getBoard());

            if (move != null) {
                int row = move[0];
                int col = move[1];
                int position = row * 8 + col;
                System.out.println("AI koos: rij " + (row+1) + ", kolom " + (col+1) + " (positie " + position + ")");

                if (inMatch && myTurn) {
                    client.send("choose " + position);
                    myTurn = false;
                }
            }
            return;
        }

        // For human players, set flag to allow input thread to proceed
        isMyTurn = true;
        startTurnTimer();
    }

    private void startTurnTimer() {
        // Cancel previous timer if any
        if (turnTimer != null) {
            try { turnTimer.cancel(); } catch (Exception ignored) {}
        }
        if (currentLegalMoves == null || currentLegalMoves.isEmpty()) return;

        turnTimer = new java.util.Timer(true);
        turnTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                if (!inMatch || !isMyTurn || autoMoveMade.get()) return;
                autoMoveMade.set(true);
                System.out.println("\n\nTIJD IS OP! Automatisch een willekeurige zet spelen...");
                int[] randomMove = currentLegalMoves.get(new java.util.Random().nextInt(currentLegalMoves.size()));
                int row = randomMove[0];
                int col = randomMove[1];
                int position = row * 8 + col;
                System.out.println("Willekeurige zet: rij " + (row+1) + ", kolom " + (col+1) + " (positie " + position + ")");
                client.send("choose " + position);
                isMyTurn = false;
                myTurn = false;
            }
        }, 8500);
    }

    private void handleMoveInput(String input) {
        if (!inMatch || !isMyTurn) return;
        if (autoMoveMade.get()) return;

        if (input.equalsIgnoreCase("forfeit") || input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("opgeven")) {
            if (turnTimer != null) turnTimer.cancel();
            client.send("forfeit");
            isMyTurn = false;
            myTurn = false;
            return;
        }

        java.util.List<int[]> legalMoves = currentLegalMoves;
        if (legalMoves == null || legalMoves.isEmpty()) {
            System.out.println("Geen legale zetten beschikbaar!");
            isMyTurn = false;
            myTurn = false;
            return;
        }

        int row;
        int col;
        try {
            int selection = Integer.parseInt(input.trim());

            if (selection >= 1 && selection <= legalMoves.size()) {
                int[] selectedMove = legalMoves.get(selection - 1);
                row = selectedMove[0];
                col = selectedMove[1];
            } else if (selection >= 0 && selection < 64) {
                row = selection / 8;
                col = selection % 8;
            } else {
                System.out.println("Kies een nummer tussen 1 en " + legalMoves.size());
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Ongeldige invoer. Typ een nummer tussen 1 en " + legalMoves.size() + " (of 'opgeven').");
            return;
        }

        boolean isLegal = false;
        for (int[] mv : legalMoves) {
            if (mv[0] == row && mv[1] == col) {
                isLegal = true;
                break;
            }
        }
        if (!isLegal) {
            System.out.println("Illegale zet! Probeer opnieuw.");
            return;
        }

        // Send move
        if (turnTimer != null) turnTimer.cancel();
        int position = row * 8 + col;
        System.out.println("Zet verzenden: rij " + (row+1) + ", kolom " + (col+1) + " (positie " + position + ")");
        client.send("choose " + position);
        isMyTurn = false;
        myTurn = false;
    }

    private void handleMove(ServerMessage msg) {
        String player = msg.get("PLAYER");
        String moveStr = msg.get("MOVE");
        String details = msg.get("DETAILS");

        if (moveStr != null) {
            int position = Integer.parseInt(moveStr);
            int row = position / 8;
            int col = position % 8;

            boolean isMyMove = player.equalsIgnoreCase(playerName);
            String playerIcon = isMyMove ? "[JIJ]" : "[TEGENSTANDER]";

            System.out.println("\n" + "-".repeat(60));
            System.out.println(playerIcon + " " + player + " speelde: Rij " + (row+1) + ", Kolom " + (col+1) + " (Positie " + position + ")");
            if (details != null && !details.isEmpty()) {
                System.out.println("   Info: " + details);
            }
            System.out.println("-".repeat(60));

            // Determine move color based on which player moved
            Color moveColor;
            if (player.equalsIgnoreCase(firstPlayerName)) {
                // First player (from MATCH message) always plays as BLACK
                moveColor = Color.BLACK;
            } else {
                // Other player plays as WHITE
                moveColor = Color.WHITE;
            }

            // Update local board
            if (game.getBoard().isLegalMove(row, col, moveColor)) {
                game.getBoard().makeMove(row, col, moveColor);

                // Next player's turn
                game.nextPlayer();
            } else {
                System.out.println("Waarschuwing: Ontvangen zet is niet legaal op lokaal bord");
            }

            // Show board with legal moves for current player
            java.util.List<int[]> legalMoves = game.getBoard().getLegalMoves(game.getCurrentPlayer());
            boardView.printBoard(game.getBoard(), legalMoves);
        }
    }

    private void handleGameEnd(String result, ServerMessage msg) {
        inMatch = false;
        myTurn = false;
        isMyTurn = false;
        autoMoveMade.set(false);
        if (turnTimer != null) {
            try { turnTimer.cancel(); } catch (Exception ignored) {}
        }
        String p1Score = msg.get("PLAYERONESCORE");
        String p2Score = msg.get("PLAYERTWOSCORE");
        String comment = msg.get("COMMENT");

        System.out.println("\n" + "=".repeat(60));
        System.out.println("                    SPEL AFGELOPEN");
        System.out.println("=".repeat(60));

        // Determine who is who
        String player1 = firstPlayerName;
        String player2 = firstPlayerName.equalsIgnoreCase(playerName) ? opponentName : playerName;
        if (!firstPlayerName.equalsIgnoreCase(playerName)) {
            player1 = opponentName;
            player2 = playerName;
        }

        System.out.println("\nEINDSCORES:");
        System.out.println("   " + player1 + ": " + p1Score + " stenen");
        System.out.println("   " + player2 + ": " + p2Score + " stenen");

        System.out.println("\nRESULTAAT VOOR JOU ("+playerName+"):");
        if (result.equals("WIN")) {
            System.out.println("   JE HEBT GEWONNEN! Gefeliciteerd!");
        } else if (result.equals("LOSS")) {
            System.out.println("   Je hebt verloren. Volgende keer beter!");
        } else {
            System.out.println("   Het is GELIJKSPEL!");
        }

        if (comment != null && !comment.isEmpty()) {
            System.out.println("\nOpmerking: " + comment);
        }

        System.out.println("\nEINDBORD:");
        boardView.printBoard(game.getBoard());
        System.out.println("=".repeat(60));
        gameActive = false;
    }
}