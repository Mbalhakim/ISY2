package controller;

import java.util.Scanner;

public class InputHandler {
    private final Scanner scanner;

    public InputHandler() {
        scanner = new Scanner(System.in);
    }

    public int[] getMoveInput() {
        System.out.print("Voer zet in (rij kolom, bijv: 3 4): ");

        try {
            int row = scanner.nextInt() - 1; // to 0-based
            int col = scanner.nextInt() - 1;

            if (row < 0 || row > 7 || col < 0 || col > 7) {
                System.out.println("Rij en kolom moeten tussen 1 en 8 liggen.");
                return null;
            }

            return new int[]{row, col};

        } catch (Exception e) {
            System.out.println("Ongeldige invoer. Gebruik: rij kolom (bijv: 3 4)");
            scanner.nextLine(); // clear buffer
            return null;
        }
    }
}
