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

        setPrefWidth(200);
        setPadding(new Insets(10));
        setSpacing(15);
        setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, new CornerRadii(5), BorderWidths.DEFAULT)));
        setBackground(new Background(new BackgroundFill(Color.WHITESMOKE, CornerRadii.EMPTY, Insets.EMPTY)));

        currentPlayerLabel = new Label();
        currentPlayerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        currentPlayerLabel.setWrapText(true);

        tilesRemainingLabel = new Label();
        tilesRemainingLabel.setFont(Font.font("Arial", 12));

        Label scoresHeader = new Label("Scores");
        scoresHeader.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        playerScoresBox = new VBox(5);

        Label historyHeader = new Label("Move History");
        historyHeader.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        moveHistoryList = new ListView<>();
        moveHistoryList.setPrefHeight(200);

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
        currentPlayerLabel.setText("Current Player: " + currentPlayer.getName());

        int remainingTiles = controller.getRemainingTileCount();
        tilesRemainingLabel.setText("Tiles Remaining: " + remainingTiles);

        updatePlayerScores();
        updateMoveHistory();
    }

    private void updatePlayerScores() {
        playerScoresBox.getChildren().clear();
        for (Player player : controller.getPlayers()) {
            Label scoreLabel = new Label(player.getName() + ": " + player.getScore());
            if (player == controller.getCurrentPlayer()) {
                scoreLabel.setTextFill(Color.BLUE);
                scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            } else {
                scoreLabel.setFont(Font.font("Arial", 12));
            }
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