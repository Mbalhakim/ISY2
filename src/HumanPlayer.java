import enums.Color;

import java.util.Scanner;

public class HumanPlayer implements Player {

    private String name;
    private Color color;
    private Scanner scanner = new Scanner(System.in);

    public HumanPlayer(String name, Color color) {
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
