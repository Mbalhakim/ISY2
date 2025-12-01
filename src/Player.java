import enums.Color;

public interface Player {
    String getName();
    Color getColor();
    Move getMove(Board board);
}