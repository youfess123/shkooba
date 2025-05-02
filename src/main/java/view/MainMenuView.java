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

public class MainMenuView extends BorderPane {

    private final List<PlayerEntry> playerEntries;
    private final VBox playersListBox;
    private final ComboBox<String> aiDifficultyCombo;
    private final CheckBox showDefinitionsBox;
    private Consumer<MainMenuSettings> onStartGameCallback;

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

        playerEntries = new ArrayList<>();
        playersListBox = new VBox(10);

        Label titleLabel = new Label("Scrabble");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        titleLabel.setAlignment(Pos.CENTER);

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

        Label difficultyLabel = new Label("AI Difficulty:");
        difficultyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        aiDifficultyCombo = new ComboBox<>();
        aiDifficultyCombo.getItems().addAll("Easy", "Medium", "Hard");
        aiDifficultyCombo.setValue("Easy");

        HBox difficultySection = new HBox(10, difficultyLabel, aiDifficultyCombo);
        difficultySection.setAlignment(Pos.CENTER);

        showDefinitionsBox = new CheckBox("Enable Word Definitions");
        showDefinitionsBox.setSelected(true);

        Button startButton = new Button("Start Game");
        startButton.setPrefWidth(150);
        startButton.setPrefHeight(40);
        startButton.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        startButton.setOnAction(e -> startGame());

        VBox optionsBox = new VBox(20, playersSection, difficultySection, showDefinitionsBox);
        optionsBox.setAlignment(Pos.CENTER);
        optionsBox.setPadding(new Insets(30, 0, 30, 0));

        VBox mainBox = new VBox(20, titleLabel, optionsBox, startButton);
        mainBox.setAlignment(Pos.CENTER);

        VBox menuPanel = new VBox(mainBox);
        menuPanel.setPadding(new Insets(30));
        menuPanel.setMaxWidth(600);
        menuPanel.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; " +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");

        setCenter(menuPanel);

        Label versionLabel = new Label("Educational Scrabble");
        versionLabel.setTextFill(Color.GRAY);
        setBottom(versionLabel);
        BorderPane.setAlignment(versionLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(versionLabel, new Insets(10, 10, 10, 0));

        addPlayer(false);
        addPlayer(true);
    }

    public void setOnStartGame(Consumer<MainMenuSettings> callback) {
        this.onStartGameCallback = callback;
    }

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

    private void removePlayer(PlayerEntry entry) {
        playerEntries.remove(entry);
        playersListBox.getChildren().remove(entry);
        updatePlayerControls();
    }

    private void updatePlayerControls() {
        for (PlayerEntry entry : playerEntries) {
            entry.setRemoveEnabled(playerEntries.size() > 2);
        }
    }

    private int getHumanPlayerCount() {
        int count = 0;
        for (PlayerEntry entry : playerEntries) {
            if (!entry.isComputer()) {
                count++;
            }
        }
        return count;
    }

    private int getComputerPlayerCount() {
        int count = 0;
        for (PlayerEntry entry : playerEntries) {
            if (entry.isComputer()) {
                count++;
            }
        }
        return count;
    }

    private void startGame() {
        if (playerEntries.size() < GameConstants.MIN_PLAYERS) {
            showError("At least " + GameConstants.MIN_PLAYERS + " players are required");
            return;
        }

        if (onStartGameCallback != null) {
            List<PlayerSettings> playerSettings = new ArrayList<>();
            for (PlayerEntry entry : playerEntries) {
                playerSettings.add(new PlayerSettings(entry.getName(), entry.isComputer()));
            }

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

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

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

            typeLabel = new Label(isComputer ? "Computer" : "Human");
            typeLabel.setMinWidth(80);
            typeLabel.setStyle(isComputer ?
                    "-fx-text-fill: #0066cc; -fx-font-weight: bold;" :
                    "-fx-text-fill: #009900; -fx-font-weight: bold;");

            Label nameLabel = new Label("Name:");
            nameField = new TextField(defaultName);
            nameField.setPrefWidth(200);
            if (isComputer) {
                nameField.setEditable(false);
                nameField.setStyle("-fx-background-color: #f0f0f0;");
            }

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