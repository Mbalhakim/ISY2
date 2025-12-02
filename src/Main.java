import enums.Color;

public class Main {
    public static void main(String[] args) {

        Player one = new HumanPlayer("playerOne", Color.BLACK);
        Player two = new HumanPlayer("playerTwo", Color.WHITE);
        Player AIOne = new AIPlayer("AIOne", Color.BLACK);
        Player AITwo = new AIPlayer("AITwo", Color.WHITE);

        System.out.println(one.getName() +" " +one.getColor());
        System.out.println("kleur van de tegenstander " + one.getColor().opposite());

        System.out.println(two.getName() +" " + two.getColor() );
        System.out.println(AIOne);
        System.out.println(AITwo);

        Cell cell = new Cell(3, 5);
        System.out.println("Row:" + cell.getRow());
        System.out.println("Col:" + cell.getCol());

        System.out.println("Is empty? " + cell.isEmpty());


        cell.setColor(Color.BLACK);


        System.out.println("Color van cell is: " + cell.getColor());


        System.out.println("Is empty? " + cell.isEmpty());


        cell.setColor(Color.WHITE);
        System.out.println("Color van cell is : " + cell.getColor());

        Board board = new Board();

    }
}