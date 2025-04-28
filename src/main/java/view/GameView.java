package view;

import controller.GameController;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.Player;

/**
 * Main view class for the Scrabble game UI.
 * Organizes the board, rack, info panel, and control buttons.
 */
public class GameView extends BorderPane {
    private final GameController controller;
    private final BoardView boardView;
    private final RackView rackView;
    private final GameInfoView gameInfoView;
    private final ControlPanel controlPanel;

    /**
     * Creates a new game view with the specified controller.
     *
     * @param controller The game controller
     */
    public GameView(GameController controller) {
        this.controller = controller;
        this.boardView = new BoardView(controller);
        this.rackView = new RackView(controller);
        this.gameInfoView = new GameInfoView(controller);
        this.controlPanel = new ControlPanel(controller);

        setCenter(boardView);
        setRight(gameInfoView);
        setBottom(createBottomPane());
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #f0f0f0;");
        setupListeners();
    }

    /**
     * Creates the bottom pane containing rack and control panel.
     *
     * @return The bottom pane
     */
    private VBox createBottomPane() {
        VBox bottomPane = new VBox(10);
        bottomPane.setPadding(new Insets(10, 0, 0, 0));
        bottomPane.getChildren().addAll(rackView, controlPanel);
        return bottomPane;
    }

    /**
     * Sets up listeners for game events.
     */
    private void setupListeners() {
        controller.setBoardUpdateListener(() -> boardView.updateBoard());
        controller.setRackUpdateListener(() -> rackView.updateRack());
        controller.setPlayerUpdateListener(() -> {
            gameInfoView.updatePlayerInfo();
            rackView.updateRack();
            controlPanel.updateButtonStates();
        });
        controller.setGameOverListener(this::showGameOverDialog);

        // Add this line to update button states when temporary placements change
        controller.setTemporaryPlacementListener(() -> controlPanel.updateButtonStates());
    }

    /**
     * Shows the game over dialog with final scores.
     */
    private void showGameOverDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText("The game has ended!");

        StringBuilder content = new StringBuilder("Final scores:\n");
        int highestScore = -1;
        String winner = "";

        for (Player player : controller.getPlayers()) {
            int score = player.getScore();
            content.append(player.getName()).append(": ").append(score).append("\n");

            if (score > highestScore) {
                highestScore = score;
                winner = player.getName();
            }
        }

        content.append("\nWinner: ").append(winner).append("!");
        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    /**
     * Control panel with buttons for game actions.
     */
    private class ControlPanel extends VBox {
        private final Button playButton;
        private final Button cancelButton;
        private final Button exchangeButton;
        private final Button passButton;
        private final Button wordHistoryButton;
        private final Button toggleDefinitionsButton;
        private boolean definitionsEnabled = true;

