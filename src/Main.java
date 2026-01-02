import controller.GameController;

public class Main {
    public static void main(String[] args) {
        GameController controller = new GameController();
        if (args.length > 0 && "online".equals(args[0])) {
            controller.startOnlineGame();
        } else {
            controller.startGame();
        }
    }
}