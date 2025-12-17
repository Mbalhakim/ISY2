package core;

import enums.Color;
import interfaces.Strategy;
import java.util.List;

public class Game {
    private final Board board;
    private final Strategy blackStrategy;
    private final Strategy whiteStrategy;
    private Color currentPlayer;

    public Game(Strategy blackStrategy, Strategy whiteStrategy) {
        this.board = new Board();
        this.blackStrategy = blackStrategy;
        this.whiteStrategy = whiteStrategy;
        this.currentPlayer = Color.BLACK;
    }
    
    public void play() {
        while (currentPlayer != null) {
            board.printBoard();
            
            Strategy activeStrategy = (currentPlayer == Color.BLACK) 
                ? blackStrategy : whiteStrategy;
            
            System.out.println("\n=== " + currentPlayer + " (" + activeStrategy.getName() + ") ===");
            
            if (!board.hasLegalMove(currentPlayer)) {
                System.out.println(currentPlayer + " heeft geen zetten. PASS.");
                currentPlayer = getNextPlayer(currentPlayer);
                continue;
            }
            
            // Krijg zet
            int[] move = activeStrategy.chooseMove(currentPlayer, board);
            
            if (move != null) {
                System.out.println(currentPlayer + " speelt: Rij " + (move[0]+1) + ", Kolom " + (move[1]+1));
                board.makeMove(move[0], move[1], currentPlayer);
            }
            
            currentPlayer = getNextPlayer(currentPlayer);
        }
        
        // Game over
        System.out.println("\n========== GAME OVER ==========\n");
        board.printBoard();
        
        int blackScore = board.getScore(Color.BLACK);
        int whiteScore = board.getScore(Color.WHITE);
        
        System.out.println("\nBLACK: " + blackScore + " stenen");
        System.out.println("WHITE: " + whiteScore + " stenen");
        
        if (blackScore > whiteScore) {
            System.out.println("\nWINNAAR: BLACK");
        } else if (whiteScore > blackScore) {
            System.out.println("\nWINNAAR: WHITE");
        } else {
            System.out.println("\nGELIJKSPEL");
        }
    }
    
    private Color getNextPlayer(Color prevPlayer) {
        Color otherPlayer = prevPlayer.opposite();
        
        if (board.hasLegalMove(otherPlayer)) {
            return otherPlayer;
        } else if (board.hasLegalMove(prevPlayer)) {
            return prevPlayer;
        } else {
            return null;
        }
    }
}
