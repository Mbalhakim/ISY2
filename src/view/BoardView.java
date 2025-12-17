package view;

import core.Board;
import enums.Color;

public class BoardView {
    public void printBoard(Board board) {
        int SIZE = 8;
        System.out.println("   1  2  3  4  5  6  7  8");
        for (int r = 0; r < SIZE; r++) {
            System.out.print((r + 1) + " ");
            for (int c = 0; c < SIZE; c++) {
                Color color = board.getCell(r, c);
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
}
