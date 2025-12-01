import enums.Color;

public class Move {
    public int row;
    public int col;
    public Color color;

    public Move(int row, int col, Color color) {
        this.row = row;
        this.col = col;
        this.color = color;
    }
}
