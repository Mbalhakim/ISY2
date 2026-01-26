package core;

import enums.Color;
import interfaces.Strategy;

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

    public Board getBoard() {
        return board;
    }
    public Color getCurrentPlayer() {
        return currentPlayer;
    }
    
    public void setCurrentPlayer(Color color) {
        this.currentPlayer = color;
    }
    
    public Strategy getActiveStrategy() {
        return (currentPlayer == Color.BLACK) ? blackStrategy : whiteStrategy;
    }
    public boolean hasCurrentPlayerMove() {
        return board.hasLegalMove(currentPlayer);
    }
    public int[] getMoveFromStrategy() {
        return getActiveStrategy().chooseMove(currentPlayer, board);
    }
    public void makeMove(int[] move) {
        board.makeMove(move[0], move[1], currentPlayer);
    }
    public void nextPlayer() {
        currentPlayer = getNextPlayer(currentPlayer);
    }

    /**
     * Execute one complete turn: get move from strategy, make move, and switch player
     */
    public void playTurn() {
        if (isGameOver()) {
            return;
        }

        int[] move = getMoveFromStrategy();
        if (move != null) {
            makeMove(move);
        }
        nextPlayer();
    }

    public boolean isGameOver() {
        return currentPlayer == null;
    }
    public int getScore(Color color) {
        return board.getScore(color);
    }
    public Color getWinner() {
        int black = getScore(Color.BLACK);
        int white = getScore(Color.WHITE);
        if (black > white) return Color.BLACK;
        if (white > black) return Color.WHITE;
        return null;
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
