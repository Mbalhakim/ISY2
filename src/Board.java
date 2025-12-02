
public class Board {

    private Cell[][] grid = new Cell[8][8];
    public Board() {
    for(int r = 0; r<8; r++){
        for (int c = 0; c<8; c++){
            grid[r][c] = new Cell(r, c);
        }
    }

}
}