        /**
         * Creates a new control panel with the specified controller.
         *
         * @param controller The game controller
         */
        public ControlPanel(GameController controller) {
            setSpacing(10);
            setPadding(new Insets(10));
            setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #cccccc; -fx-border-radius: 5;");

            // Create main game action buttons
            playButton = new Button("Play Word");
            playButton.setOnAction(e -> {
                boolean success = controller.commitPlacement();
                if (!success) {
                    if (controller.getTemporaryPlacements().isEmpty()) {
                        showError("No tiles placed. Please place tiles on the board first.");
                    } else {
                        showError("Invalid word placement. Ensure your tiles form valid English words.");
                    }
                }
            });

            cancelButton = new Button("Cancel Placement");
            cancelButton.setOnAction(e -> {
                if (!controller.getTemporaryPlacements().isEmpty()) {
                    controller.cancelPlacements();
                    showInfo("Placement cancelled. Tiles have been returned to your rack.");
                } else {
                    showInfo("No tiles to cancel.");
                }
            });

            exchangeButton = new Button("Exchange Tiles");
            exchangeButton.setOnAction(e -> {
                if (controller.getTemporaryPlacements().isEmpty()) {
                    if (controller.getSelectedTiles().isEmpty()) {
                        showError("No tiles selected. Please select tiles from your rack to exchange.");
                    } else {
                        boolean success = controller.exchangeTiles();
                        if (!success) {
                            showError("Exchange failed. There may not be enough tiles in the bag.");
                        }
                    }
                } else {
                    showError("Please cancel your current placement before exchanging tiles.");
                }
            });

            passButton = new Button("Pass Turn");
            passButton.setOnAction(e -> {
                if (controller.getTemporaryPlacements().isEmpty()) {
                    boolean success = controller.passTurn();
                    if (success) {
                        showInfo("Turn passed to " + controller.getCurrentPlayer().getName() + ".");
                    }
                } else {
                    showError("Please cancel your current placement before passing.");
                }
            });

            // Add new buttons for word definitions feature
            wordHistoryButton = new Button("Word History");
            wordHistoryButton.setOnAction(e -> controller.showWordHistory());
            wordHistoryButton.setTooltip(new Tooltip("View definitions of previously played words"));

            toggleDefinitionsButton = new Button("Definitions: ON");
            toggleDefinitionsButton.setOnAction(e -> toggleDefinitionsFeature(controller));
            toggleDefinitionsButton.setTooltip(new Tooltip("Toggle automatic word definitions after moves"));

            // Create an HBox for the main game control buttons
            HBox gameControlsBox = new HBox(10);
            gameControlsBox.getChildren().addAll(playButton, cancelButton, exchangeButton, passButton);
            for (var node : gameControlsBox.getChildren()) {
                HBox.setHgrow(node, Priority.ALWAYS);
                ((Button) node).setMaxWidth(Double.MAX_VALUE);
            }

            // Create an HBox for the educational feature buttons
            HBox educationalControlsBox = new HBox(10);
            educationalControlsBox.getChildren().addAll(wordHistoryButton, toggleDefinitionsButton);
            for (var node : educationalControlsBox.getChildren()) {
                HBox.setHgrow(node, Priority.ALWAYS);
                ((Button) node).setMaxWidth(Double.MAX_VALUE);
            }

            // Add button rows to the control panel
            getChildren().addAll(gameControlsBox, educationalControlsBox);

            // Set tooltips
            playButton.setTooltip(new Tooltip("Confirm and play the word"));
            cancelButton.setTooltip(new Tooltip("Cancel tile placement"));
            exchangeButton.setTooltip(new Tooltip("Exchange selected tiles for new ones"));
            passButton.setTooltip(new Tooltip("Pass your turn"));

            updateButtonStates();
        }

        /**
         * Toggles the word definitions feature on/off.
         *
         * @param controller The game controller
         */
        private void toggleDefinitionsFeature(GameController controller) {
            definitionsEnabled = !definitionsEnabled;
            controller.setShowDefinitionsEnabled(definitionsEnabled);

            if (definitionsEnabled) {
                toggleDefinitionsButton.setText("Definitions: ON");
                showInfo("Word definitions will be shown after each move.");
            } else {
                toggleDefinitionsButton.setText("Definitions: OFF");
                showInfo("Word definitions will not be shown automatically.");
            }
        }

        /**
         * Updates the enabled/disabled state of buttons based on the current game state.
         */
        public void updateButtonStates() {
            Player currentPlayer = controller.getCurrentPlayer();
            boolean isPlayerTurn = !currentPlayer.isComputer();
            boolean hasTemporaryPlacements = !controller.getTemporaryPlacements().isEmpty();
            boolean hasSelectedTiles = !controller.getSelectedTiles().isEmpty();

            playButton.setDisable(!isPlayerTurn || !hasTemporaryPlacements);
            cancelButton.setDisable(!isPlayerTurn || !hasTemporaryPlacements);
            exchangeButton.setDisable(!isPlayerTurn || hasTemporaryPlacements || !hasSelectedTiles);
            passButton.setDisable(!isPlayerTurn || hasTemporaryPlacements);

            // Word history button is always enabled as long as there are moves
            wordHistoryButton.setDisable(controller.getMoveHistory().isEmpty());
        }
    }

    /**
     * Shows an information dialog.
     *
     * @param message The message to display
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an error dialog.
     *
     * @param message The error message to display
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}