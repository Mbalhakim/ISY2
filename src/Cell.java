import enums.Color;

public class Cell {

    private final int row;
    private final int col;
    private Color state;

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.state = Color.EMPTY;
    }

    public boolean isEmpty() {
        return state == Color.EMPTY;
    }

    public void setColor(Color color) {
        this.state = color;
    }

    public Color getColor() {
        return state;
    }

    public int getRow() { return row; }

    public int getCol() { return col; }


@Override
public String toString() {
    if (state == Color.EMPTY) {
        return "[ ]";
    } else if (state == Color.BLACK) {
        return "[B]";
    } else if (state == Color.WHITE) {
        return "[W]";
    }
    return "[?]";
}

}

