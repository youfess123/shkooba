package view;

import controller.GameController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.Board;
import model.Move;
import model.Square;
import model.Tile;
import utilities.GameConstants;

import java.awt.Point;
import java.util.logging.Logger;

public class BoardView extends GridPane {
    private static final Logger logger = Logger.getLogger(BoardView.class.getName());

    private final GameController controller;
    private final SquareView[][] squareViews;

    // Initialization methods
    public BoardView(GameController controller) {
        this.controller = controller;
        this.squareViews = new SquareView[Board.SIZE][Board.SIZE];

        initializeLayout();
        addCoordinateLabels();
        initializeSquares();
        logger.fine("Board view initialized with coordinates");
    }

    private void initializeLayout() {
        setHgap(2);
        setVgap(2);
        setPadding(new Insets(5));
        setBorder(new Border(new BorderStroke(
                Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        setBackground(new Background(new BackgroundFill(
                Color.DARKGRAY, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    private void addCoordinateLabels() {
        Label cornerLabel = new Label("");
        cornerLabel.setPrefSize(20, 20);
        cornerLabel.setAlignment(Pos.CENTER);
        cornerLabel.setStyle("-fx-background-color: #888888; -fx-text-fill: white;");
        add(cornerLabel, 0, 0);

        for (int col = 0; col < Board.SIZE; col++) {
            Label colLabel = new Label(Integer.toString(col + 1));
            colLabel.setPrefSize(GameConstants.SQUARE_SIZE, 20);
            colLabel.setAlignment(Pos.CENTER);
            colLabel.setStyle("-fx-background-color: #888888; -fx-text-fill: white; -fx-font-weight: bold;");
            add(colLabel, col + 1, 0);
        }

        for (int row = 0; row < Board.SIZE; row++) {
            Label rowLabel = new Label(Integer.toString(row + 1));
            rowLabel.setPrefSize(20, GameConstants.SQUARE_SIZE);
            rowLabel.setAlignment(Pos.CENTER);
            rowLabel.setStyle("-fx-background-color: #888888; -fx-text-fill: white; -fx-font-weight: bold;");
            add(rowLabel, 0, row + 1);
        }
    }

    private void initializeSquares() {
        Board board = controller.getBoard();

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Square square = board.getSquare(row, col);
                SquareView squareView = new SquareView(square, row, col);
                squareViews[row][col] = squareView;
                add(squareView, col + 1, row + 1);
            }
        }
    }

    // Update methods
    public void updateBoard() {
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                squareViews[row][col].update();
            }
        }
    }

    // SquareView inner class
    private class SquareView extends StackPane {
        private final Square square;
        private final int row;
        private final int col;
        private final Label letterLabel;
        private final Label valueLabel;
        private final Label premiumLabel;
        private boolean isTemporaryTile = false;

        // Initialization
        public SquareView(Square square, int row, int col) {
            this.square = square;
            this.row = row;
            this.col = col;

            setPrefSize(GameConstants.SQUARE_SIZE, GameConstants.SQUARE_SIZE);
            setBorder(new Border(new BorderStroke(
                    Color.DARKGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0.5))));

            letterLabel = new Label();
            letterLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            letterLabel.setAlignment(Pos.CENTER);

            valueLabel = new Label();
            valueLabel.setFont(Font.font("Arial", 8));
            valueLabel.setTextFill(Color.BLACK);
            valueLabel.setAlignment(Pos.BOTTOM_RIGHT);
            valueLabel.setTranslateX(10);
            valueLabel.setTranslateY(12);

            premiumLabel = new Label();
            premiumLabel.setFont(Font.font("Arial", 10));
            premiumLabel.setAlignment(Pos.CENTER);

            getChildren().addAll(premiumLabel, letterLabel, valueLabel);
            setAlignment(Pos.CENTER);

            setupDropTarget();
            update();
        }

        // Update methods
        public void update() {
            isTemporaryTile = controller.hasTemporaryTileAt(row, col);

            if (isTemporaryTile) {
                Tile tempTile = controller.getTemporaryTileAt(row, col);
                if (tempTile != null) {
                    updateTemporaryTile(tempTile);
                    return;
                }
            }

            if (square.hasTile()) {
                updateWithPlacedTile();
            } else {
                updateEmptySquare();
            }
        }

        private void updateTemporaryTile(Tile tile) {
            letterLabel.setText(String.valueOf(tile.getLetter()));
            valueLabel.setText(String.valueOf(tile.getValue()));

            if (tile.isBlank()) {
                letterLabel.setTextFill(Color.BLUE);
                valueLabel.setText("0");
            } else {
                letterLabel.setTextFill(Color.BLACK);
            }

            letterLabel.setStyle("-fx-background-color: #FFAA00; -fx-padding: 5; -fx-background-radius: 3;");
            premiumLabel.setText("");

            setBackground(new Background(new BackgroundFill(
                    Color.LIGHTYELLOW, CornerRadii.EMPTY, Insets.EMPTY)));
            setBorder(new Border(new BorderStroke(
                    Color.ORANGE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));
        }

        private void updateWithPlacedTile() {
            Tile tile = square.getTile();

            letterLabel.setText(String.valueOf(tile.getLetter()));
            valueLabel.setText(String.valueOf(tile.getValue()));

            if (tile.isBlank()) {
                letterLabel.setTextFill(Color.BLUE);
                valueLabel.setText("0");
            } else {
                letterLabel.setTextFill(Color.BLACK);
            }

            letterLabel.setStyle("-fx-background-color: #CD7F32; -fx-padding: 5; -fx-background-radius: 3;");
            premiumLabel.setText("");

            setBorder(new Border(new BorderStroke(
                    Color.DARKGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0.5))));
            setBackground(new Background(new BackgroundFill(
                    Color.BURLYWOOD, CornerRadii.EMPTY, Insets.EMPTY)));
        }

        private void updateEmptySquare() {
            letterLabel.setText("");
            valueLabel.setText("");
            letterLabel.setStyle("");

            setBorder(new Border(new BorderStroke(
                    Color.DARKGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0.5))));

            Square.SquareType squareType = square.getSquareType();
            switch (squareType) {
                case DOUBLE_LETTER:
                    premiumLabel.setText("DL");
                    premiumLabel.setTextFill(Color.WHITE);
                    break;
                case TRIPLE_LETTER:
                    premiumLabel.setText("TL");
                    premiumLabel.setTextFill(Color.WHITE);
                    break;
                case DOUBLE_WORD:
                    premiumLabel.setText("DW");
                    premiumLabel.setTextFill(Color.WHITE);
                    break;
                case TRIPLE_WORD:
                    premiumLabel.setText("TW");
                    premiumLabel.setTextFill(Color.WHITE);
                    break;
                case CENTER:
                    premiumLabel.setText("★");
                    premiumLabel.setTextFill(Color.BLACK);
                    break;
                default:
                    premiumLabel.setText("");
                    break;
            }

            Color backgroundColor = getSquareBackgroundColor(squareType);
            setBackground(new Background(new BackgroundFill(
                    backgroundColor, CornerRadii.EMPTY, Insets.EMPTY)));
        }

        // Utility methods
        private Color getSquareBackgroundColor(Square.SquareType squareType) {
            switch (squareType) {
                case DOUBLE_LETTER:
                    return Color.LIGHTBLUE;
                case TRIPLE_LETTER:
                    return Color.BLUE;
                case DOUBLE_WORD:
                case CENTER:
                    return Color.LIGHTPINK;
                case TRIPLE_WORD:
                    return Color.RED;
                default:
                    return Color.BEIGE;
            }
        }

        private boolean isValidPlacement() {
            Board board = controller.getBoard();

            if (board.getSquare(row, col).hasTile() || controller.hasTemporaryTileAt(row, col)) {
                return false;
            }

            if (board.isEmpty() && controller.getTemporaryPlacements().isEmpty()) {
                return row == GameConstants.CENTER_SQUARE && col == GameConstants.CENTER_SQUARE;
            }

            if (!controller.getTemporaryPlacements().isEmpty()) {
                return controller.isValidTemporaryPlacement(row, col);
            }

            return board.hasAdjacentTile(row, col);
        }

        // Drag and drop handling
        private void setupDropTarget() {
            setOnDragOver(event -> {
                if (event.getGestureSource() != this && isValidPlacement()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                    setStyle("-fx-border-color: gold; -fx-border-width: 2;");

                    if (!controller.getTemporaryPlacements().isEmpty()) {
                        Move.Direction direction = controller.determineDirection();
                        if (direction != null) {
                            setStyle("-fx-border-color: gold; -fx-border-width: 2;");
                        }
                    }
                }
                event.consume();
            });

            setOnDragEntered(event -> {
                if (event.getGestureSource() != this && isValidPlacement()) {
                    setStyle("-fx-border-color: gold; -fx-border-width: 2;");
                }
                event.consume();
            });

            setOnDragExited(event -> {
                setStyle("-fx-border-color: darkgray; -fx-border-width: 0.5;");
                event.consume();
            });

            setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;

                if (db.hasString() && isValidPlacement()) {
                    try {
                        int tileIndex = Integer.parseInt(db.getString());
                        success = controller.placeTileTemporarily(tileIndex, row, col);

                        if (success) {
                            logger.fine("Tile placed temporarily at (" + row + ", " + col + ")");
                        }
                    } catch (NumberFormatException e) {
                        logger.warning("Invalid drag data: " + e.getMessage());
                    }
                }

                event.setDropCompleted(success);
                event.consume();
            });
        }
    }
}