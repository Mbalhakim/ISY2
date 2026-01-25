package gui;

import core.Game;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import factory.DefaultGameFactory;
import interfaces.GameFactory;
import interfaces.Strategy;
import strategies.HumanStrategy;
import strategies.MinimaxStrategy;
import strategies.RandomStrategy;

public class MainGui extends Application {
    private Game game;
    private GuiGameController guiController;
    private GuiOnlineGameController onlineController;

    @Override
    public void start(Stage stage) {
        // Factory to create games based on user choice (reuse existing factory logic)
        GameFactory gameFactory = new DefaultGameFactory();

        // Title Label
        Label titleLabel = new Label("🎮 OTHELLO GAME 🎮");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.DARKBLUE);
        titleLabel.setAlignment(Pos.CENTER);

        // Info label with better styling
        Label infoLabel = new Label("Welcome! Select a game mode and click Start to begin playing.");
        infoLabel.setFont(Font.font("Arial", 12));
        infoLabel.setWrapText(true);
        infoLabel.setMaxWidth(500);

        // Turn label with better styling
        Label turnLabel = new Label("Turn: Not Started");
        turnLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        turnLabel.setTextFill(Color.DARKGREEN);

        // Status label with better styling
        Label statusLabel = new Label("Ready to start");
        statusLabel.setFont(Font.font("Arial", 12));
        statusLabel.setTextFill(Color.DARKSLATEGRAY);

        // Game mode selector with better width
        ComboBox<String> modeSelector = new ComboBox<>();
        modeSelector.getItems().addAll(
            "Human vs Human",
            "Human vs Random AI",
            "Human vs Minimax AI",
            "Random AI vs Random AI",
            "Minimax AI vs Minimax AI",
            "Minimax AI vs Random AI",
            "Online Multiplayer"
        );
        modeSelector.setValue(modeSelector.getItems().get(0));
        modeSelector.setPrefWidth(200);

        // Online strategy selector (only visible for online mode)
        Label onlineStrategyLabel = new Label("Your Strategy:");
        ComboBox<String> onlineStrategySelector = new ComboBox<>();
        onlineStrategySelector.getItems().addAll(
            "Human Player",
            "Random AI",
            "Minimax AI"
        );
        onlineStrategySelector.setValue("Human Player");
        onlineStrategySelector.setPrefWidth(150);

        // Initially hide online strategy selector
        onlineStrategyLabel.setVisible(false);
        onlineStrategyLabel.setManaged(false);
        onlineStrategySelector.setVisible(false);
        onlineStrategySelector.setManaged(false);

        // Mode descriptions for user help
        Label modeDescLabel = new Label(getModeDescription(modeSelector.getValue()));
        modeDescLabel.setFont(Font.font("Arial", 11));
        modeDescLabel.setTextFill(Color.GRAY);
        modeDescLabel.setWrapText(true);
        modeDescLabel.setMaxWidth(400);

        // update description when selection changes
        modeSelector.setOnAction(e -> {
            modeDescLabel.setText(getModeDescription(modeSelector.getValue()));
            // Show/hide online strategy selector based on mode
            boolean isOnlineMode = modeSelector.getValue().contains("Online");
            onlineStrategyLabel.setVisible(isOnlineMode);
            onlineStrategyLabel.setManaged(isOnlineMode);
            onlineStrategySelector.setVisible(isOnlineMode);
            onlineStrategySelector.setManaged(isOnlineMode);
        });

