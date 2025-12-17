package view;

import java.util.Scanner;

public class MenuView {
    private final Scanner scanner = new Scanner(System.in);

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
}
