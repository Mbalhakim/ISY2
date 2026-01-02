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

    public Board copy() {
        Board newBoard = new Board();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                newBoard.grid[r][c] = this.grid[r][c];
            }
        }
        return newBoard;
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

    public int getSize() {
        return SIZE;
    }

    public static int[] stringToMove(String move) {
        if (move == null || move.length() != 2) return null;
        try {
            int row = Integer.parseInt(move.substring(0, 1)) - 1;
            int col = Integer.parseInt(move.substring(1, 2)) - 1;
            if (row < 0 || row >= 8 || col < 0 || col >= 8) return null;
            return new int[]{row, col};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
