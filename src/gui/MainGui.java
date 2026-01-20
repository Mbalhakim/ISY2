package gui;

import core.Game;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import factory.DefaultGameFactory;
import interfaces.GameFactory;
import controller.OnlineGameController;
import view.BoardView;
import view.GameView;
import strategies.HumanStrategy;

public class MainGui extends Application {
    private Game game;
    private GuiGameController guiController;

    @Override
    public void start(Stage stage) {
        // Factory to create games based on user choice (reuse existing factory logic)
        GameFactory gameFactory = new DefaultGameFactory();

        // UI elements
        Label infoLabel = new Label("Choose mode and press Start");
        Label turnLabel = new Label("");

        ComboBox<String> modeSelector = new ComboBox<>();
        modeSelector.getItems().addAll(
            "1 - Human vs Human",
            "2 - Human vs Random AI",
            "3 - Human vs Minimax AI",
            "4 - Random AI vs Random AI",
            "5 - Minimax AI vs Minimax AI",
            "6 - Minimax AI vs Random AI",
            "7 - Online (multiplayer)"
        );
        modeSelector.setValue(modeSelector.getItems().get(0));

        // update info label when selection changes
        modeSelector.setOnAction(ev -> infoLabel.setText("Selected: " + modeSelector.getValue()));

        Button startBtn = new Button("Start Game");
        HBox topBar = new HBox(10, new Label("Mode:"), modeSelector, startBtn, infoLabel, turnLabel);
        topBar.setPadding(new Insets(8));

        // board pane ready but board will be drawn after Start
        BoardPane boardPane = new BoardPane(8, 60, (r, c) -> {
            // click callback -> delegate to controller if present
            if (guiController != null) guiController.onCellClicked(r, c);
        });

        // Use a StackPane as the center so Canvas can be sized reliably
        StackPane centerStack = new StackPane(boardPane);

        BorderPane root = new BorderPane();
        // Put the topBar in the top so the center region can be sized for the board
        root.setTop(topBar);
        root.setCenter(centerStack);

        Scene scene = new Scene(root);
        stage.setTitle("Othello - GUI");
        stage.setScene(scene);
        stage.setWidth(640);
        stage.setHeight(520);
        stage.show();

        // Allow the BoardPane to grow and shrink with the available center area.
        boardPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        centerStack.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Bind the BoardPane preferred size to the StackPane size so layout can freely enlarge/shrink it.
        boardPane.prefWidthProperty().bind(centerStack.widthProperty());
        boardPane.prefHeightProperty().bind(centerStack.heightProperty());

        // Start button behavior: create Game via factory and set up controller
        startBtn.setOnAction(ev -> {
            String selected = modeSelector.getValue();
            int choice = 1;
            try {
                if (selected != null) {
                    String numPart = selected.split(" - ", 2)[0].trim();
                    choice = Integer.parseInt(numPart);
                }
            } catch (Exception ex) {
                infoLabel.setText("Invalid selection, using mode 1");
                choice = 1;
            }

            if (choice == 7) {
                // Start online controller (console-driven). This will print to the IDE run console and read stdin from there.
                infoLabel.setText("Starting online play (console I/O). Check the run console to enter your name and moves...");
                // create a local Game backing (board state) and start OnlineGameController
                Game onlineGame = new Game(new HumanStrategy(), new HumanStrategy());
                BoardView bv = new BoardView();
                GameView gv = new GameView();
                OnlineGameController ogc = new OnlineGameController(onlineGame, bv, gv, new HumanStrategy());
                new Thread(() -> ogc.play()).start();

                // clear GUI controller since online play uses console view
                guiController = null;
                return;
            }

            // create game with chosen mode (local play)
            game = gameFactory.createGame(choice);
            if (game == null) {
                infoLabel.setText("Failed to create game for mode " + choice);
                return;
            }
            // create GUI controller with turn label
            guiController = new GuiGameController(game, boardPane, turnLabel);

            // initial draw + possible immediate AI moves
            guiController.refreshBoard();

            // show player types in info label for clarity
            String[] types = gameFactory.getPlayerTypes(choice);
            infoLabel.setText("Game started: " + selected + " — " + types[0] + " vs " + types[1]);
        });

        // Initial empty draw
        boardPane.drawEmpty();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        // clean up threads/connections if any
    }

    public static void main(String[] args) {
        launch(args);
    }
}
