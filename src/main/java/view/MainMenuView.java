package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import utilities.GameConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Main menu screen for the Scrabble game.
 * Allows players to customize game settings including AI difficulty before starting.
 */
public class MainMenuView extends BorderPane {

    private final List<PlayerEntry> playerEntries;
    private final VBox playersListBox;
    private final ComboBox<String> aiDifficultyCombo;
    private final CheckBox showDefinitionsBox;
    private Consumer<MainMenuSettings> onStartGameCallback;

    /**
     * Settings class to hold all user configuration options from the main menu.
     */
    public static class MainMenuSettings {
        private final List<PlayerSettings> players;
        private final int aiDifficulty;
        private final boolean showDefinitions;

        public MainMenuSettings(List<PlayerSettings> players, int aiDifficulty, boolean showDefinitions) {
            this.players = players;
            this.aiDifficulty = aiDifficulty;
            this.showDefinitions = showDefinitions;
        }

        public List<PlayerSettings> getPlayers() {
            return players;
        }

        public int getDifficulty() {
            return aiDifficulty;
        }

        public boolean isShowDefinitions() {
            return showDefinitions;
        }
    }

    /**
     * Settings for a single player.
     */
    public static class PlayerSettings {
        private final String name;
        private final boolean isComputer;

        public PlayerSettings(String name, boolean isComputer) {
            this.name = name;
            this.isComputer = isComputer;
        }

        public String getName() {
            return name;
        }

        public boolean isComputer() {
            return isComputer;
        }
    }

    public MainMenuView() {
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f0f0f0;");

        // Initialize player entries list
        playerEntries = new ArrayList<>();
        playersListBox = new VBox(10);

        // Create title
        Label titleLabel = new Label("Scrabble");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        titleLabel.setAlignment(Pos.CENTER);

        // Create player management section
        Label playersLabel = new Label("Players:");
        playersLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        Button addHumanButton = new Button("Add Human Player");
        addHumanButton.setOnAction(e -> addPlayer(false));

        Button addComputerButton = new Button("Add Computer Player");
        addComputerButton.setOnAction(e -> addPlayer(true));

        HBox addButtonsBox = new HBox(10, addHumanButton, addComputerButton);
        addButtonsBox.setAlignment(Pos.CENTER);

        VBox playersSection = new VBox(10, playersLabel, playersListBox, addButtonsBox);
        playersSection.setAlignment(Pos.CENTER);

        // Create AI difficulty selection
        Label difficultyLabel = new Label("AI Difficulty:");
        difficultyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        aiDifficultyCombo = new ComboBox<>();
        aiDifficultyCombo.getItems().addAll("Easy", "Medium", "Hard");
        aiDifficultyCombo.setValue("Easy");

        HBox difficultySection = new HBox(10, difficultyLabel, aiDifficultyCombo);
        difficultySection.setAlignment(Pos.CENTER);

        // Additional options
        showDefinitionsBox = new CheckBox("Show word definitions after moves");
        showDefinitionsBox.setSelected(true);

        // Create Start Game button
        Button startButton = new Button("Start Game");
        startButton.setPrefWidth(150);
        startButton.setPrefHeight(40);
        startButton.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        startButton.setOnAction(e -> startGame());

        // Create layout
        VBox optionsBox = new VBox(20, playersSection, difficultySection, showDefinitionsBox);
        optionsBox.setAlignment(Pos.CENTER);
        optionsBox.setPadding(new Insets(30, 0, 30, 0));

        VBox mainBox = new VBox(20, titleLabel, optionsBox, startButton);
        mainBox.setAlignment(Pos.CENTER);

        // Add panel with background
        VBox menuPanel = new VBox(mainBox);
        menuPanel.setPadding(new Insets(30));
        menuPanel.setMaxWidth(600);
        menuPanel.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; " +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");

        setCenter(menuPanel);

        // Create version info
        Label versionLabel = new Label("Scrabble Game v1.0");
        versionLabel.setTextFill(Color.GRAY);
        setBottom(versionLabel);
        BorderPane.setAlignment(versionLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(versionLabel, new Insets(10, 10, 10, 0));

        // Add default players (1 human, 1 computer)
        addPlayer(false);
        addPlayer(true);
    }

    /**
     * Sets the callback to be executed when the game should start.
     *
     * @param callback Function that takes the menu settings as parameter
     */
    public void setOnStartGame(Consumer<MainMenuSettings> callback) {
        this.onStartGameCallback = callback;
    }

