package gui;

import core.Game;
import javafx.application.Application;
import javafx.application.Platform;
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
        Label titleLabel = new Label("🎮 OTHELLO SPEL 🎮");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.DARKBLUE);
        titleLabel.setAlignment(Pos.CENTER);

        // Info label with better styling
        Label infoLabel = new Label("Welkom! Selecteer een spelmodus en klik op Start om te beginnen.");
        infoLabel.setFont(Font.font("Arial", 12));
        infoLabel.setWrapText(true);
        infoLabel.setMaxWidth(500);

        // Turn label with better styling
        Label turnLabel = new Label("Beurt: Niet Gestart");
        turnLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        turnLabel.setTextFill(Color.DARKGREEN);

        // Status label with better styling
        Label statusLabel = new Label("Klaar om te starten");
        statusLabel.setFont(Font.font("Arial", 12));
        statusLabel.setTextFill(Color.DARKSLATEGRAY);

        // Game mode selector with better width
        ComboBox<String> modeSelector = new ComboBox<>();
        modeSelector.getItems().addAll(
            "Mens vs Mens",
            "Mens vs Willekeurige AI",
            "Mens vs Minimax AI",
            "Willekeurige AI vs Willekeurige AI",
            "Minimax AI vs Minimax AI",
            "Minimax AI vs Willekeurige AI",
            "Online Multiplayer"
        );
        modeSelector.setValue(modeSelector.getItems().get(0));
        modeSelector.setPrefWidth(200);

        // Online strategy selector (only visible for online mode)
        Label onlineStrategyLabel = new Label("Jouw Strategie:");
        ComboBox<String> onlineStrategySelector = new ComboBox<>();
        onlineStrategySelector.getItems().addAll(
            "Menselijke Speler",
            "Willekeurige AI",
            "Minimax AI"
        );
        onlineStrategySelector.setValue("Menselijke Speler");
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
        Button startBtn = new Button("▶ Start Spel");
        startBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        startBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8 20;");
        startBtn.setOnMouseEntered(e -> startBtn.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-padding: 8 20;"));
        startBtn.setOnMouseExited(e -> startBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8 20;"));

        // Reset button
        Button resetBtn = new Button("🔄 Opnieuw");
        resetBtn.setFont(Font.font("Arial", 12));
        resetBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-padding: 8 15;");
        resetBtn.setOnMouseEntered(e -> resetBtn.setStyle("-fx-background-color: #e68900; -fx-text-fill: white; -fx-padding: 8 15;"));
        resetBtn.setOnMouseExited(e -> resetBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-padding: 8 15;"));

        // Top bar with better layout
        HBox modeBox = new HBox(10, new Label("Spelmodus:"), modeSelector, onlineStrategyLabel, onlineStrategySelector, startBtn, resetBtn);
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
        VBox infoPanel = new VBox(15, infoLabel);
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

            // Set references to null FIRST to stop any pending operations
            Game oldGame = game;
            GuiGameController oldController = guiController;

            game = null;
            guiController = null;

            // Give any pending UI operations a moment to complete with the old references
            Platform.runLater(() -> {
                // Reset UI
                boardPane.drawEmpty();
                turnLabel.setText("Beurt: Niet Gestart");
                statusLabel.setText("Klaar om te starten");
                infoLabel.setText("Spel gereset. Selecteer een modus en klik op Start om een nieuw spel te beginnen.");
            });
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
                String instruction = strategyName.equals("Menselijke Speler")
                    ? "Klik op gemarkeerde vakjes als het jouw beurt is!"
                    : "Jouw " + strategyName + " speelt automatisch als het jouw beurt is!";

                infoLabel.setText("🌐 Verbinden met online spelserver...\n\nJe wordt gevraagd je spelersnaam in te voeren, daarna word je gekoppeld aan een tegenstander.\n\n" + instruction);
                statusLabel.setText("Online verbinding initialiseren...");
                turnLabel.setText("Beurt: Wachten op tegenstander...");

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
                infoLabel.setText("❌ Kon geen spel maken voor modus " + choice + ". Probeer opnieuw.");
                statusLabel.setText("Fout bij maken spel");
                return;
            }
            // create GUI controller with turn label
            guiController = new GuiGameController(game, boardPane, turnLabel);

            // initial draw + possible immediate AI moves
            guiController.refreshBoard();

            // show player types in info label for clarity
            String[] types = gameFactory.getPlayerTypes(choice);
            infoLabel.setText("✅ Spel gestart!\n\nModus: " + selected + "\n" + types[0] + " vs " + types[1] + "\n\n" +
                            (types[0].contains("Mens") ? "Jouw beurt! Klik op een gemarkeerd groen vakje om je zet te doen." : "AI is aan het denken..."));
            statusLabel.setText("Spel bezig");
        });

        // Initial empty draw
        boardPane.drawEmpty();
    }

    private Strategy getOnlineStrategy(String strategyName) {
        if (strategyName == null) return new HumanStrategy();
        if (strategyName.contains("Willekeurige AI")) {
            return new RandomStrategy();
        } else if (strategyName.contains("Minimax AI")) {
            return new MinimaxStrategy(6); // depth 6 for good performance
        }
        return new HumanStrategy(); // Default to human
    }

    private String getModeDescription(String mode) {
        if (mode == null) return "";
        if (mode.contains("Mens vs Mens")) {
            return "Twee spelers spelen om de beurt op dezelfde computer. Perfect om met een vriend te spelen!";
        } else if (mode.contains("Mens vs Willekeurige AI")) {
            return "Speel tegen een computer die willekeurige zetten doet. Goed voor beginners!";
        } else if (mode.contains("Mens vs Minimax AI")) {
            return "Daag jezelf uit tegen een slimme AI die vooruit denkt. Aanbevolen voor ervaren spelers!";
        } else if (mode.contains("Willekeurige AI vs Willekeurige AI")) {
            return "Kijk hoe twee willekeurige AI's strijden. Ideaal voor demonstratie!";
        } else if (mode.contains("Minimax AI vs Minimax AI")) {
            return "Kijk hoe twee slimme AI's het uitvechten. Zie optimale strategie in actie!";
        } else if (mode.contains("Minimax AI vs Willekeurige AI")) {
            return "Kijk hoe een slimme AI een willekeurige tegenstander domineert. Leerzaam om te bekijken!";
        } else if (mode.contains("Online Multiplayer")) {
            return "Speel tegen een andere speler via het netwerk. Vereist dat de spelserver draait!";
        }
        return "Selecteer een spelmodus om details te zien.";
    }

    private int getModeChoice(String mode) {
        if (mode == null) return 1;
        if (mode.contains("Mens vs Mens")) return 1;
        if (mode.contains("Mens vs Willekeurige AI")) return 2;
        if (mode.contains("Mens vs Minimax AI")) return 3;
        if (mode.contains("Willekeurige AI vs Willekeurige AI")) return 4;
        if (mode.contains("Minimax AI vs Minimax AI")) return 5;
        if (mode.contains("Minimax AI vs Willekeurige AI")) return 6;
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
