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
import utilities.WordFinder;

import java.util.List;

public class GameView extends BorderPane {
    private final GameController controller;
    private final BoardView boardView;
    private final RackView rackView;
    private final GameInfoView gameInfoView;
    private final ControlPanel controlPanel;

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

    private VBox createBottomPane() {
        VBox bottomPane = new VBox(10);
        bottomPane.setPadding(new Insets(10, 0, 0, 0));
        bottomPane.getChildren().addAll(rackView, controlPanel);
        return bottomPane;
    }

    private void setupListeners() {
        controller.setBoardUpdateListener(() -> boardView.updateBoard());
        controller.setRackUpdateListener(() -> rackView.updateRack());
        controller.setPlayerUpdateListener(() -> {
            gameInfoView.updatePlayerInfo();
            rackView.updateRack();
            controlPanel.updateButtonStates();
        });
        controller.setGameOverListener(this::showEndGameView);
        controller.setTemporaryPlacementListener(() -> controlPanel.updateButtonStates());
    }

    public void cleanup() {
        if (controlPanel != null) {
            controlPanel.closeHintDialog();
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showEndGameView() {
        EndGameView endGameView = new EndGameView(controller.getPlayers());
        endGameView.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private class ControlPanel extends VBox {
        private final Button playButton;
        private final Button cancelButton;
        private final Button exchangeButton;
        private final Button passButton;
        private final Button wordHistoryButton;
        private final Button hintsButton;
        private HintView hintView;

        public ControlPanel(GameController controller) {
            setSpacing(10);
            setPadding(new Insets(10));
            setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #cccccc; -fx-border-radius: 5;");

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

            wordHistoryButton = new Button("Word History");
            wordHistoryButton.setOnAction(e -> controller.showWordHistory());
            wordHistoryButton.setTooltip(new Tooltip("View definitions of previously played words"));

            hintsButton = new Button("Show Hints");
            hintsButton.setOnAction(e -> showHints());
            hintsButton.setTooltip(new Tooltip("Show possible word placements"));

            hintView = new HintView();

            HBox gameControlsBox = new HBox(10);
            gameControlsBox.getChildren().addAll(playButton, cancelButton, exchangeButton, passButton);
            for (var node : gameControlsBox.getChildren()) {
                HBox.setHgrow(node, Priority.ALWAYS);
                ((Button) node).setMaxWidth(Double.MAX_VALUE);
            }

            HBox educationalControlsBox = new HBox(10);
            educationalControlsBox.getChildren().addAll(wordHistoryButton, hintsButton);
            for (var node : educationalControlsBox.getChildren()) {
                HBox.setHgrow(node, Priority.ALWAYS);
                ((Button) node).setMaxWidth(Double.MAX_VALUE);
            }

            getChildren().addAll(gameControlsBox, educationalControlsBox);

            playButton.setTooltip(new Tooltip("Confirm and play the word"));
            cancelButton.setTooltip(new Tooltip("Cancel tile placement"));
            exchangeButton.setTooltip(new Tooltip("Exchange selected tiles for new ones"));
            passButton.setTooltip(new Tooltip("Pass your turn"));

            updateButtonStates();
        }

        private void showHints() {
            List<WordFinder.WordPlacement> hints = controller.generateHints();
            if (hints.isEmpty()) {
                showInfo("No valid moves found with your current tiles.");
            } else {
                hintView.showHints(hints);
            }
        }

        public void closeHintDialog() {
            if (hintView != null) {
                hintView.close();
            }
        }

        public void updateButtonStates() {
            Player currentPlayer = controller.getCurrentPlayer();
            boolean isPlayerTurn = !currentPlayer.isComputer();
            boolean hasTemporaryPlacements = !controller.getTemporaryPlacements().isEmpty();
            boolean hasSelectedTiles = !controller.getSelectedTiles().isEmpty();

            playButton.setDisable(!isPlayerTurn || !hasTemporaryPlacements);
            cancelButton.setDisable(!isPlayerTurn || !hasTemporaryPlacements);
            exchangeButton.setDisable(!isPlayerTurn || hasTemporaryPlacements || !hasSelectedTiles);
            passButton.setDisable(!isPlayerTurn || hasTemporaryPlacements);
            hintsButton.setDisable(!isPlayerTurn);
            wordHistoryButton.setDisable(controller.getMoveHistory().isEmpty());
        }
    }
}