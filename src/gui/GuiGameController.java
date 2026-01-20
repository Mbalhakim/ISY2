package gui;

import core.Game;
import core.Board;
import enums.Color;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import strategies.HumanStrategy;

/**
 * Minimal controller that bridges Game and BoardPane.
 * For networked play, integrate with OnlineGameController instead:
 * - Use Platform.runLater for UI updates
 * - Run blocking/network tasks off the FX thread
 */
public class GuiGameController {
    private final Game game;
    private final BoardPane boardPane;
    private final Label turnLabel;

    // guard to prevent multiple concurrent AI runners
    private final AtomicBoolean aiRunning = new AtomicBoolean(false);
    // ensure game-over dialog shown only once
    private final AtomicBoolean gameOverShown = new AtomicBoolean(false);

    public GuiGameController(Game game, BoardPane boardPane, Label turnLabel) {
        this.game = game;
        this.boardPane = boardPane;
        this.turnLabel = turnLabel;
    }

    public void refreshBoard() {
        Platform.runLater(() -> {
            Board b = game.getBoard();
            List<int[]> legal = b.getLegalMoves(game.getCurrentPlayer());
            boardPane.drawBoard(b, legal);

            // update turn label with current player and scores
            String playerText = (game.getCurrentPlayer() == null) ? "Game Over" : game.getCurrentPlayer().toString();
            int black = game.getScore(Color.BLACK);
            int white = game.getScore(Color.WHITE);
            turnLabel.setText("Turn: " + playerText + " — B: " + black + " W: " + white);

            // if game over, show dialog once
            if (game.isGameOver() && gameOverShown.compareAndSet(false, true)) {
                showGameOver();
            }
        });

        // possibly trigger AI moves (off FX thread)
        startAIMovesIfNeeded();
    }

    public void onCellClicked(int row, int col) {
        // Only allow human strategy to perform clicks
        if (!(game.getActiveStrategy() instanceof HumanStrategy)) {
            System.out.println("Not a human turn - click ignored.");
            return;
        }

        // recompute legal moves just before sending
        Board b = game.getBoard();
        enums.Color player = game.getCurrentPlayer();
        List<int[]> legal = b.getLegalMoves(player);
        boolean isLegal = legal.stream().anyMatch(m -> m[0] == row && m[1] == col);
        if (!isLegal) {
            System.out.println("Clicked illegal cell: " + (row+1) + "," + (col+1));
            return;
        }

        // Execute move on background thread to avoid blocking UI
        new Thread(() -> {
            game.makeMove(new int[]{row, col});
            game.nextPlayer();
            // update UI
            refreshBoard();
        }).start();
    }

    private void startAIMovesIfNeeded() {
        // Start a single background thread that will play out consecutive AI turns
        if (!aiRunning.compareAndSet(false, true)) return; // already running
        if (game.getCurrentPlayer() == null) {
            aiRunning.set(false);
            return;
        }
        if (game.getActiveStrategy() instanceof HumanStrategy) {
            aiRunning.set(false);
            return;
        }

        new Thread(() -> {
            try {
                // Loop while current active player is an AI and game not over
                while (true) {
                    // check end conditions each iteration
                    if (game.getCurrentPlayer() == null) break;
                    if (game.getActiveStrategy() instanceof HumanStrategy) break;

                    // Recompute legal moves for current player
                    List<int[]> legal = game.getBoard().getLegalMoves(game.getCurrentPlayer());
                    if (legal == null || legal.isEmpty()) {
                        // no moves: pass turn
                        game.nextPlayer();
                        Platform.runLater(this::refreshBoard);
                        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                        continue;
                    }

                    // Ask strategy for a move (may block). Wrap to avoid crashing the AI loop.
                    int[] move = null;
                    try {
                        move = game.getMoveFromStrategy();
                    } catch (Exception e) {
                        // log and continue
                        System.err.println("AI strategy error: " + e.getMessage());
                        e.printStackTrace();
                    }

                    // Fallback: if strategy returned null, pick a random legal move
                    if (move == null) {
                        move = legal.get(new java.util.Random().nextInt(legal.size()));
                    }

                    // Verify the chosen move is still legal (recompute)
                    boolean isStillLegal = false;
                    List<int[]> currentLegal = game.getBoard().getLegalMoves(game.getCurrentPlayer());
                    if (currentLegal != null) {
                        for (int[] m : currentLegal) {
                            if (m[0] == move[0] && m[1] == move[1]) {
                                isStillLegal = true;
                                break;
                            }
                        }
                    }

                    if (!isStillLegal) {
                        // move became illegal (race or board changed). Refresh and retry
                        Platform.runLater(this::refreshBoard);
                        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                        continue;
                    }

                    // Make move and advance player
                    game.makeMove(move);
                    game.nextPlayer();

                    // update UI after each move
                    Platform.runLater(this::refreshBoard);

                    // small delay so UI updates are visible / avoid hammering
                    try { Thread.sleep(350); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                }
            } finally {
                aiRunning.set(false);
                // ensure final UI update and game-over detection
                Platform.runLater(() -> {
                    refreshBoard();
                });
            }
        }).start();
    }

    private void showGameOver() {
        Platform.runLater(() -> {
            Color winner = game.getWinner();
            String msg;
            if (winner == null) {
                msg = "Draw!";
            } else if (winner == Color.BLACK) {
                msg = "Black wins! \uD83C\uDFC6";
            } else {
                msg = "White wins! \uD83C\uDFC6";
            }

            // Update status label and show an alert
            turnLabel.setText("Game over: " + msg + "  Score B: " + game.getScore(Color.BLACK) + " W: " + game.getScore(Color.WHITE));
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText("Game finished");
            alert.setContentText(msg + "\nFinal score — Black: " + game.getScore(Color.BLACK)
                    + " White: " + game.getScore(Color.WHITE));
            alert.showAndWait();
        });
    }
}
