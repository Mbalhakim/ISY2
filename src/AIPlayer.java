import enums.Color;
import java.util.Random;

public class AIPlayer implements Player {

    private String name;
    private Color color;
    private Random rng = new Random();

    public AIPlayer(String name, Color color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getName() { return name; }

    @Override
    public Color getColor() { return color; }

    @Override
    public Move getMove(Board board) {
        return null;
    }


}
