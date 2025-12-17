package strategies;

import interfaces.Strategy;
import core.Board;
import enums.Color;
import java.util.List;
import controller.InputHandler;

public class HumanStrategy implements Strategy {
    private static final InputHandler inputHandler = new InputHandler();

    @Override
    public int[] chooseMove(Color player, Board board) {
        List<int[]> legalMoves = board.getLegalMoves(player);
        System.out.println("\nJouw legale zetten:");
        for (int i = 0; i < legalMoves.size(); i++) {
            int[] move = legalMoves.get(i);
            System.out.println((i+1) + ". Rij " + (move[0]+1) + ", Kolom " + (move[1]+1));
        }
        while (true) {
            int[] move = inputHandler.getMoveInput();
            if (move != null && board.isLegalMove(move[0], move[1], player)) {
                return move;
            }
            System.out.println("Ongeldige zet! Probeer opnieuw.");
        }
    }

    @Override
    public String getName() {
        return "Human";
    }
}
