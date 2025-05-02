package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.Move;
import utilities.WordFinder.WordPlacement;

import java.util.List;

public class HintView {
    private final Stage dialog;
    private final TableView<HintRow> hintsTable;

    public static class HintRow {
        private final String word;
        private final String position;
        private final String direction;
        private final int score;

        public HintRow(WordPlacement placement) {
            this.word = placement.getWord();
            this.position = (placement.getRow() + 1) + ", " + (placement.getCol() + 1);
            this.direction = placement.getDirection() == Move.Direction.HORIZONTAL ? "Horizontal" : "Vertical";
            this.score = placement.getScore();
        }

        public String getWord() { return word; }
        public String getPosition() { return position; }
        public String getDirection() { return direction; }
        public Integer getScore() { return score; }
    }

    public HintView() {
        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Available Moves");
        dialog.setMinWidth(450);
        dialog.setMinHeight(350);

        Label titleLabel = new Label("Possible Moves");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setPadding(new Insets(10));

        // Create table for hints
        hintsTable = new TableView<>();
        hintsTable.setPlaceholder(new Label("No possible moves found"));

        TableColumn<HintRow, String> wordCol = new TableColumn<>("Word");
        wordCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getWord()));
        wordCol.setPrefWidth(120);

        TableColumn<HintRow, String> posCol = new TableColumn<>("Starting Square");
        posCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPosition()));
        posCol.setPrefWidth(120);

        TableColumn<HintRow, String> dirCol = new TableColumn<>("Direction");
        dirCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDirection()));
        dirCol.setPrefWidth(100);

        TableColumn<HintRow, Integer> scoreCol = new TableColumn<>("Points");
        scoreCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getScore()));
        scoreCol.setPrefWidth(80);
        scoreCol.setSortType(TableColumn.SortType.DESCENDING);

        hintsTable.getColumns().addAll(wordCol, posCol, dirCol, scoreCol);
        hintsTable.getSortOrder().add(scoreCol);

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> dialog.close());
        closeButton.setPrefWidth(100);

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));
        buttonBox.getChildren().add(closeButton);

        BorderPane layout = new BorderPane();
        layout.setTop(titleLabel);
        layout.setCenter(hintsTable);
        layout.setBottom(buttonBox);

        Scene scene = new Scene(layout);
        dialog.setScene(scene);
    }

    public void showHints(List<WordPlacement> placements) {
        hintsTable.getItems().clear();

        // Convert WordPlacements to HintRows
        for (WordPlacement placement : placements) {
            hintsTable.getItems().add(new HintRow(placement));
        }

        // Sort by score
        hintsTable.sort();

        dialog.show();
    }

    public void close() {
        if (dialog.isShowing()) {
            dialog.close();
        }
    }
}