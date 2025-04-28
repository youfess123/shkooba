package controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Player;
import model.Rack;
import model.Move;
import model.Dictionary;
import utilities.WordFinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages the hint functionality for the Scrabble game.
 * Uses GADDAG structure to find possible word placements.
 */
public class HintManager {
    private static final Logger logger = Logger.getLogger(HintManager.class.getName());

    private final GameController gameController;

    /**
     * Creates a new HintManager.
     *
     * @param gameController The game controller
     */
    public HintManager(GameController gameController) {
        this.gameController = gameController;
    }

    /**
     * Shows possible word placements based on the current board and player's rack.
     * Uses the Gaddag structure for efficient word finding.
     */
    public void showHints() {
        if (!gameController.isGameInProgress() || gameController.getCurrentPlayer().isComputer()) {
            return;
        }

        // Get the player's rack
        Player currentPlayer = gameController.getCurrentPlayer();
        Rack rack = currentPlayer.getRack();

        // Use the WordFinder to find valid placements
        WordFinder wordFinder = new WordFinder(gameController.getDictionary(), gameController.getBoard());
        List<WordFinder.WordPlacement> placements = wordFinder.findAllPlacements(rack);

        // Check if we found any words
        if (placements.isEmpty()) {
            // Show a dialog indicating no hints are available
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Hints");
                alert.setHeaderText("No Valid Moves Available");
                alert.setContentText("No valid word placements found with your current tiles. You might want to consider exchanging tiles or passing your turn.");
                alert.showAndWait();
            });
            logger.info("No hints found for the current rack");
            return;
        }

        // Sort by score in descending order
        Collections.sort(placements, (a, b) -> Integer.compare(b.getScore(), a.getScore()));

        // Limit to top N hints for better readability
        int maxHints = 10;
        List<WordFinder.WordPlacement> topPlacements = placements.stream()
                .limit(maxHints)
                .collect(Collectors.toList());

        // Show the dialog with the hints
        showHintDialog(topPlacements, placements.size() > maxHints ? placements.size() - maxHints : 0);

        // Log the hints found
        logger.info("Found " + placements.size() + " possible word placements");
    }

    /**
     * Shows a custom dialog with the hints.
     * This uses a separate dialog from the definition feature to avoid conflicts.
     *
     * @param placements The list of word placements to show
     * @param additionalCount Number of additional placements not shown
     */
    private void showHintDialog(List<WordFinder.WordPlacement> placements, int additionalCount) {
        Platform.runLater(() -> {
            // Create a custom dialog to avoid conflicts with definition dialog
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Scrabble Hints");
            dialog.setWidth(500);
            dialog.setHeight(400);

            VBox content = new VBox(10);
            content.setPadding(new Insets(15));

            // Add title
            Label titleLabel = new Label("Available Word Placements");
            titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            content.getChildren().add(titleLabel);

            // Create list view of placements
            ListView<String> placementsList = new ListView<>();
            for (int i = 0; i < placements.size(); i++) {
                WordFinder.WordPlacement placement = placements.get(i);
                String word = placement.getWord();
                int score = placement.getScore();
                int row = placement.getRow() + 1; // Convert to 1-based for display
                int col = placement.getCol() + 1; // Convert to 1-based for display
                String direction = placement.getDirection() == Move.Direction.HORIZONTAL ? "horizontally" : "vertically";

                placementsList.getItems().add(String.format("%d. %s (%d points) - Starting at (%d,%d) %s",
                        i+1, word, score, row, col, direction));
            }

            // Add the list to a scroll pane
            ScrollPane scrollPane = new ScrollPane(placementsList);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
            content.getChildren().add(scrollPane);

            // Add footer if there are additional placements
            if (additionalCount > 0) {
                Label footerLabel = new Label("...and " + additionalCount + " more possibilities.");
                content.getChildren().add(footerLabel);
            }

            // Add OK button
            javafx.scene.control.Button okButton = new javafx.scene.control.Button("OK");
            okButton.setDefaultButton(true);
            okButton.setPrefWidth(100);
            okButton.setOnAction(e -> dialog.close());

            // Add button to the content
            VBox buttonBox = new VBox();
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
            buttonBox.getChildren().add(okButton);
            content.getChildren().add(buttonBox);

            // Set the content to the dialog
            javafx.scene.Scene scene = new javafx.scene.Scene(content);
            dialog.setScene(scene);

            // Show the dialog
            dialog.showAndWait();
        });
    }
}