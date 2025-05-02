package view;

import controller.GameController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.Player;
import model.Rack;
import model.Tile;
import utilities.GameConstants;

import java.util.ArrayList;
import java.util.List;

public class RackView extends HBox {

    private final GameController controller;
    private List<TileView> tileViews;

    public RackView(GameController controller) {
        this.controller = controller;
        this.tileViews = new ArrayList<>();

        setSpacing(10);
        setPadding(new Insets(10));
        setAlignment(Pos.CENTER);

        setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, new CornerRadii(5), BorderWidths.DEFAULT)));
        setBackground(new Background(new BackgroundFill(Color.rgb(240, 240, 240), CornerRadii.EMPTY, Insets.EMPTY)));

        updateRack();
    }

    public void updateRack() {
        getChildren().clear();
        tileViews.clear();

        Player currentPlayer = controller.getCurrentPlayer();
        Rack rack = currentPlayer.getRack();

        Label playerLabel = new Label("Current Player: " + currentPlayer.getName());
        playerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        playerLabel.setPadding(new Insets(0, 20, 0, 0));
        getChildren().add(playerLabel);

        for (int i = 0; i < rack.size(); i++) {
            Tile tile = rack.getTile(i);
            TileView tileView = new TileView(tile, i);

            if (controller.isTileSelected(i)) {
                tileView.select();
            }

            tileViews.add(tileView);
            getChildren().add(tileView);
        }

        for (int i = 0; i < rack.getEmptySlots(); i++) {
            EmptySlotView emptySlot = new EmptySlotView();
            getChildren().add(emptySlot);
        }
    }

    private class TileView extends StackPane {
        private final Tile tile;
        private final int index;
        private final Label letterLabel;
        private final Label valueLabel;
        private boolean isSelected = false;

        public TileView(Tile tile, int index) {
            this.tile = tile;
            this.index = index;

            setPrefSize(GameConstants.TILE_SIZE, GameConstants.TILE_SIZE);
            setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, new CornerRadii(3), BorderWidths.DEFAULT)));

            if (tile.isBlank()) {
                setBackground(new Background(new BackgroundFill(Color.LIGHTYELLOW, CornerRadii.EMPTY, Insets.EMPTY)));
            } else {
                setBackground(new Background(new BackgroundFill(Color.BURLYWOOD, CornerRadii.EMPTY, Insets.EMPTY)));
            }

            letterLabel = new Label(String.valueOf(tile.getLetter()));
            letterLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            letterLabel.setAlignment(Pos.CENTER);

            if (tile.isBlank() && tile.getLetter() != '*') {
                letterLabel.setTextFill(Color.BLUE);
            }

            valueLabel = new Label(String.valueOf(tile.getValue()));
            valueLabel.setFont(Font.font("Arial", 10));
            valueLabel.setAlignment(Pos.BOTTOM_RIGHT);
            valueLabel.setTranslateX(10);
            valueLabel.setTranslateY(10);

            getChildren().addAll(letterLabel, valueLabel);

            setupEvents();
        }

        private void setupEvents() {
            setOnMouseClicked(event -> {
                if (tile.isBlank() && tile.getLetter() == '*') {
                    BlankTileView dialog = new BlankTileView();
                    char selectedLetter = dialog.showAndWait();
                    if (selectedLetter != '\0') {
                        controller.setBlankTileLetter(index, selectedLetter);
                        updateRack();
                    }
                } else {
                    controller.selectTileFromRack(index);
                    updateRack();
                }
            });

            setOnDragDetected(event -> {
                if (controller.isTileSelected(index)) {
                    if (tile.isBlank() && tile.getLetter() == '*') {
                        BlankTileView dialog = new BlankTileView();
                        char selectedLetter = dialog.showAndWait();
                        if (selectedLetter != '\0') {
                            controller.setBlankTileLetter(index, selectedLetter);
                            updateRack();
                        } else {
                            event.consume();
                            return;
                        }
                    }

                    Dragboard db = startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(String.valueOf(index));
                    db.setContent(content);

                    event.consume();
                } else {
                    controller.selectTileFromRack(index);
                    updateRack();
                }
            });

            setOnDragDone(event -> {
                event.consume();
            });
        }

        public void select() {
            isSelected = true;
            setBackground(new Background(new BackgroundFill(Color.GOLD, CornerRadii.EMPTY, Insets.EMPTY)));
            setBorder(new Border(new BorderStroke(Color.ORANGE, BorderStrokeStyle.SOLID, new CornerRadii(3), new BorderWidths(2))));
        }

    }

    private class EmptySlotView extends StackPane {
        public EmptySlotView() {
            setPrefSize(GameConstants.TILE_SIZE, GameConstants.TILE_SIZE);
            setBorder(new Border(new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.DASHED, new CornerRadii(3), BorderWidths.DEFAULT)));
            setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        }
    }
}