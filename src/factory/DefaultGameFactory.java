package factory;

import core.Game;
import interfaces.GameFactory;
import strategies.HumanStrategy;
import strategies.RandomStrategy;

public class DefaultGameFactory implements GameFactory {
    @Override
    public Game createGame(int choice) {
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

    @Override
    public String[] getPlayerTypes(int choice) {
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
}
