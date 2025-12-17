package interfaces;

import core.Game;

public interface GameFactory {
    Game createGame(int choice);
    String[] getPlayerTypes(int choice);
}
