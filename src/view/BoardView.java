package view;

import core.Board;
import enums.Color;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoardView {
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String LEGAL_MOVE_MARKER = "+";
    private static final boolean ANSI_ENABLED = System.getenv("NO_COLOR") == null;

    public void printBoard(Board board) {
        printBoard(board, null);
    }

    /**
     * Prints the board. If legalMoves is provided, all EMPTY squares that are legal moves
     * will be shown in green.
     */
    public void printBoard(Board board, List<int[]> legalMoves) {
        Set<String> legal = null;
        if (legalMoves != null && !legalMoves.isEmpty()) {
            legal = new HashSet<>();
            for (int[] mv : legalMoves) {
                if (mv != null && mv.length >= 2) {
                    legal.add(mv[0] + "," + mv[1]);
        }
    }
}


        int size = board.getSize();

        // column header
        System.out.print("   ");
        for (int c = 0; c < size; c++) {
            System.out.printf(" %s |", center(Integer.toString(c + 1), 1));
        }
        System.out.println();

        // top border
        System.out.print("---+");
        for (int c = 0; c < size; c++) System.out.print("---+");
        System.out.println();

        // rows
        for (int r = 0; r < size; r++) {
            System.out.printf("%2d |", r + 1);

            for (int c = 0; c < size; c++) {
                Color color = board.getCell(r, c);

                String sym;
                boolean empty = (color == null || color == Color.EMPTY);
                if (empty && legal != null && legal.contains(r + "," + c)) {
                    sym = green(LEGAL_MOVE_MARKER);
                } else {
                    sym = getPrintableSymbol(color);
                }

                System.out.printf(" %s |", sym);
            }
            System.out.println();

            System.out.print("---+");
            for (int c = 0; c < size; c++) System.out.print("---+");
            System.out.println();
        }
    }

    private String green(String s) {
        if (!ANSI_ENABLED) return s;
        return ANSI_GREEN + s + ANSI_RESET;
    }

    // Centers a string inside a fixed width (simple helper for headers)
    private String center(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s;

        int totalPadding = width - s.length();
        int left = totalPadding / 2;
        int right = totalPadding - left;

        return " ".repeat(left) + s + " ".repeat(right);
    }

    private String getPrintableSymbol(Color color) {
        if (color == null || color == Color.EMPTY) return " ";
        if (color == Color.BLACK) return "○";
        if (color == Color.WHITE) return "●";
        return " ";
    }
}
