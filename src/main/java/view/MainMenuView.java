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

import java.util.function.Consumer;

/**
 * Main menu screen for the Scrabble game.
 * Allows players to customize game settings including AI difficulty before starting.
 */
public class MainMenuView extends BorderPane {

    private final TextField playerNameField;
    private final ToggleGroup difficultyGroup;
    private final RadioButton easyButton;
    private final RadioButton mediumButton;
    private final RadioButton hardButton;
    private final CheckBox showDefinitionsBox;
    private Consumer<MainMenuSettings> onStartGameCallback;

    /**
     * Settings class to hold all user configuration options from the main menu.
     */
    public static class MainMenuSettings {
        private final String playerName;
        private final int difficulty;
        private final boolean showDefinitions;

        public MainMenuSettings(String playerName, int difficulty, boolean showDefinitions) {
            this.playerName = playerName;
            this.difficulty = difficulty;
            this.showDefinitions = showDefinitions;
        }

        public String getPlayerName() {
            return playerName;
        }

        public int getDifficulty() {
            return difficulty;
        }

        public boolean isShowDefinitions() {
            return showDefinitions;
        }
    }

    public MainMenuView() {
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f0f0f0;");

        // Create title
        Label titleLabel = new Label("Scrabble");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        titleLabel.setAlignment(Pos.CENTER);

        // Create player name input
        Label nameLabel = new Label("Your Name:");
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        playerNameField = new TextField("Player 1");
        playerNameField.setPrefWidth(200);

        HBox nameBox = new HBox(10, nameLabel, playerNameField);
        nameBox.setAlignment(Pos.CENTER);

        // Create difficulty selection
        Label difficultyLabel = new Label("AI Difficulty:");
        difficultyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        difficultyGroup = new ToggleGroup();

        easyButton = new RadioButton("Easy");
        easyButton.setToggleGroup(difficultyGroup);
        easyButton.setUserData(GameConstants.AI_EASY);
        easyButton.setSelected(true);

        mediumButton = new RadioButton("Medium");
        mediumButton.setToggleGroup(difficultyGroup);
        mediumButton.setUserData(GameConstants.AI_MEDIUM);

        hardButton = new RadioButton("Hard");
        hardButton.setToggleGroup(difficultyGroup);
        hardButton.setUserData(GameConstants.AI_HARD);

        VBox difficultyBox = new VBox(5, easyButton, mediumButton, hardButton);

        HBox difficultySection = new HBox(10, difficultyLabel, difficultyBox);
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
        VBox optionsBox = new VBox(20, nameBox, difficultySection, showDefinitionsBox);
        optionsBox.setAlignment(Pos.CENTER);
        optionsBox.setPadding(new Insets(30, 0, 30, 0));

        VBox mainBox = new VBox(20, titleLabel, optionsBox, startButton);
        mainBox.setAlignment(Pos.CENTER);

        // Add panel with background
        VBox menuPanel = new VBox(mainBox);
        menuPanel.setPadding(new Insets(30));
        menuPanel.setMaxWidth(500);
        menuPanel.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; " +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");

        setCenter(menuPanel);

        // Create version info
        Label versionLabel = new Label("Scrabble Game v1.0");
        versionLabel.setTextFill(Color.GRAY);
        setBottom(versionLabel);
        BorderPane.setAlignment(versionLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(versionLabel, new Insets(10, 10, 10, 0));
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
     * Collects all user settings and starts the game.
     */
    private void startGame() {
        if (onStartGameCallback != null) {
            String name = playerNameField.getText().trim();
            if (name.isEmpty()) {
                name = "Player 1";
            }

            Toggle selectedDifficulty = difficultyGroup.getSelectedToggle();
            int difficulty = GameConstants.AI_EASY; // Default to easy
            if (selectedDifficulty != null) {
                difficulty = (int) selectedDifficulty.getUserData();
            }

            boolean showDefinitions = showDefinitionsBox.isSelected();

            MainMenuSettings settings = new MainMenuSettings(name, difficulty, showDefinitions);
            onStartGameCallback.accept(settings);
        }
    }
}