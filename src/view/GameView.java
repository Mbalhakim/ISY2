package view;

public class GameView {
    public void printStart() {
        System.out.println("OTHELLO GAME");
    }
    public void printGameOver() {
        System.out.println("\n========== GAME OVER ==========");
    }
    public void printWinner(String winner) {
        if (winner == null) {
            System.out.println("\nGELIJKSPEL");
        } else {
            System.out.println("\nWINNAAR: " + winner);
        }
    }
    public void printTurn(String player, String strategy) {
        System.out.println("\n=== " + player + " (" + strategy + ") ===");
    }
    public void printNoMoves(String player) {
        System.out.println(player + " heeft geen zetten. PASS.");
    }
    public void printMove(String player, int row, int col) {
        System.out.println(player + " speelt: Rij " + (row+1) + ", Kolom " + (col+1));
    }
}
