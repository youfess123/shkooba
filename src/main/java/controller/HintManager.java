package controller;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import model.Player;
import model.Rack;
import model.Tile;
import model.Move;
import utilities.WordFinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages the hint functionality for the Scrabble game.
 * Responsible for finding, displaying, and applying word placement hints.
 */
public class HintManager {
    private static final Logger logger = Logger.getLogger(HintManager.class.getName());

    private final GameController gameController;
    private List<WordFinder.WordPlacement> currentHints = new ArrayList<>();
    private boolean hintsActive = false;

    /**
     * Creates a new HintManager.
     *
     * @param gameController The game controller
     */
    public HintManager(GameController gameController) {
        this.gameController = gameController;
    }

    /**
     * Shows or hides word placement hints for the current player.
     * If hints are already active, this will hide them.
     */
    public void showHints() {
        if (!gameController.isGameInProgress() || gameController.getCurrentPlayer().isComputer()) {
            return;
        }

        // Toggle hints - if already showing, hide them
        if (hintsActive) {
            clearHints();
            return;
        }

        // Get hints using the WordFinder
        Player currentPlayer = gameController.getCurrentPlayer();
        Rack rack = currentPlayer.getRack();

        // Use the WordFinder to find valid placements
        WordFinder wordFinder = new WordFinder(gameController.getDictionary(), gameController.getBoard());
        List<WordFinder.WordPlacement> placements = wordFinder.findAllPlacements(rack);

        // Check if we found any hints
        if (placements.isEmpty()) {
            // Show a dialog indicating no hints are available
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Hints");
                alert.setHeaderText("No Hints Available");
                alert.setContentText("No valid word placements found with your current tiles.");
                alert.showAndWait();
            });
            logger.info("No hints found for the current rack");
            return;
        }

        // Limit to top N hints for better usability
        int maxHints = 5;
        currentHints = placements.stream()
                .limit(maxHints)
                .collect(Collectors.toList());

        // Update the board to show hints
        hintsActive = true;
        gameController.updateBoard();

        // Add a simple information dialog about the hints
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Hints");
            alert.setHeaderText("Found " + currentHints.size() + " possible word placements");

            StringBuilder content = new StringBuilder("Click on a highlighted square to place the suggested word.\n\n");
            content.append("Available hints (sorted by score):\n");

            for (int i = 0; i < currentHints.size(); i++) {
                WordFinder.WordPlacement hint = currentHints.get(i);
                content.append((i+1)).append(". ")
                        .append(hint.getWord()).append(" (")
                        .append(hint.getScore()).append(" points)\n");
            }

            alert.setContentText(content.toString());
            alert.showAndWait();
        });

        // Log the hints found
        logger.info("Found " + currentHints.size() + " possible word placements");
        for (WordFinder.WordPlacement wp : currentHints) {
            logger.fine("Hint: " + wp.toString());
        }
    }

    /**
     * Clears all current hints and updates the board.
     */
    public void clearHints() {
        hintsActive = false;
        currentHints.clear();
        gameController.updateBoard();
    }

    /**
     * Checks if hints are currently active/visible.
     *
     * @return true if hints are active, false otherwise
     */
    public boolean areHintsActive() {
        return hintsActive;
    }

    /**
     * Gets the current list of word placement hints.
     *
     * @return An unmodifiable list of word placements
     */
    public List<WordFinder.WordPlacement> getCurrentHints() {
        return Collections.unmodifiableList(currentHints);
    }

    /**
     * Applies a selected hint by placing the necessary tiles on the board.
     *
     * @param hint The word placement hint to apply
     */
    public void applyHint(WordFinder.WordPlacement hint) {
        if (!hintsActive || hint == null) {
            return;
        }

        // Clear any existing temporary placements
        gameController.cancelPlacements();

        // Apply the hint by placing its tiles
        int row = hint.getRow();
        int col = hint.getCol();
        Move.Direction direction = hint.getDirection();
        String word = hint.getWord();

        logger.info("Applying hint: " + word + " at (" + row + "," + col + ") " +
                (direction == Move.Direction.HORIZONTAL ? "horizontally" : "vertically"));

        // Process each letter in the word
        for (int i = 0; i < word.length(); i++) {
            int currentRow = row + (direction == Move.Direction.VERTICAL ? i : 0);
            int currentCol = col + (direction == Move.Direction.HORIZONTAL ? i : 0);

            // Skip if there's already a tile on the board
            if (gameController.getBoard().getSquare(currentRow, currentCol).hasTile()) {
                logger.fine("Skipping position (" + currentRow + "," + currentCol + ") - already has tile");
                continue;
            }

            // Find the tile in the rack
            char letter = word.charAt(i);
            int rackIndex = findTileInRack(letter);

            if (rackIndex >= 0) {
                logger.fine("Placing " + letter + " from rack index " + rackIndex +
                        " at (" + currentRow + "," + currentCol + ")");
                gameController.placeTileTemporarily(rackIndex, currentRow, currentCol);
            } else {
                logger.warning("Could not find tile for letter " + letter + " in rack");
            }
        }

        // Clear hints after applying
        clearHints();
    }

    /**
     * Finds a tile in the current player's rack that matches the given letter.
     *
     * @param letter The letter to find
     * @return The index of the tile in the rack, or -1 if not found
     */
    private int findTileInRack(char letter) {
        Rack rack = gameController.getCurrentPlayer().getRack();
        // Get tiles that are already being used in temporary placements
        List<Integer> usedIndices = gameController.getTemporaryIndices();

        // Find a tile in the rack that matches the letter and isn't already used
        for (int i = 0; i < rack.size(); i++) {
            Tile tile = rack.getTile(i);
            // Check if the tile has the right letter and isn't already used in a temporary placement
            if (tile.getLetter() == letter && !usedIndices.contains(i)) {
                return i;
            }
        }
        return -1; // Not found
    }
}