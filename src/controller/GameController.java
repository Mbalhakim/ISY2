package controller;
import core.Game;
import enums.Color;
import interfaces.Strategy;
import interfaces.GameFactory;
import factory.DefaultGameFactory;
import strategies.HumanStrategy;
import view.BoardView;
import view.GameView;
import view.ScoreView;
import view.MenuView;

import network.ISYClient;
import network.ISYListener;
import network.ISYMessage;

import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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

    private void playGame(Game game) {
        while (!game.isGameOver()) {
            Strategy activeStrategy = game.getActiveStrategy();
            List<int[]> legalMovesToShow = null;
            if (activeStrategy instanceof HumanStrategy) {
                legalMovesToShow = game.getBoard().getLegalMoves(game.getCurrentPlayer());
            }
            boardView.printBoard(game.getBoard(), legalMovesToShow);
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
        ISYClient client = new ISYClient();
        client.addListener(new ISYListener() {
            @Override
            public void onMessage(ISYMessage message) {
                System.out.println("[SERVER MSG] " + message.getType() + " -> " + message.getPayload());
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

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter username for server: ");
            String name = br.readLine();
            client.connect("127.0.0.1", 7789);
            client.sendRaw("login " + name);
            client.sendRaw("subscribe reversi-8");
            System.out.println("Connected and subscribed. Listening for server messages. Type 'quit' to exit.");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().equalsIgnoreCase("quit") || line.trim().equalsIgnoreCase("exit")) break;
                client.sendRaw(line);
            }
        } catch (Exception e) {
            System.out.println("Network error: " + e.getMessage());
        } finally {
            client.close();
        }
    }
}
