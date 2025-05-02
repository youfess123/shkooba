package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.Player;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class EndGameView {
    private final Stage dialog;
    private final List<Player> players;

    public EndGameView(List<Player> players) {
        this.players = new ArrayList<>(players);

        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.DECORATED);
        dialog.setTitle("Game Over");
        dialog.setMinWidth(400);
        dialog.setMinHeight(300);

        BorderPane layout = createLayout();
        Scene scene = new Scene(layout);
        dialog.setScene(scene);
    }

    private BorderPane createLayout() {
        // Title section
        Label titleLabel = new Label("Game Over!");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setPadding(new Insets(15));

        // Sort players by score (highest to lowest)
        players.sort(Comparator.comparing(Player::getScore).reversed());

        // Identify the winner(s) (handle ties)
        int highestScore = players.isEmpty() ? 0 : players.get(0).getScore();
        List<Player> winners = new ArrayList<>();
        for (Player player : players) {
            if (player.getScore() == highestScore) {
                winners.add(player);
            }
        }

        // Winner section
        VBox winnerBox = new VBox(10);
        winnerBox.setAlignment(Pos.CENTER);
        winnerBox.setPadding(new Insets(10, 0, 20, 0));

        String winnerText;
        if (winners.size() == 1) {
            winnerText = "Winner: " + winners.get(0).getName() + "!";
        } else {
            StringBuilder sb = new StringBuilder("Tie between: ");
            for (int i = 0; i < winners.size(); i++) {
                if (i > 0) sb.append(" and ");
                sb.append(winners.get(i).getName());
            }
            winnerText = sb.toString();
        }

        Label winnerLabel = new Label(winnerText);
        winnerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        winnerLabel.setTextFill(Color.GREEN);
        winnerBox.getChildren().add(winnerLabel);

        // Scores section
        VBox scoresBox = new VBox(5);
        scoresBox.setPadding(new Insets(10));
        scoresBox.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-radius: 5;");

        Label scoresHeader = new Label("Final Scores");
        scoresHeader.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        scoresBox.getChildren().add(scoresHeader);

        // Add a separator
        Separator separator = new Separator();
        scoresBox.getChildren().add(separator);

        // Add player scores
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            HBox playerRow = new HBox(10);
            playerRow.setAlignment(Pos.CENTER_LEFT);
            playerRow.setPadding(new Insets(5, 10, 5, 10));

            // Highlight the winner(s)
            if (player.getScore() == highestScore) {
                playerRow.setStyle("-fx-background-color: #e6ffe6;");
            } else if (i % 2 == 1) {
                playerRow.setStyle("-fx-background-color: #f5f5f5;");
            }

            // Rank
            Label rankLabel = new Label((i + 1) + ".");
            rankLabel.setMinWidth(30);
            rankLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

            // Player name with type indicator
            String playerType = player.isComputer() ? " (Computer)" : " (Human)";
            Label nameLabel = new Label(player.getName() + playerType);
            nameLabel.setFont(Font.font("Arial", 14));
            nameLabel.setMinWidth(200);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            // Score
            Label scoreLabel = new Label(String.valueOf(player.getScore()));
            scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            scoreLabel.setMinWidth(80);
            scoreLabel.setAlignment(Pos.CENTER_RIGHT);

            playerRow.getChildren().addAll(rankLabel, nameLabel, scoreLabel);
            scoresBox.getChildren().add(playerRow);
        }

        // Combine sections
        VBox contentBox = new VBox(10);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.setPadding(new Insets(10));
        contentBox.getChildren().addAll(winnerBox, scoresBox);

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        // Close button
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> dialog.close());
        closeButton.setPrefWidth(120);
        closeButton.setPrefHeight(30);
        closeButton.setStyle("-fx-font-size: 14px;");

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));
        buttonBox.getChildren().add(closeButton);

        BorderPane layout = new BorderPane();
        layout.setTop(titleLabel);
        layout.setCenter(scrollPane);
        layout.setBottom(buttonBox);

        return layout;
    }

    public void show() {
        dialog.show();
    }

    public void close() {
        if (dialog.isShowing()) {
            dialog.close();
        }
    }
}