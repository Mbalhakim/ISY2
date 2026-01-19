package controller;
import core.Game;
import enums.Color;
import factory.DefaultGameFactory;
import interfaces.GameFactory;
import interfaces.Strategy;
import java.util.List;
import strategies.HumanStrategy;
import view.BoardView;
import view.GameView;
import view.MenuView;
import view.ScoreView;

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
            // Online mode - ask for strategy
            System.out.println("\nOnline Modus - Kies je strategie:");
            System.out.println("1. Menselijk (jij speelt handmatig)");
            System.out.println("2. Willekeurige AI");
            System.out.println("3. Minimax AI");
            System.out.print("Keuze (1-3): ");
            
            int strategyChoice = 1;
            try {
                strategyChoice = new java.util.Scanner(System.in).nextInt();
            } catch (Exception e) {
                System.out.println("Ongeldige invoer, gebruik menselijke strategie");
            }
            
            Strategy strategy;
            switch (strategyChoice) {
                case 2:
                    strategy = new strategies.RandomStrategy();
                    System.out.println("Gebruik Willekeurige AI strategie voor online spel");
                    break;
                case 3:
                    strategy = new strategies.MinimaxStrategy(3); // Depth 3 for faster online play
                    System.out.println("Gebruik Minimax AI strategie voor online spel");
                    break;
                default:
                    strategy = new strategies.HumanStrategy();
                    System.out.println("Gebruik menselijke strategie voor online spel");
                    break;
            }
            
            OnlineGameController onlineController = new OnlineGameController(game, boardView, gameView, strategy);
            onlineController.play();
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
}
