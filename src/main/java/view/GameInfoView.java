package view;

import controller.GameController;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.Move;
import model.Player;

import java.util.List;

public class GameInfoView extends VBox {

    private final GameController controller;
    private final Label currentPlayerLabel;
    private final Label tilesRemainingLabel;
    private final VBox playerScoresBox;
    private final ListView<String> moveHistoryList;

    public GameInfoView(GameController controller) {
        this.controller = controller;

        setPrefWidth(250);
        setPadding(new Insets(10));
        setSpacing(15);
        setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, new CornerRadii(5), BorderWidths.DEFAULT)));
        setBackground(new Background(new BackgroundFill(Color.WHITESMOKE, CornerRadii.EMPTY, Insets.EMPTY)));

        // Current Player section
        currentPlayerLabel = new Label();
        currentPlayerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        currentPlayerLabel.setWrapText(true);
        currentPlayerLabel.setStyle("-fx-background-color: #e5e5e5; -fx-padding: 5; -fx-background-radius: 3;");

        tilesRemainingLabel = new Label();
        tilesRemainingLabel.setFont(Font.font("Arial", 12));

        // Players section
        Label scoresHeader = new Label("Players");
        scoresHeader.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        playerScoresBox = new VBox(5);
        playerScoresBox.setStyle("-fx-background-color: white; -fx-border-color: #dddddd; -fx-border-radius: 5; -fx-padding: 5;");

        // Move History section
        Label historyHeader = new Label("Move History");
        historyHeader.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        moveHistoryList = new ListView<>();
        moveHistoryList.setPrefHeight(250);
        moveHistoryList.setStyle("-fx-background-radius: 3; -fx-border-radius: 3;");

        Separator separator1 = new Separator();
        Separator separator2 = new Separator();

        getChildren().addAll(
                currentPlayerLabel,
                tilesRemainingLabel,
                separator1,
                scoresHeader,
                playerScoresBox,
                separator2,
                historyHeader,
                moveHistoryList
        );

        updatePlayerInfo();
    }

    public void updatePlayerInfo() {
        Player currentPlayer = controller.getCurrentPlayer();

        // Update current player with highlight for type
        String playerType = currentPlayer.isComputer() ? "Computer" : "Human";
        String playerTypeStyle = currentPlayer.isComputer() ?
                "color: #0066cc;" : "color: #009900;";

        currentPlayerLabel.setText("Current Player: " + currentPlayer.getName() +
                " (" + playerType + ")");

        int remainingTiles = controller.getRemainingTileCount();
        tilesRemainingLabel.setText("Tiles Remaining: " + remainingTiles);

        updatePlayerScores();
        updateMoveHistory();
    }

    private void updatePlayerScores() {
        playerScoresBox.getChildren().clear();

        List<Player> players = controller.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);

            // Create a more styled player entry
            Label scoreLabel = new Label(player.getName() + ": " + player.getScore() + " pts");

            // Apply special styling for current player
            if (player == controller.getCurrentPlayer()) {
                scoreLabel.setTextFill(Color.BLUE);
                scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                scoreLabel.setStyle("-fx-background-color: #e6f2ff; -fx-padding: 3 5; -fx-background-radius: 3;");
            } else {
                scoreLabel.setFont(Font.font("Arial", 12));
                scoreLabel.setStyle("-fx-padding: 3 5;");
            }

            // Add icon/indicator for player type
            String playerType = player.isComputer() ? "[Computer]" : "[Human]";
            scoreLabel.setText(playerType + " " + scoreLabel.getText());

            playerScoresBox.getChildren().add(scoreLabel);
        }
    }

    private void updateMoveHistory() {
        moveHistoryList.getItems().clear();
        moveHistoryList.getItems().add("Game started");
        List<Move> moves = controller.getMoveHistory();
        for (Move move : moves) {
            moveHistoryList.getItems().add(move.toString());
        }

        // Scroll to the bottom to show most recent moves
        if (!moveHistoryList.getItems().isEmpty()) {
            moveHistoryList.scrollTo(moveHistoryList.getItems().size() - 1);
        }
    }
}