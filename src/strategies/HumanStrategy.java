package strategies;

import interfaces.Strategy;
import core.Board;
import enums.Color;
import java.util.List;
import java.util.Scanner;

public class HumanStrategy implements Strategy {
    private final Scanner scanner;
    
    public HumanStrategy() {
        this.scanner = new Scanner(System.in);
    }
    
    @Override
    public int[] chooseMove(Color player, Board board) {
        List<int[]> legalMoves = board.getLegalMoves(player);
        
        System.out.println("\nJouw legale zetten:");
        for (int i = 0; i < legalMoves.size(); i++) {
            int[] move = legalMoves.get(i);
            System.out.println((i+1) + ". Rij " + (move[0]+1) + ", Kolom " + (move[1]+1));
        }
        
        while (true) {
            try {
                System.out.print("Voer zet in (bijv. 27 voor rij 2, kolom 7): ");
                int input = scanner.nextInt();
                
                int row = (input / 10) - 1;  // Eerste cijfer = rij
                int col = (input % 10) - 1;  // Laatste cijfer = kolom
                
                if (board.isLegalMove(row, col, player)) {
                    return new int[]{row, col};
                }
                
                System.out.println("Ongeldige zet! Probeer opnieuw.");
            } catch (Exception e) {
                System.out.println("Ongeldige invoer!");
                scanner.nextLine();
            }
        }
    }
    
    @Override
    public String getName() {
        return "Human";
    }
}
