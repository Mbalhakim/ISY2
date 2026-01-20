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
    private Color opponentColor;
    private String firstPlayerName;
    private boolean isMyTurn;
    private boolean gameActive;
    private boolean inMatch = false;

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

            // Subscribe to Othello
            System.out.println("Inschrijven voor Othello spel...");
            client.send("subscribe reversi-8");
            System.out.println("Wachten op tegenstander...");

            while (gameActive && client.isConnected()) {
                if (isMyTurn) {
                    makeMove(br);
                }
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

    private void handleServerMessage(String rawMessage) {
        System.out.println("[SERVER] " + rawMessage);
        ServerMessage msg = new ServerMessage(rawMessage);

        switch (msg.getType()) {
            case "MATCH":
                handleMatch(msg);
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

    private void handleMatch(ServerMessage msg) {
        opponentName = msg.get("OPPONENT");
        firstPlayerName = msg.get("PLAYERTOMOVE");
        inMatch = true;

        // Determine local and opponent colors and cache them (do NOT derive later from names)
        if (firstPlayerName.equalsIgnoreCase(playerName)) {
            myColor = Color.BLACK;
            opponentColor = Color.WHITE;
            game.setCurrentPlayer(Color.BLACK);
        } else {
            myColor = Color.WHITE;
            opponentColor = Color.BLACK;
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
        // mark that it's our turn (use only isMyTurn)
        isMyTurn = true;

        System.out.println("\n" + "=".repeat(50));
        System.out.println(" JOUW BEURT - JE HEBT ~10 SECONDEN!");
        System.out.println("=".repeat(50));
        System.out.println(msg.get("TURNMESSAGE"));

        // Show legal moves for MY color
        java.util.List<int[]> legalMoves = game.getBoard().getLegalMoves(myColor);
        boardView.printBoard(game.getBoard(), legalMoves);

        if (legalMoves.isEmpty()) {
            System.out.println("Geen legale zetten beschikbaar!");
            client.send("choose -1");
            isMyTurn = false;
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
            // recompute legal moves just before sending
            java.util.List<int[]> currentLegal = game.getBoard().getLegalMoves(myColor);
            int[] move = strategy.chooseMove(myColor, game.getBoard());

            if (move != null) {
                int row = move[0];
                int col = move[1];
                int position = row * 8 + col;
                System.out.println("AI koos: rij " + (row+1) + ", kolom " + (col+1) + " (positie " + position + ")");

                // ensure move is currently legal
                boolean isStillLegal = false;
                for (int[] m : currentLegal) {
                    if (m[0] == row && m[1] == col) {
                        isStillLegal = true;
                        break;
                    }
                }

                if (inMatch && isMyTurn && isStillLegal) {
                    client.send("choose " + position);
                    isMyTurn = false;
                } else {
                    System.out.println("AI zet niet verzonden (niet meer legaal of niet onze beurt)");
                }
            }
            return;
        }

        // For human players, isMyTurn already true so input thread will proceed
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

            // Determine move color: use cached colors instead of deriving via firstPlayerName
            Color moveColor = isMyMove ? myColor : opponentColor;

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
        isMyTurn = false;
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

    private void makeMove(BufferedReader br) {
        boolean validMove = false;
        final boolean[] autoMoveMade = {false};
        java.util.List<int[]> legalMoves = game.getBoard().getLegalMoves(myColor);

        if (legalMoves.isEmpty()) {
            System.out.println("Geen legale zetten beschikbaar!");
            isMyTurn = false;
            return;
        }

        // Auto-move timer (8.5 seconds)
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                if (isMyTurn && !autoMoveMade[0] && inMatch) {
                    autoMoveMade[0] = true;
                    System.out.println("\n\nTIJD IS OP! Automatisch een willekeurige zet spelen...");
                    // recompute legal moves here to avoid stale list
                    java.util.List<int[]> currentLegal = game.getBoard().getLegalMoves(myColor);
                    if (currentLegal.isEmpty()) {
                        client.send("choose -1");
                        isMyTurn = false;
                        return;
                    }
                    int[] randomMove = currentLegal.get(new java.util.Random().nextInt(currentLegal.size()));
                    int row = randomMove[0];
                    int col = randomMove[1];
                    int position = row * 8 + col;
                    System.out.println("Willekeurige zet: rij " + (row+1) + ", kolom " + (col+1) + " (positie " + position + ")");
                    client.send("choose " + position);
                    isMyTurn = false;
                }
            }
        }, 8500); // 8.5 seconds

        // Use a separate thread for input to avoid blocking
        Thread inputThread = new Thread(() -> {
            while (!validMove && isMyTurn && gameActive && !autoMoveMade[0]) {
                try {
                    System.out.print("\nKies zet (typ nummer 1-" + legalMoves.size() + ", of 'opgeven'): ");
                    String input = br.readLine();
                    if (input == null) {
                        return;
                    }
                    input = input.trim();

                    if (autoMoveMade[0] || !isMyTurn || !inMatch) {
                        return;
                    }

                    if (input.equalsIgnoreCase("forfeit") || input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("opgeven")) {
                        timer.cancel();
                        client.send("forfeit");
                        isMyTurn = false;
                        return;
                    }

                    int row = -1;
                    int col = -1;

                    try {
                        int selection = Integer.parseInt(input);

                        if (selection >= 1 && selection <= legalMoves.size()) {
                            int[] selectedMove = legalMoves.get(selection - 1);
                            row = selectedMove[0];
                            col = selectedMove[1];
                        } else if (selection >= 0 && selection < 64) {
                            row = selection / 8;
                            col = selection % 8;
                        } else {
                            System.out.println("Kies een nummer tussen 1 en " + legalMoves.size());
                            continue;
                        }

                        if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                            // recompute legal moves just before sending to avoid stale choices
                            java.util.List<int[]> currentLegal = game.getBoard().getLegalMoves(myColor);
                            boolean isLegal = false;
                            for (int[] move : currentLegal) {
                                if (move[0] == row && move[1] == col) {
                                    isLegal = true;
                                    break;
                                }
                            }

                            if (isLegal) {
                                timer.cancel();
                                int position = row * 8 + col;
                                System.out.println("Zet verzenden: rij " + (row+1) + ", kolom " + (col+1) + " (positie " + position + ")");
                                if (inMatch && isMyTurn) {
                                    client.send("choose " + position);
                                    isMyTurn = false;
                                    return;
                                } else {
                                    System.out.println("Kan zet niet verzenden - niet in geldige match staat");
                                    return;
                                }
                            } else {
                                System.out.println("Illegale zet! Probeer opnieuw.");
                                System.out.println("   Legale zetten zijn:");
                                for (int i = 0; i < legalMoves.size(); i++) {
                                    int[] move = legalMoves.get(i);
                                    System.out.println("   " + (i+1) + ". (" + (move[0]+1) + "," + (move[1]+1) + ")");
                                }
                            }
                        } else {
                            System.out.println("Buiten bereik! Probeer opnieuw.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Ongeldige invoer. Typ een nummer tussen 1 en " + legalMoves.size());
                    }
                } catch (Exception e) {
                    if (isMyTurn && !autoMoveMade[0]) {
                        System.out.println("Fout bij lezen invoer: " + e.getMessage());
                    }
                    break;
                }
            }
        });

        inputThread.start();

        // Wait for either input or timer
        try {
            inputThread.join(9000); // Wait max 9 seconds for input
        } catch (InterruptedException e) {
            // Ignore
        }

        timer.cancel(); // Ensure timer is cancelled
    }
}
