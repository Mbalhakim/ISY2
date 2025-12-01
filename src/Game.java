import enums.Color;

public class Game {

    private Board board;
    private Player p1;
    private Player p2;
    private Color currentTurn;

    public Game(Player p1, Player p2) {
        this.board = new Board();
        this.p1 = p1;
        this.p2 = p2;
        this.currentTurn = Color.BLACK;
    }


}
