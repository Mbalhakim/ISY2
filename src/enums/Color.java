package enums;

public enum Color {
    BLACK,
    WHITE,
    EMPTY;

    public Color opposite() {
        switch (this) {
            case BLACK: return WHITE;
            case WHITE: return BLACK;
            default: return EMPTY;
        }
    }
}
