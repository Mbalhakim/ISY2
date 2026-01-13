package controller;

import core.Board;
import core.Game;
import enums.Color;
import interfaces.Strategy;
import network.ServerClient;
import network.ServerListener;
import network.ServerMessage;
import view.BoardView;
import view.GameView;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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
            Thread.sleep(500); // Wait for connection
            
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

            // Wait for game to end
            while (gameActive && client.isConnected()) {
                if (isMyTurn) {
                    makeMove(br);
                } else {
                    // Wait for opponent's turn
                    Thread.sleep(100);
                }
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
        
        // Determine colors: first player is BLACK
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
        
        // Show legal moves
        java.util.List<int[]> legalMoves = game.getBoard().getLegalMoves(game.getCurrentPlayer());
        boardView.printBoard(game.getBoard(), legalMoves);
    }

    private void handleYourTurn(ServerMessage msg) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println(" JOUW BEURT - JE HEBT ~10 SECONDEN!");
        System.out.println("=".repeat(50));
        System.out.println(msg.get("TURNMESSAGE"));
        
        // Show legal moves
        java.util.List<int[]> legalMoves = game.getBoard().getLegalMoves(game.getCurrentPlayer());
        boardView.printBoard(game.getBoard(), legalMoves);
        
        if (legalMoves.isEmpty()) {
            System.out.println("Geen legale zetten beschikbaar!");
        } else {
            System.out.println("\nSNEL! Legale zetten (" + legalMoves.size() + " beschikbaar):");
            for (int[] move : legalMoves) {
                System.out.print("  (" + (move[0]+1) + "," + (move[1]+1) + ")");
            }
            System.out.println();
        }
        
        isMyTurn = true;
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

            // Determine the color of the player who made this move
            Color moveColor = game.getCurrentPlayer();
            
            // Make the move on our local board
            if (game.getBoard().isLegalMove(row, col, moveColor)) {
                game.getBoard().makeMove(row, col, moveColor);
                
                // Switch to next player
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
        
        // Get legal moves
        java.util.List<int[]> legalMoves = game.getBoard().getLegalMoves(game.getCurrentPlayer());
        
        if (legalMoves.isEmpty()) {
            System.out.println("Geen legale zetten beschikbaar!");
            isMyTurn = false;
            return;
        }
        
        // Check if using AI strategy
        if (!(strategy instanceof strategies.HumanStrategy)) {
            // AI makes the move immediately
            System.out.println("\nAI denkt na...");
            int[] move = strategy.chooseMove(game.getCurrentPlayer(), game.getBoard());
            if (move != null) {
                int position = move[0] * 8 + move[1];
                System.out.println("AI koos: rij " + (move[0]+1) + ", kolom " + (move[1]+1) + " (positie " + position + ")");
                client.send("move " + position);
                isMyTurn = false;
            }
            return;
        }
        
        // Human player - use input with timer
        // Start auto-move timer (8.5 seconds to be safe)
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                if (isMyTurn && !autoMoveMade[0]) {
                    autoMoveMade[0] = true;
                    System.out.println("\n\nTIJD IS OP! Automatisch een willekeurige zet spelen...");
                    int[] randomMove = legalMoves.get(new java.util.Random().nextInt(legalMoves.size()));
                    int position = randomMove[0] * 8 + randomMove[1];
                    System.out.println("Willekeurige zet: rij " + (randomMove[0]+1) + ", kolom " + (randomMove[1]+1) + " (positie " + position + ")");
                    client.send("move " + position);
                    isMyTurn = false;
                }
            }
        }, 8500); // 8.5 seconds
        
        // Use a separate thread for input to avoid blocking
        Thread inputThread = new Thread(() -> {
            while (!validMove && isMyTurn && gameActive && !autoMoveMade[0]) {
                try {
                    System.out.print("\nZET (10s limiet - formaten: '23', '2 3', of '2,3'): ");
                    String input = br.readLine().trim();

                    if (autoMoveMade[0] || !isMyTurn) {
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
                        // Try space-separated format: "2 3"
                        if (input.contains(" ")) {
                            String[] parts = input.split("\\s+");
                            if (parts.length == 2) {
                                row = Integer.parseInt(parts[0]) - 1; // Convert to 0-indexed
                                col = Integer.parseInt(parts[1]) - 1; // Convert to 0-indexed
                            }
                        }
                        // Try comma-separated format: "2,3"
                        else if (input.contains(",")) {
                            String[] parts = input.split(",");
                            if (parts.length == 2) {
                                row = Integer.parseInt(parts[0].trim()) - 1; // Convert to 0-indexed
                                col = Integer.parseInt(parts[1].trim()) - 1; // Convert to 0-indexed
                            }
                        }
                        // Try concatenated format: "23"
                        else if (input.length() == 2 && Character.isDigit(input.charAt(0)) && Character.isDigit(input.charAt(1))) {
                            row = Character.getNumericValue(input.charAt(0)) - 1; // Convert to 0-indexed
                            col = Character.getNumericValue(input.charAt(1)) - 1; // Convert to 0-indexed
                        }
                        // Single digit with space/comma variations
                        else {
                            System.out.println("Ongeldig formaat. Gebruik: '2 3' of '23' of '2,3'");
                            continue;
                        }
                        
                        // Validate range (now checking 0-7 after conversion from 1-8 input)
                        if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                            // Check if move is legal
                            boolean isLegal = false;
                            for (int[] move : legalMoves) {
                                if (move[0] == row && move[1] == col) {
                                    isLegal = true;
                                    break;
                                }
                            }
                            
                            if (isLegal) {
                                timer.cancel(); // Cancel auto-move timer
                                // Convert to server format (0-63)
                                int position = row * 8 + col;
                                System.out.println("Zet verzenden: rij " + (row+1) + ", kolom " + (col+1) + " (positie " + position + ")");
                                client.send("move " + position);
                                isMyTurn = false;
                                return;
                            } else {
                                System.out.println("Illegale zet! Positie (" + (row+1) + "," + (col+1) + ") is geen geldige zet.");
                                System.out.println("   Legale zetten zijn: ");
                                for (int[] move : legalMoves) {
                                    System.out.print("   (" + (move[0]+1) + "," + (move[1]+1) + ")");
                                }
                                System.out.println();
                            }
                        } else {
                            System.out.println("Buiten bereik! Rij en kolom moeten 1-8 zijn.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Ongeldige nummers. Voer alleen cijfers in.");
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
