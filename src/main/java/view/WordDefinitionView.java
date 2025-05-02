package view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import service.DictionaryService;
import service.DictionaryService.WordDefinition;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Dialog to display word definitions after a player makes a move
 * or when looking up definitions from word history.
 */
public class WordDefinitionView {
    private final Stage dialog;
    private final DictionaryService dictionaryService;

    // UI components for the word definition display
    private final Label titleLabel;
    private final VBox definitionsBox;
    private final Button closeButton;
    private final Label loadingLabel;

    public WordDefinitionView(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;

        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Word Definition");
        dialog.setMinWidth(400);
        dialog.setMinHeight(300);

        // Create UI components
        titleLabel = new Label();
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setPadding(new Insets(10, 10, 5, 10));

        definitionsBox = new VBox(10);
        definitionsBox.setPadding(new Insets(10));

        loadingLabel = new Label("Loading definition...");
        loadingLabel.setFont(Font.font("Arial", 14));
        loadingLabel.setAlignment(Pos.CENTER);
        loadingLabel.setTextAlignment(TextAlignment.CENTER);
        loadingLabel.setMaxWidth(Double.MAX_VALUE);
        loadingLabel.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(definitionsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPadding(new Insets(5));

        closeButton = new Button("Close");
        closeButton.setOnAction(e -> dialog.close());
        closeButton.setPrefWidth(100);

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));
        buttonBox.getChildren().add(closeButton);

        BorderPane layout = new BorderPane();
        layout.setTop(titleLabel);
        layout.setCenter(scrollPane);
        layout.setBottom(buttonBox);

        Scene scene = new Scene(layout);
        dialog.setScene(scene);
    }

    public void showDefinition(String word) {
        // Clear previous content
        definitionsBox.getChildren().clear();

        // Set the title
        titleLabel.setText("Definition: " + word.toUpperCase());

        // Show loading indicator
        definitionsBox.getChildren().add(loadingLabel);

        // Show the dialog
        dialog.show();
        dialog.setOnShown(e -> loadDefinition(word));
    }

    public void showDefinitions(List<String> words) {
        if (words.isEmpty()) {
            return;
        }



        // Clear previous content
        definitionsBox.getChildren().clear();

        // Set the title
        titleLabel.setText("Word Definitions");

        // Create a list view for word selection
        ListView<String> wordListView = new ListView<>();
        wordListView.getItems().addAll(words);

        Label instructionLabel = new Label("Select a word to view its definition:");
        instructionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        VBox selectionBox = new VBox(10);
        selectionBox.getChildren().addAll(instructionLabel, wordListView);

        // Show the selection UI
        definitionsBox.getChildren().add(selectionBox);

        // Handle word selection
        wordListView.getSelectionModel().selectedItemProperty().addListener((obs, oldWord, newWord) -> {
            if (newWord != null) {
                definitionsBox.getChildren().clear();
                definitionsBox.getChildren().add(loadingLabel);
                loadDefinition(newWord);
            }
        });

        // Show the dialog
        dialog.show();
    }

    /**
     * Loads the definition for a word and updates the UI when it's available.
     *
     * @param word The word to look up
     */
    private void loadDefinition(String word) {
        CompletableFuture<List<WordDefinition>> future = dictionaryService.getDefinitions(word);

        future.thenAccept(definitions -> {
            Platform.runLater(() -> {
                displayDefinitions(word, definitions);
            });
        });
    }

    /**
     * Updates the UI to display the loaded definitions.
     *
     * @param word The word that was looked up
     * @param definitions The list of definitions
     */
    private void displayDefinitions(String word, List<WordDefinition> definitions) {
        definitionsBox.getChildren().clear();

        if (definitions.isEmpty()) {
            Label noDefLabel = new Label("No definition found for '" + word + "'.");
            noDefLabel.setFont(Font.font("Arial", 14));
            definitionsBox.getChildren().add(noDefLabel);
            return;
        }

        for (WordDefinition def : definitions) {
            VBox defBox = new VBox(5);
            defBox.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: #f8f8f8;");

            Label posLabel = new Label(def.getPartOfSpeech());
            posLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            posLabel.setStyle("-fx-text-fill: #555555;");

            Label defLabel = new Label(def.getDefinition());
            defLabel.setFont(Font.font("Arial", 14));
            defLabel.setWrapText(true);

            defBox.getChildren().addAll(posLabel, defLabel);

            if (def.hasExamples()) {
                Label exampleLabel = new Label("Example: " + def.getExamples().get(0));
                exampleLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
                exampleLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
                exampleLabel.setWrapText(true);
                defBox.getChildren().add(exampleLabel);
            }

            definitionsBox.getChildren().add(defBox);
        }
    }

    /**
     * Closes the dialog if it's showing.
     */
    public void close() {
        if (dialog.isShowing()) {
            dialog.close();
        }
    }
}