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
    
    // 2D grid: [row][col]
    private Color[][] grid;
    
    public Board() {
        grid = new Color[SIZE][SIZE];
        // start positie
        grid[3][3] = Color.WHITE;
        grid[3][4] = Color.BLACK;
        grid[4][3] = Color.BLACK;
        grid[4][4] = Color.WHITE;
    }

    
   public void printBoard() {
        System.out.println("   1  2  3  4  5  6  7  8");
        for (int r = 0; r < SIZE; r++) {
            System.out.print((r + 1) + " ");
            for (int c = 0; c < SIZE; c++) {
                Color color = grid[r][c];
                if (color == Color.BLACK) {
                    System.out.print(" B ");
                } else if (color == Color.WHITE) {
                    System.out.print(" W ");
                } else {
                    System.out.print("[ ]");
                }
            }
            System.out.println();
        }
    }
    

    private boolean isInBounds(int row, int col) {

        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }
    
    // vind bracket positie in een richting
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
    
    // krijg alle legale zetten
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
        
        // flip alle stenen
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
}