import controller.GameController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import model.Dictionary;
import model.Game;
import model.Player;
import utilities.GameConstants;
import view.GameView;
import view.MainMenuView;
import view.MainMenuView.MainMenuSettings;
import view.MainMenuView.PlayerSettings;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScrabbleApp extends Application {
    private static final Logger logger = Logger.getLogger(ScrabbleApp.class.getName());

    private Game game;
    private GameController gameController;
    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        try {
            primaryStage.setTitle("Scrabble");
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(1000);
            primaryStage.setOnCloseRequest(e -> cleanupResources());

            showMainMenu();
            primaryStage.show();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start the application", e);
            showErrorAndExit("Failed to start the application: " + e.getMessage());
        }
    }

    /**
     * Shows the main menu screen.
     */
    private void showMainMenu() {
        MainMenuView mainMenuView = new MainMenuView();
        mainMenuView.setOnStartGame(this::startGameWithSettings);

        Scene scene = new Scene(mainMenuView, 1024, 768);
        scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());

        primaryStage.setScene(scene);
    }

    /**
     * Starts the game with the selected settings from the main menu.
     *
     * @param settings The game settings selected by the user
     */
    private void startGameWithSettings(MainMenuSettings settings) {
        try {
            // Initialize game with selected settings
            initGame(settings.getPlayers(), settings.getDifficulty());

            // Create controller and view
            gameController = new GameController(game);
            GameView gameView = new GameView(gameController);

            // Apply additional settings
            gameController.setShowDefinitionsEnabled(settings.isShowDefinitions());

            // Set up scene and start game
            Scene scene = new Scene(gameView, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());

            primaryStage.setScene(scene);
            gameController.startGame();

            logger.info("Game started with " + settings.getPlayers().size() + " players and difficulty level: " + settings.getDifficulty());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start the game", e);
            showErrorAndExit("Failed to start the game: " + e.getMessage());
        }
    }

    /**
     * Initializes the game with multiple players and AI difficulty.
     *
     * @param playerSettings List of player settings from the menu
     * @param difficulty The AI difficulty level
     * @throws IOException If the dictionary cannot be loaded
     */
    private void initGame(List<PlayerSettings> playerSettings, int difficulty) throws IOException {
        InputStream dictionaryStream = Dictionary.loadDefaultDictionary();
        game = new Game(dictionaryStream, "Dictionary");

        // Add all players
        for (PlayerSettings settings : playerSettings) {
            Player player = new Player(settings.getName(), settings.isComputer());
            game.addPlayer(player);

            logger.info("Added player: " + player.getName() +
                    (player.isComputer() ? " (Computer)" : " (Human)"));
        }

        // Store difficulty in game for controller to use later
        game.setAiDifficulty(difficulty);

        logger.info("Game initialized with " + playerSettings.size() +
                " players and AI difficulty: " + difficulty);
    }

    private void showErrorAndExit(String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Application Error");
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("ERROR: " + message);
        }
        Platform.exit();
    }

    private void cleanupResources() {
        if (gameController != null) {
            gameController.shutdown();
        }
        logger.info("Application resources cleaned up");
    }

    @Override
    public void stop() throws Exception {
        cleanupResources();
        super.stop();
    }
}