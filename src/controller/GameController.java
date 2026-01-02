package controller;
import core.Game;
import core.Board;
import enums.Color;
import interfaces.Strategy;
import interfaces.GameFactory;
import factory.DefaultGameFactory;
import view.BoardView;
import view.GameView;
import view.ScoreView;
import view.MenuView;

import network.ISYClient;
import network.ISYListener;
import network.ISYMessage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Map;
import java.util.List;

public class GameController {
    private final InputHandler inputHandler;
    private final BoardView boardView;
    private final GameView gameView;
    private final ScoreView scoreView;
    private final MenuView menuView;
    private final GameFactory gameFactory;

    public GameController() {
        this.inputHandler = new InputHandler();
        this.boardView = new BoardView();
        this.gameView = new GameView();
        this.scoreView = new ScoreView();
        this.menuView = new MenuView();
        this.gameFactory = new DefaultGameFactory();
    }

    public void startGame() {
        gameView.printStart();
        int choice = menuView.getMenuChoice();
        String[] playerTypes = gameFactory.getPlayerTypes(choice);
        gameView.printTurn(playerTypes[0], playerTypes[1]);
        Game game = gameFactory.createGame(choice);
        if (choice == 7) {
            onlinePlay(game);
        } else {
            playGame(game);
        }
    }

    public void startOnlineGame() {
        Game game = gameFactory.createGame(7); // Online mode
        onlinePlay(game);
    }

    private void playGame(Game game) {
        while (!game.isGameOver()) {
            boardView.printBoard(game.getBoard());
            Strategy activeStrategy = game.getActiveStrategy();
            String player = game.getCurrentPlayer().toString();
            String strat = activeStrategy.getName();
            gameView.printTurn(player, strat);
            if (!game.hasCurrentPlayerMove()) {
                gameView.printNoMoves(player);
                game.nextPlayer();
                continue;
            }
            int[] move = game.getMoveFromStrategy();
            if (move != null) {
                gameView.printMove(player, move[0], move[1]);
                game.makeMove(move);
            }
            game.nextPlayer();
        }
        gameView.printGameOver();
        boardView.printBoard(game.getBoard());
        int blackScore = game.getScore(Color.BLACK);
        int whiteScore = game.getScore(Color.WHITE);
        scoreView.printScore(blackScore, whiteScore);
        Color winner = game.getWinner();
        gameView.printWinner(winner == null ? null : winner.toString());
    }

    private void onlinePlay(Game game) {
        System.out.println("Starting online mode: connecting to server localhost:7789...");
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter username for server (no spaces): ");
            String playerName = br.readLine().trim();
            ISYClient client = new ISYClient();
            final Color[] playerColorHolder = new Color[1];
            final String[] opponentNameHolder = new String[1];
            client.addListener(new ISYListener() {
                @Override
                public void onMessage(ISYMessage message) {
                    String type = message.getType();
                    Map<String, String> payload = message.getPayload();
                    if ("SVR GAME MATCH".equals(type)) {
                        opponentNameHolder[0] = payload.get("OPPONENT");
                        String toMove = payload.get("PLAYERTOMOVE");
                        if (toMove.equals(playerName)) {
                            playerColorHolder[0] = Color.BLACK; // Assuming first to move is black
                        } else {
                            playerColorHolder[0] = Color.WHITE;
                        }
                        Color firstPlayerColor = toMove.equals(playerName) ? playerColorHolder[0] : playerColorHolder[0].opposite();
                        game.setCurrentPlayer(firstPlayerColor);
                        gameView.printStart();
                        gameView.printTurn(game.getCurrentPlayer().toString(), "Human");
                        boardView.printBoard(game.getBoard());
                        System.out.println("Match started. You are " + playerColorHolder[0] + ", opponent: " + opponentNameHolder[0]);
                    } else if ("SVR GAME MOVE".equals(type)) {
                        String moveStr = payload.get("MOVE");
                        String player = payload.get("PLAYER");
                        int[] move = Board.stringToMove(moveStr);
                        if (move != null && playerColorHolder[0] != null) {
                            Color moveColor = player.equals(playerName) ? playerColorHolder[0] : playerColorHolder[0].opposite();
                            game.setCurrentPlayer(moveColor);
                            gameView.printMove(moveColor.toString(), move[0], move[1]);
                            game.makeMove(move);
                            game.nextPlayer();
                            boardView.printBoard(game.getBoard());
                            if (!game.isGameOver()) {
                                gameView.printTurn(game.getCurrentPlayer().toString(), "Human");
                            }
                        }
                    } else if ("SVR GAME YOURTURN".equals(type)) {
                        // Prompt for move
                        List<int[]> validMoves = game.getBoard().getLegalMoves(game.getCurrentPlayer());
                        if (!validMoves.isEmpty()) {
                            System.out.print("Valid moves: ");
                            for (int[] m : validMoves) {
                                System.out.print((m[0] + 1) + "" + (m[1] + 1) + " ");
                            }
                            System.out.println();
                        }
                        while (true) {
                            System.out.println("Your turn. Enter move (e.g., 44 for row 4, column 4): ");
                            try {
                                String moveStr = br.readLine().trim().toUpperCase();
                                int[] move = Board.stringToMove(moveStr);
                                if (move != null) {
                                    client.sendRaw("move reversi-8 " + moveStr);
                                    break;
                                } else {
                                    System.out.println("Invalid move format. Please enter two digits (e.g., 44).");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (type.startsWith("SVR GAME WIN") || type.startsWith("SVR GAME LOSS") || type.startsWith("SVR GAME DRAW")) {
                        // Handle WIN/LOSS/DRAW
                        gameView.printGameOver();
                        boardView.printBoard(game.getBoard());
                        int blackScore = game.getScore(Color.BLACK);
                        int whiteScore = game.getScore(Color.WHITE);
                        scoreView.printScore(blackScore, whiteScore);
                        Color winner = game.getWinner();
                        gameView.printWinner(winner == null ? null : winner.toString());
                        System.out.println("Game result: " + type);
                    }
                }

                @Override
                public void onRawLine(String line) {
                    System.out.println("[SERVER LINE] " + line);
                }

                @Override
                public void onError(Exception e) {
                    System.out.println("[SERVER ERROR] " + e.getMessage());
                }

                @Override
                public void onClose() {
                    System.out.println("[SERVER CLOSED]");
                }
            });
            client.connect("127.0.0.1", 7789);
            client.sendRaw("login " + playerName);
            client.sendRaw("subscribe reversi-8");
            System.out.println("Connected and subscribed. Listening for server messages. To quit, close the terminal.");
            // No main loop for input, moves are handled in YOURTURN
            // Wait indefinitely, or until close
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                // ignore
            }
            client.close();
        } catch (Exception e) {
            System.out.println("Network error: " + e.getMessage());
        }
    }
}
