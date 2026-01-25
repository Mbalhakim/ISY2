package gui;

import core.Game;
import enums.Color;
import interfaces.Strategy;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import network.ServerClient;
import network.ServerListener;
import network.ServerMessage;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * GUI-based online game controller that integrates with BoardPane
 * instead of using console I/O.
 */
public class GuiOnlineGameController {
    private ServerClient client;
    private Game game;
    private BoardPane boardPane;
    private Label turnLabel;
    private Label statusLabel;
    private Strategy strategy;

    private String playerName;
    private String opponentName;
    private Color myColor;
    private Color opponentColor;
    private String firstPlayerName;
    private boolean isMyTurn;
    private boolean gameActive;
    private boolean inMatch = false;
    private AtomicBoolean moveInProgress = new AtomicBoolean(false);
    private Timer autoMoveTimer;

    public GuiOnlineGameController(Game game, BoardPane boardPane, Label turnLabel, Label statusLabel, Strategy strategy) {
        this.game = game;
        this.boardPane = boardPane;
        this.turnLabel = turnLabel;
        this.statusLabel = statusLabel;
        this.strategy = strategy;
        this.client = new ServerClient();
        this.isMyTurn = false;
        this.gameActive = true;
    }

    public void start() {
        // Setup listener
        client.addListener(new ServerListener() {
            @Override
            public void onMessage(String message) {
                handleServerMessage(message);
            }

            @Override
            public void onError(Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                });
            }

            @Override
            public void onClose() {
                Platform.runLater(() -> {
                    statusLabel.setText("Connection closed");
                    gameActive = false;
                });
            }
        });

        // Run connection and login on background thread
        new Thread(() -> {
            try {
                Platform.runLater(() -> statusLabel.setText("Connecting to server..."));
                client.connect("127.0.0.1", 7789);
                Thread.sleep(500);

                // Prompt for player name on FX thread
                Platform.runLater(() -> {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Player Name");
                    dialog.setHeaderText("Online Game - Login");
                    dialog.setContentText("Enter your player name:");

                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(name -> {
                        playerName = name;
                        // Send login on background thread
                        new Thread(() -> {
                            try {
                                client.send("login " + playerName);
                                Thread.sleep(500);
                                client.send("subscribe reversi-8");
                                Platform.runLater(() -> statusLabel.setText("Waiting for opponent..."));
                            } catch (Exception e) {
                                Platform.runLater(() -> statusLabel.setText("Error during login: " + e.getMessage()));
                            }
                        }).start();
                    });

                    if (!result.isPresent()) {
                        cleanup();
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Connection failed: " + e.getMessage());
                    showAlert("Connection Error", "Failed to connect to game server: " + e.getMessage());
                });
                cleanup();
            }
        }).start();
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

        // Determine colors
        if (firstPlayerName.equalsIgnoreCase(playerName)) {
            myColor = Color.BLACK;
            opponentColor = Color.WHITE;
            game.setCurrentPlayer(Color.BLACK);
        } else {
            myColor = Color.WHITE;
            opponentColor = Color.BLACK;
            game.setCurrentPlayer(Color.BLACK);
        }

        Platform.runLater(() -> {
            statusLabel.setText("Match started! You: " + myColor + " vs " + opponentName);
            refreshBoard();
        });
    }

    private void handleYourTurn(ServerMessage msg) {
        isMyTurn = true;
        cancelAutoMoveTimer();

        Platform.runLater(() -> {
            List<int[]> legalMoves = game.getBoard().getLegalMoves(myColor);

            if (legalMoves.isEmpty()) {
                statusLabel.setText("No legal moves available - skipping turn");
                client.send("choose -1");
                isMyTurn = false;
                return;
            }

            statusLabel.setText("YOUR TURN! (~10 seconds) - " + legalMoves.size() + " legal moves");
            refreshBoard();

            // Set up auto-move timer (8.5 seconds)
            startAutoMoveTimer();

            // If AI strategy, make move immediately
            if (!(strategy instanceof strategies.HumanStrategy)) {
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // Small delay for realism
                        makeAIMove();
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }).start();
            }
        });
    }

    private void handleMove(ServerMessage msg) {
        String player = msg.get("PLAYER");
        String moveStr = msg.get("MOVE");

        if (moveStr != null) {
            int position = Integer.parseInt(moveStr);
            int row = position / 8;
            int col = position % 8;

            boolean isMyMove = player.equalsIgnoreCase(playerName);
            Color moveColor = isMyMove ? myColor : opponentColor;

            Platform.runLater(() -> {
                String playerLabel = isMyMove ? "You" : opponentName;
                statusLabel.setText(playerLabel + " played: Row " + (row+1) + ", Col " + (col+1));

                // Update board
                if (game.getBoard().isLegalMove(row, col, moveColor)) {
                    game.getBoard().makeMove(row, col, moveColor);
                    game.nextPlayer();
                    refreshBoard();
                } else {
                    System.out.println("Warning: Received illegal move from server");
                }
            });
        }
    }

    private void handleGameEnd(String result, ServerMessage msg) {
        inMatch = false;
        isMyTurn = false;
        gameActive = false;
        cancelAutoMoveTimer();

        String p1Score = msg.get("PLAYERONESCORE");
        String p2Score = msg.get("PLAYERTWOSCORE");
        String comment = msg.get("COMMENT");

        Platform.runLater(() -> {
            String resultText;
            if (result.equals("WIN")) {
                resultText = "YOU WON!";
            } else if (result.equals("LOSS")) {
                resultText = "You Lost";
            } else {
                resultText = "DRAW";
            }

            statusLabel.setText("Game Over - " + resultText);
            refreshBoard();

            String message = resultText + "\n\n" +
                    "Final Scores:\n" +
                    "Player 1: " + p1Score + "\n" +
                    "Player 2: " + p2Score;

            if (comment != null && !comment.isEmpty()) {
                message += "\n\n" + comment;
            }

            showAlert("Game Over", message);
        });
    }

    private void startAutoMoveTimer() {
        cancelAutoMoveTimer();
        autoMoveTimer = new Timer();
        autoMoveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isMyTurn && !moveInProgress.get() && inMatch) {
                    Platform.runLater(() -> {
                        statusLabel.setText("TIME'S UP! Making random move...");
                        makeRandomMove();
                    });
                }
            }
        }, 8500);
    }

    private void cancelAutoMoveTimer() {
        if (autoMoveTimer != null) {
            autoMoveTimer.cancel();
            autoMoveTimer = null;
        }
    }

    private void makeRandomMove() {
        if (!isMyTurn || moveInProgress.getAndSet(true)) return;

        List<int[]> legalMoves = game.getBoard().getLegalMoves(myColor);
        if (legalMoves.isEmpty()) {
            client.send("choose -1");
            isMyTurn = false;
            moveInProgress.set(false);
            return;
        }

        int[] randomMove = legalMoves.get(new java.util.Random().nextInt(legalMoves.size()));
        int position = randomMove[0] * 8 + randomMove[1];

        cancelAutoMoveTimer();
        client.send("choose " + position);
        isMyTurn = false;
        moveInProgress.set(false);
    }

    private void makeAIMove() {
        if (!isMyTurn || moveInProgress.getAndSet(true)) return;

        List<int[]> legalMoves = game.getBoard().getLegalMoves(myColor);
        int[] move = strategy.chooseMove(myColor, game.getBoard());

        if (move != null) {
            // Verify move is still legal
            boolean isLegal = legalMoves.stream().anyMatch(m -> m[0] == move[0] && m[1] == move[1]);

            if (isLegal) {
                int position = move[0] * 8 + move[1];
                Platform.runLater(() -> {
                    statusLabel.setText("AI chose: Row " + (move[0]+1) + ", Col " + (move[1]+1));
                });

                cancelAutoMoveTimer();
                client.send("choose " + position);
                isMyTurn = false;
                moveInProgress.set(false);
                return;
            }
        }

        // Fallback to random move
        makeRandomMove();
    }

    public void onCellClicked(int row, int col) {
        if (!isMyTurn || moveInProgress.get() || !inMatch) {
            Platform.runLater(() -> statusLabel.setText("Not your turn or game not active"));
            return;
        }

        if (!(strategy instanceof strategies.HumanStrategy)) {
            return; // Only allow clicks for human strategy
        }

        // Check if move is legal
        List<int[]> legalMoves = game.getBoard().getLegalMoves(myColor);
        boolean isLegal = legalMoves.stream().anyMatch(m -> m[0] == row && m[1] == col);

        if (!isLegal) {
            Platform.runLater(() -> statusLabel.setText("Illegal move! Choose a highlighted cell."));
            return;
        }

        if (moveInProgress.compareAndSet(false, true)) {
            int position = row * 8 + col;

            Platform.runLater(() -> {
                statusLabel.setText("Sending move: Row " + (row+1) + ", Col " + (col+1));
            });

            cancelAutoMoveTimer();
            client.send("choose " + position);
            isMyTurn = false;
            moveInProgress.set(false);
        }
    }

    private void refreshBoard() {
        Platform.runLater(() -> {
            List<int[]> legalMoves;
            if (isMyTurn) {
                legalMoves = game.getBoard().getLegalMoves(myColor);
            } else {
                legalMoves = game.getBoard().getLegalMoves(game.getCurrentPlayer());
            }

            boardPane.drawBoard(game.getBoard(), legalMoves);

            int black = game.getScore(Color.BLACK);
            int white = game.getScore(Color.WHITE);
            String currentPlayer = game.getCurrentPlayer() != null ? game.getCurrentPlayer().toString() : "Game Over";
            turnLabel.setText("Turn: " + currentPlayer + " — B: " + black + " W: " + white + " — You: " + myColor);
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void cleanup() {
        gameActive = false;
        inMatch = false;
        isMyTurn = false;
        cancelAutoMoveTimer();
        if (client != null) {
            client.close();
        }
    }
}

