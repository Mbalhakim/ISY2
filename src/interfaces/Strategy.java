package interfaces;

import core.Board;
import enums.Color;

/**
 * Interface voor spel strategieën.
 */
public interface Strategy {

    int[] chooseMove(Color player, Board board);
    
    String getName();
}
