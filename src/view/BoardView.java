package view;

import core.Board;
import enums.Color;

public class BoardView {
    public void printBoard(Board board) {
        int SIZE = board.getSize();
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
                Color color = board.getCell(r, c);
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

}
