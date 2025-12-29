package strategies;

import interfaces.Strategy;
import core.Board;
import enums.Color;
import java.util.List;

public class MinimaxStrategy implements Strategy {
    private final int maxDepth;

    public MinimaxStrategy(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public int[] chooseMove(Color player, Board board) {
        List<int[]> legalMoves = board.getLegalMoves(player);

        if (legalMoves.isEmpty()) {
            return null;
        }

        int bestValue = Integer.MIN_VALUE;
        int[] bestMove = null;

        for (int[] move : legalMoves) {
            Board newBoard = copyBoard(board);
            newBoard.makeMove(move[0], move[1], player);

            int value = minimax(newBoard, maxDepth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, player.opposite(), false);

            if (value > bestValue) {
                bestValue = value;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private int minimax(Board board, int depth, int alpha, int beta, Color currentPlayer, boolean maximizing) {
        if (depth == 0 || !board.hasLegalMove(currentPlayer) && !board.hasLegalMove(currentPlayer.opposite())) {
            return evaluateBoard(board, currentPlayer);
        }

        List<int[]> legalMoves = board.getLegalMoves(currentPlayer);

        if (legalMoves.isEmpty()) {
            
            return minimax(board, depth - 1, alpha, beta, currentPlayer.opposite(), !maximizing);
        }

        if (maximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int[] move : legalMoves) {
                Board newBoard = copyBoard(board);
                newBoard.makeMove(move[0], move[1], currentPlayer);

                int eval = minimax(newBoard, depth - 1, alpha, beta, currentPlayer.opposite(), false);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) {
                    break;
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int[] move : legalMoves) {
                Board newBoard = copyBoard(board);
                newBoard.makeMove(move[0], move[1], currentPlayer);

                int eval = minimax(newBoard, depth - 1, alpha, beta, currentPlayer.opposite(), true);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) {
                    break;
                }
            }
            return minEval;
        }
    }

    private int evaluateBoard(Board board, Color player) {
        Color opponent = player.opposite();

       
        int pieceCount = board.getScore(player) - board.getScore(opponent);

       
        int cornerValue = 0;
        int[][] corners = {{0, 0}, {0, 7}, {7, 0}, {7, 7}};
        for (int[] corner : corners) {
            if (board.getCell(corner[0], corner[1]) == player) {
                cornerValue += 10;
            } else if (board.getCell(corner[0], corner[1]) == opponent) {
                cornerValue -= 10;
            }
        }

      
        int mobility = board.getLegalMoves(player).size() - board.getLegalMoves(opponent).size();

       
        return pieceCount + cornerValue + mobility;
    }

    private Board copyBoard(Board original) {
        return original.copy();
    }

    @Override
    public String getName() {
        return "Minimax AI (Depth " + maxDepth + ")";
    }
}