        // Start button with better styling
        Button startBtn = new Button("▶ Start Game");
        startBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        startBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8 20;");
        startBtn.setOnMouseEntered(e -> startBtn.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-padding: 8 20;"));
        startBtn.setOnMouseExited(e -> startBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8 20;"));

        // Reset button
        Button resetBtn = new Button("🔄 Reset");
        resetBtn.setFont(Font.font("Arial", 12));
        resetBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-padding: 8 15;");
        resetBtn.setOnMouseEntered(e -> resetBtn.setStyle("-fx-background-color: #e68900; -fx-text-fill: white; -fx-padding: 8 15;"));
        resetBtn.setOnMouseExited(e -> resetBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-padding: 8 15;"));

        // Top bar with better layout
        HBox modeBox = new HBox(10, new Label("Game Mode:"), modeSelector, onlineStrategyLabel, onlineStrategySelector, startBtn, resetBtn);
        modeBox.setAlignment(Pos.CENTER_LEFT);
        modeBox.setPadding(new Insets(10));

        VBox topSection = new VBox(10, titleLabel, modeBox, modeDescLabel, new Separator());
        topSection.setPadding(new Insets(10));
        topSection.setStyle("-fx-background-color: #f0f0f0;");

        // Status bar with better styling
        HBox statusBar = new HBox(20, turnLabel, new Separator(), statusLabel);
        statusBar.setAlignment(Pos.CENTER);
        statusBar.setPadding(new Insets(10));
        statusBar.setStyle("-fx-background-color: #e8e8e8; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");

        // Instructions panel
        VBox instructionsBox = new VBox(5);
        instructionsBox.setAlignment(Pos.CENTER);
        Label instTitle = new Label("How to Play:");
        instTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        Label inst1 = new Label("• Click on a highlighted green cell to place your piece");
        Label inst2 = new Label("• Capture opponent pieces by surrounding them");
        Label inst3 = new Label("• The game ends when no moves are available");
        inst1.setFont(Font.font("Arial", 11));
        inst2.setFont(Font.font("Arial", 11));
        inst3.setFont(Font.font("Arial", 11));
        instructionsBox.getChildren().addAll(instTitle, inst1, inst2, inst3);
        instructionsBox.setPadding(new Insets(10));
        instructionsBox.setStyle("-fx-background-color: #fffacd; -fx-border-color: #daa520; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");

        // board pane ready but board will be drawn after Start
        BoardPane boardPane = new BoardPane(8, 60, (r, c) -> {
            // click callback -> delegate to controller if present
            if (guiController != null) {
                guiController.onCellClicked(r, c);
            } else if (onlineController != null) {
                onlineController.onCellClicked(r, c);
            }
        });

        // Use a StackPane as the center so Canvas can be sized reliably
        StackPane centerStack = new StackPane(boardPane);
        centerStack.setPadding(new Insets(10));
        centerStack.setStyle("-fx-background-color: #ffffff;");

        // Info panel on the right
        VBox infoPanel = new VBox(15, instructionsBox, infoLabel);
        infoPanel.setPadding(new Insets(10));
        infoPanel.setPrefWidth(250);
        infoPanel.setStyle("-fx-background-color: #fafafa; -fx-border-color: #cccccc; -fx-border-width: 0 0 0 1;");

        BorderPane root = new BorderPane();
        root.setTop(topSection);
        root.setCenter(centerStack);
        root.setRight(infoPanel);
        root.setBottom(statusBar);

        Scene scene = new Scene(root);
        stage.setTitle("Othello - Reversi Game");
        stage.setScene(scene);
        stage.setWidth(900);
        stage.setHeight(700);
        stage.setMinWidth(700);
        stage.setMinHeight(600);
        stage.show();

        // Allow the BoardPane to grow and shrink with the available center area.
        boardPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        centerStack.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Bind the BoardPane preferred size to the StackPane size so layout can freely enlarge/shrink it.
        boardPane.prefWidthProperty().bind(centerStack.widthProperty());
        boardPane.prefHeightProperty().bind(centerStack.heightProperty());

        // Reset button behavior
        resetBtn.setOnAction(e -> {
            // Cleanup existing controllers
            if (onlineController != null) {
                onlineController.cleanup();
                onlineController = null;
            }
            guiController = null;
            game = null;

            // Reset UI
            boardPane.drawEmpty();
            turnLabel.setText("Turn: Not Started");
            statusLabel.setText("Ready to start");
            infoLabel.setText("Game reset. Select a mode and click Start to begin a new game.");
        });

        // Start button behavior: create Game via factory and set up controller
        startBtn.setOnAction(e -> {
            // Cleanup any existing game completely
            if (onlineController != null) {
                onlineController.cleanup();
                onlineController = null;
            }
            guiController = null;
            game = null;

            // Clear the board before starting new game
            boardPane.drawEmpty();

            String selected = modeSelector.getValue();
            int choice = getModeChoice(selected);

            if (choice == 7) {
                // Get selected strategy for online play
                Strategy selectedStrategy = getOnlineStrategy(onlineStrategySelector.getValue());
                String strategyName = onlineStrategySelector.getValue();

                // Start online GUI controller
                String instruction = strategyName.equals("Human Player")
                    ? "Click on highlighted cells when it's your turn!"
                    : "Your " + strategyName + " will play automatically when it's your turn!";

                infoLabel.setText("🌐 Connecting to online game server...\n\nYou will be prompted to enter your player name, then matched with an opponent.\n\n" + instruction);
                statusLabel.setText("Initializing online connection...");
                turnLabel.setText("Turn: Waiting for match...");

                // Create a local Game backing (board state)
                Game onlineGame = new Game(new HumanStrategy(), new HumanStrategy());

                // Create GUI online controller with selected strategy
                onlineController = new GuiOnlineGameController(
                    onlineGame,
                    boardPane,
                    turnLabel,
                    statusLabel,
                    selectedStrategy
                );

                // Clear local game controller since online play uses its own controller
                guiController = null;

                // Draw initial empty board
                boardPane.drawEmpty();

                // Start online game on background thread
                onlineController.start();
                return;
            }

            // create game with chosen mode (local play)
            game = gameFactory.createGame(choice);
            if (game == null) {
                infoLabel.setText("❌ Failed to create game for mode " + choice + ". Please try again.");
                statusLabel.setText("Error creating game");
                return;
            }
            // create GUI controller with turn label
            guiController = new GuiGameController(game, boardPane, turnLabel);

            // initial draw + possible immediate AI moves
            guiController.refreshBoard();

            // show player types in info label for clarity
            String[] types = gameFactory.getPlayerTypes(choice);
            infoLabel.setText("✅ Game started!\n\nMode: " + selected + "\n" + types[0] + " vs " + types[1] + "\n\n" +
                            (types[0].contains("Human") ? "Your turn! Click on a highlighted green cell to make your move." : "AI is thinking..."));
            statusLabel.setText("Game in progress");
        });

        // Initial empty draw
        boardPane.drawEmpty();
    }

    private Strategy getOnlineStrategy(String strategyName) {
        if (strategyName == null) return new HumanStrategy();
        if (strategyName.contains("Random AI")) {
            return new RandomStrategy();
        } else if (strategyName.contains("Minimax AI")) {
            return new MinimaxStrategy(4); // depth 4 for good performance
        }
        return new HumanStrategy(); // Default to human
    }

    private String getModeDescription(String mode) {
        if (mode == null) return "";
        if (mode.contains("Human vs Human")) {
            return "Two players take turns on the same computer. Perfect for playing with a friend!";
        } else if (mode.contains("Human vs Random AI")) {
            return "Play against a computer that makes random moves. Good for beginners!";
        } else if (mode.contains("Human vs Minimax AI")) {
            return "Challenge yourself against a smart AI that thinks ahead. Recommended for experienced players!";
        } else if (mode.contains("Random AI vs Random AI")) {
            return "Watch two random AIs compete. Great for demonstration!";
        } else if (mode.contains("Minimax AI vs Minimax AI")) {
            return "Watch two smart AIs battle it out. See optimal strategy in action!";
        } else if (mode.contains("Minimax AI vs Random AI")) {
            return "Watch a smart AI dominate a random opponent. Educational viewing!";
        } else if (mode.contains("Online Multiplayer")) {
            return "Play against another player over the network. Requires game server to be running!";
        }
        return "Select a game mode to see details.";
    }

    private int getModeChoice(String mode) {
        if (mode == null) return 1;
        if (mode.contains("Human vs Human")) return 1;
        if (mode.contains("Human vs Random AI")) return 2;
        if (mode.contains("Human vs Minimax AI")) return 3;
        if (mode.contains("Random AI vs Random AI")) return 4;
        if (mode.contains("Minimax AI vs Minimax AI")) return 5;
        if (mode.contains("Minimax AI vs Random AI")) return 6;
        if (mode.contains("Online Multiplayer")) return 7;
        return 1;
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        // clean up threads/connections if any
        if (onlineController != null) {
            onlineController.cleanup();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
