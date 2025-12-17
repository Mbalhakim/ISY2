import core.Game;
import strategies.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("OTHELLO GAME");
  
        
        System.out.println("Kies spel modus:");
        System.out.println("1. Human vs Human");
        System.out.println("2. Human vs Random AI");
        System.out.println("3. Random vs Random");
        
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        System.out.print("\nKeuze (1-3): ");
        
        int choice = 1;
        try {
            choice = scanner.nextInt();
        } catch (Exception e) {
            System.out.println("Ongeldige invoer, gebruik Human vs Human");
            scanner.nextLine();
        }
        
        Game game;
        
        switch (choice) {
            case 1:
                System.out.println("\n▶ Human (BLACK) vs Human (WHITE)\n");
                game = new Game(new HumanStrategy(), new HumanStrategy());
                break;
            case 2:
                System.out.println("\n▶ Human (BLACK) vs Random AI (WHITE)\n");
                game = new Game(new HumanStrategy(), new RandomStrategy());
                break;
            case 3:
                System.out.println("\n▶ Random AI (BLACK) vs Random AI (WHITE)\n");
                game = new Game(new RandomStrategy(), new RandomStrategy());
                break;
            default:
                System.out.println("\nOngeldige keuze, gebruik Human vs Human\n");
                game = new Game(new HumanStrategy(), new HumanStrategy());
        }
        
        game.play();
    }
}