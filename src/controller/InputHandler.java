package controller;

import java.util.Scanner;

public class InputHandler {
    private final Scanner scanner;

    public InputHandler() {
        scanner = new Scanner(System.in);
    }

    public int getMenuChoice() {
        System.out.println("Kies spel modus:");
        System.out.println("1. Human vs Human");
        System.out.println("2. Human vs Random AI");
        System.out.println("3. Random vs Random");
        System.out.print("\nKeuze (1-3): ");
        int choice = 1;
        try {
            choice = scanner.nextInt();
        } catch (Exception e) {
            System.out.println("Ongeldige invoer, gebruik Human vs Human");
            scanner.nextLine();
        }
        return choice;
    }

    public int[] getMoveInput() {
        System.out.print("Voer zet in (bijv. 27 voor rij 2, kolom 7): ");
        try {
            int input = scanner.nextInt();
            int row = (input / 10) - 1;
            int col = (input % 10) - 1;
            return new int[]{row, col};
        } catch (Exception e) {
            System.out.println("Ongeldige invoer!");
            scanner.nextLine();
            return null;
        }
    }
}