    /**
     * Adds a new player entry to the list.
     *
     * @param isComputer Whether this is a computer player
     */
    private void addPlayer(boolean isComputer) {
        if (playerEntries.size() >= GameConstants.MAX_PLAYERS) {
            showError("Maximum number of players reached (" + GameConstants.MAX_PLAYERS + ")");
            return;
        }

        String defaultName;
        if (isComputer) {
            defaultName = "Computer " + (getComputerPlayerCount() + 1);
        } else {
            defaultName = "Player " + (getHumanPlayerCount() + 1);
        }

        PlayerEntry entry = new PlayerEntry(defaultName, isComputer);
        playerEntries.add(entry);
        playersListBox.getChildren().add(entry);

        updatePlayerControls();
    }

    /**
     * Removes a player entry from the list.
     *
     * @param entry The player entry to remove
     */
    private void removePlayer(PlayerEntry entry) {
        playerEntries.remove(entry);
        playersListBox.getChildren().remove(entry);
        updatePlayerControls();
    }

    /**
     * Updates the enabled state of player controls based on current state.
     */
    private void updatePlayerControls() {
        // Make sure at least 2 players are in the game
        for (PlayerEntry entry : playerEntries) {
            entry.setRemoveEnabled(playerEntries.size() > 2);
        }
    }

    /**
     * Gets the number of human players currently added.
     */
    private int getHumanPlayerCount() {
        int count = 0;
        for (PlayerEntry entry : playerEntries) {
            if (!entry.isComputer()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the number of computer players currently added.
     */
    private int getComputerPlayerCount() {
        int count = 0;
        for (PlayerEntry entry : playerEntries) {
            if (entry.isComputer()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Collects all user settings and starts the game.
     */
    private void startGame() {
        if (playerEntries.size() < GameConstants.MIN_PLAYERS) {
            showError("At least " + GameConstants.MIN_PLAYERS + " players are required");
            return;
        }

        if (onStartGameCallback != null) {
            // Create player settings
            List<PlayerSettings> playerSettings = new ArrayList<>();
            for (PlayerEntry entry : playerEntries) {
                playerSettings.add(new PlayerSettings(entry.getName(), entry.isComputer()));
            }

            // Get AI difficulty
            int difficulty;
            switch (aiDifficultyCombo.getValue()) {
                case "Medium":
                    difficulty = GameConstants.AI_MEDIUM;
                    break;
                case "Hard":
                    difficulty = GameConstants.AI_HARD;
                    break;
                default:
                    difficulty = GameConstants.AI_EASY;
                    break;
            }

            boolean showDefinitions = showDefinitionsBox.isSelected();

            MainMenuSettings settings = new MainMenuSettings(playerSettings, difficulty, showDefinitions);
            onStartGameCallback.accept(settings);
        }
    }

    /**
     * Shows an error dialog.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Component representing a single player entry in the menu.
     */
    private class PlayerEntry extends HBox {
        private final TextField nameField;
        private final Label typeLabel;
        private final Button removeButton;
        private final boolean computer;

        public PlayerEntry(String defaultName, boolean isComputer) {
            super(10);
            this.computer = isComputer;

            setPadding(new Insets(5));
            setAlignment(Pos.CENTER_LEFT);
            setStyle("-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-background-color: #f8f8f8; -fx-padding: 10;");

            // Player type icon/label
            typeLabel = new Label(isComputer ? "Computer" : "Human");
            typeLabel.setMinWidth(80);
            typeLabel.setStyle(isComputer ?
                    "-fx-text-fill: #0066cc; -fx-font-weight: bold;" :
                    "-fx-text-fill: #009900; -fx-font-weight: bold;");

            // Name field
            Label nameLabel = new Label("Name:");
            nameField = new TextField(defaultName);
            nameField.setPrefWidth(200);
            if (isComputer) {
                nameField.setEditable(false);
                nameField.setStyle("-fx-background-color: #f0f0f0;");
            }

            // Remove button
            removeButton = new Button("Remove");
            removeButton.setOnAction(e -> removePlayer(this));

            getChildren().addAll(typeLabel, nameLabel, nameField, removeButton);
            HBox.setHgrow(nameField, Priority.ALWAYS);
        }

        public String getName() {
            return nameField.getText().trim().isEmpty() ? typeLabel.getText() : nameField.getText().trim();
        }

        public boolean isComputer() {
            return computer;
        }

        public void setRemoveEnabled(boolean enabled) {
            removeButton.setDisable(!enabled);
        }
    }
}