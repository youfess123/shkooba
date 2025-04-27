

import controller.GameController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import model.Dictionary;
import model.Game;
import model.Player;
import view.GameView;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScrabbleApp extends Application {
    private static final Logger logger = Logger.getLogger(ScrabbleApp.class.getName());

    private Game game;
    private GameController gameController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            initGame();
            gameController = new GameController(game);
            GameView gameView = new GameView(gameController);

            Scene scene = new Scene(gameView, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());


            primaryStage.setTitle("Scrabble");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.setOnCloseRequest(e -> cleanupResources());
            primaryStage.show();

            gameController.startGame();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start the game", e);
            showErrorAndExit("Failed to start the game: " + e.getMessage());
        }
    }

    private void initGame() throws IOException {
        InputStream dictionaryStream = Dictionary.loadDefaultDictionary();
        game = new Game(dictionaryStream, "Dictionary");
        game.addPlayer(new Player("Player 1",false));
        game.addPlayer(new Player("Computer", true));
        logger.info("Game initialized with 2 players");
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