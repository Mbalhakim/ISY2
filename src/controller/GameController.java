package controller;

import core.Game;
import core.Board;
import enums.Color;
import interfaces.Strategy;
import strategies.HumanStrategy;
import strategies.RandomStrategy;
import controller.InputHandler;
import view.BoardView;
import view.GameView;
import view.ScoreView;
import view.MenuView;

public class GameController {
    private final InputHandler inputHandler;
    private final BoardView boardView;
    private final GameView gameView;
    private final ScoreView scoreView;
    private final MenuView menuView;

    public GameController() {
        this.inputHandler = new InputHandler();
        this.boardView = new BoardView();
        this.gameView = new GameView();
        this.scoreView = new ScoreView();
        this.menuView = new MenuView();
    }

    public void startGame() {
        gameView.printStart();
        int choice = menuView.getMenuChoice();
        String[] playerTypes = getPlayerTypes(choice);
        gameView.printTurn(playerTypes[0], playerTypes[1]);
        Game game = createGame(choice);
        playGame(game);
    }

    private Game createGame(int choice) {
        switch (choice) {
            case 1:
                return new Game(new HumanStrategy(), new HumanStrategy());
            case 2:
                return new Game(new HumanStrategy(), new RandomStrategy());
            case 3:
                return new Game(new RandomStrategy(), new RandomStrategy());
            default:
                return new Game(new HumanStrategy(), new HumanStrategy());
        }
    }

    private String[] getPlayerTypes(int choice) {
        switch (choice) {
            case 1:
                return new String[]{"Human (BLACK)", "Human (WHITE)"};
            case 2:
                return new String[]{"Human (BLACK)", "Random AI (WHITE)"};
            case 3:
                return new String[]{"Random AI (BLACK)", "Random AI (WHITE)"};
            default:
                return new String[]{"Human (BLACK)", "Human (WHITE)"};
        }
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
}
