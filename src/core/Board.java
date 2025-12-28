package core;

import enums.Color;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private int SIZE = 8;

    // 8 richtingen
    private int[][] DIRECTIONS = {
        {-1, 0}, {-1, 1}, {0, 1}, {1, 1}, {1, 0}, {1, -1}, {0, -1}, {-1, -1}
    };

    // 2D bord: [rij][kolom]
    private Color[][] grid;

    public Board() {
        grid = new Color[SIZE][SIZE];
        // startpositie
        grid[3][3] = Color.WHITE;
        grid[3][4] = Color.BLACK;
        grid[4][3] = Color.BLACK;
        grid[4][4] = Color.WHITE;
    }

    public void printBoard() {
        // breedte voor rij-nummers links
        int labelWidth = String.format("%2d |", 0).length();
        for (int i = 0; i < labelWidth; i++) System.out.print(' ');

        // kolomnummers bovenaan
        for (int c = 0; c < SIZE; c++) {
            String label = center(Integer.toString(c + 1), 1);
            System.out.printf(" %s |", label);
        }
        System.out.println();

        // bovenste scheidingslijn
        for (int i = 0; i < labelWidth - 1; i++) System.out.print(' ');
        System.out.print('+');
        for (int c = 0; c < SIZE; c++) {
            System.out.print("---+");
        }
        System.out.println();

        // rijen
        for (int r = 0; r < SIZE; r++) {
            // rij-nummer
            System.out.printf("%2d |", r + 1);
            for (int c = 0; c < SIZE; c++) {
                Color color = grid[r][c];
                String sym = getPrintableSymbol(color);
                System.out.printf(" %s |", sym);
            }
            System.out.println();

            // scheidingslijn na elke rij
            for (int i = 0; i < labelWidth - 1; i++) System.out.print('-');
            System.out.print('+');
            for (int c = 0; c < SIZE; c++) {
                System.out.print("---+");
            }
            System.out.println();
        }
    }

    // simpele helper om tekst te centreren
    private String center(String s, int width) {
        if (s == null) s = "";
        int len = s.codePointCount(0, s.length());
        if (len >= width) return s;
        int pad = width - len;
        int left = pad / 2;
        int right = pad - left;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < left; i++) sb.append(' ');
        sb.append(s);
        for (int i = 0; i < right; i++) sb.append(' ');
        return sb.toString();
    }

    // kiest het juiste teken voor een vakje
    private String getPrintableSymbol(Color color) {
        if (color == Color.BLACK) return "○";
        if (color == Color.WHITE) return "●";
        return " ";
    }

    private boolean isInBounds(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    // checkt of stenen worden ingesloten
    private int[] findBracket(int row, int col, Color player, int[] direction) {
        int r = row + direction[0];
        int c = col + direction[1];

        if (!isInBounds(r, c) || grid[r][c] == player) {
            return null;
        }

        Color opponent = player.opposite();
        while (isInBounds(r, c) && grid[r][c] == opponent) {
            r += direction[0];
            c += direction[1];
        }

        if (!isInBounds(r, c) || grid[r][c] == null) {
            return null;
        }

        return new int[]{r, c};
    }

    public boolean isLegalMove(int row, int col, Color player) {
        if (!isInBounds(row, col) || grid[row][col] != null) {
            return false;
        }

        for (int[] dir : DIRECTIONS) {
            if (findBracket(row, col, player, dir) != null) {
                return true;
            }
        }
        return false;
    }

    // geeft alle geldige zetten
    public List<int[]> getLegalMoves(Color player) {
        List<int[]> moves = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (isLegalMove(r, c, player)) {
                    moves.add(new int[]{r, c});
                }
            }
        }
        return moves;
    }

    public boolean hasLegalMove(Color player) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (isLegalMove(r, c, player)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void makeMove(int row, int col, Color player) {
        if (!isLegalMove(row, col, player)) {
            throw new IllegalArgumentException("Ongeldige zet!");
        }

        grid[row][col] = player;

        // draai alle stenen om
        for (int[] dir : DIRECTIONS) {
            makeFlips(row, col, player, dir);
        }
    }

    private void makeFlips(int row, int col, Color player, int[] direction) {
        int[] bracket = findBracket(row, col, player, direction);
        if (bracket == null) {
            return;
        }

        int r = row + direction[0];
        int c = col + direction[1];

        while (r != bracket[0] || c != bracket[1]) {
            grid[r][c] = player;
            r += direction[0];
            c += direction[1];
        }
    }

    public int getScore(Color player) {
        int count = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == player) {
                    count++;
                }
            }
        }
        return count;
    }

    public Color getCell(int row, int col) {
        return grid[row][col];
    }
}
