package strategies;

import interfaces.Strategy;
import core.Board;
import enums.Color;
import java.util.List;
import java.util.Random;

public class RandomStrategy implements Strategy {
    private final Random random;
    
    public RandomStrategy() {
        this.random = new Random();
    }
    
    @Override
    public int[] chooseMove(Color player, Board board) {
        List<int[]> legalMoves = board.getLegalMoves(player);
        
        if (legalMoves.isEmpty()) {
            return null;
        }
        
        return legalMoves.get(random.nextInt(legalMoves.size()));
    }
    
    @Override
    public String getName() {
        return "Random AI";
    }
}
