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

/**
 * Visual representation of the Scrabble board in the user interface.
 * Renders a grid of squares, handling drag-and-drop of tiles.
 */
public class BoardView extends GridPane {
    private static final Logger logger = Logger.getLogger(BoardView.class.getName());

    private final GameController controller;
    private final SquareView[][] squareViews;

    /**
     * Creates a new board view for the specified controller.
     *
     * @param controller The game controller
     */
    public BoardView(GameController controller) {
        this.controller = controller;
        this.squareViews = new SquareView[Board.SIZE][Board.SIZE];

        initializeLayout();
        initializeSquares();

        logger.fine("Board view initialized");
    }

    /**
     * Sets up the basic layout properties of the board.
     */
    private void initializeLayout() {
        // Set spacing between squares
        setHgap(2);
        setVgap(2);
        setPadding(new Insets(5));

        // Add a border around the entire board
        setBorder(new Border(new BorderStroke(
                Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

        // Set a background color for the board
        setBackground(new Background(new BackgroundFill(
                Color.DARKGRAY, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    /**
     * Initializes all squares on the board view.
     */
    private void initializeSquares() {
        Board board = controller.getBoard();

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Square square = board.getSquare(row, col);
                SquareView squareView = new SquareView(square, row, col);
                squareViews[row][col] = squareView;
                add(squareView, col, row);
            }
        }
    }

    /**
     * Updates the visual state of all squares on the board.
     * Should be called whenever the board state changes.
     */
    public void updateBoard() {
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                squareViews[row][col].update();
            }
        }
    }

    /**
     * Visual representation of a single square on the board.
     */
    private class SquareView extends StackPane {
        private final Square square;
        private final int row;
        private final int col;
        private final Label letterLabel;
        private final Label valueLabel;
        private final Label premiumLabel;
        private boolean isTemporaryTile = false;

        /**
         * Creates a view for a specific square on the board.
         *
         * @param square The square model
         * @param row The row coordinate
         * @param col The column coordinate
         */
        public SquareView(Square square, int row, int col) {
            this.square = square;
            this.row = row;
            this.col = col;

            // Set the size of the square
            setPrefSize(GameConstants.SQUARE_SIZE, GameConstants.SQUARE_SIZE);

            // Set a border around the square
            setBorder(new Border(new BorderStroke(
                    Color.DARKGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0.5))));

            // Create labels for letter, value, and premium indicators
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

            // Add the labels to the square view
            getChildren().addAll(premiumLabel, letterLabel, valueLabel);
            setAlignment(Pos.CENTER);

            // Set up drag-and-drop functionality
            setupDropTarget();

            // Initialize the visual state
            update();
        }

        /**
         * Updates the visual state of this square based on the current game state.
         */
        public void update() {
            // Check if this square has a temporary tile (during placement)
            isTemporaryTile = controller.hasTemporaryTileAt(row, col);

            if (isTemporaryTile) {
                Tile tempTile = controller.getTemporaryTileAt(row, col);
                if (tempTile != null) {
                    updateTemporaryTile(tempTile);
                    return;
                }
            }

            // Update based on the square's actual state
            if (square.hasTile()) {
                updateWithPlacedTile();
            } else {
                updateEmptySquare();
            }
        }

        /**
         * Updates the view to show a temporary tile (during placement).
         *
         * @param tile The temporary tile
         */
        private void updateTemporaryTile(Tile tile) {
            // Set the letter
            letterLabel.setText(String.valueOf(tile.getLetter()));

            // Set the value
            valueLabel.setText(String.valueOf(tile.getValue()));

            // Special styling for blank tiles
            if (tile.isBlank()) {
                letterLabel.setTextFill(Color.BLUE);
                valueLabel.setText("0");
            } else {
                letterLabel.setTextFill(Color.BLACK);
            }

            // Style the tile differently to indicate it's temporary
            letterLabel.setStyle("-fx-background-color: #FFAA00; -fx-padding: 5; -fx-background-radius: 3;");
            premiumLabel.setText("");

            // Use a different background for temporary tiles
            setBackground(new Background(new BackgroundFill(
                    Color.LIGHTYELLOW, CornerRadii.EMPTY, Insets.EMPTY)));

            // Use a special border to highlight the temporary tile
            setBorder(new Border(new BorderStroke(
                    Color.ORANGE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));
        }

        /**
         * Updates the view to show a placed tile.
         */
        private void updateWithPlacedTile() {
            Tile tile = square.getTile();

            // Set the letter
            letterLabel.setText(String.valueOf(tile.getLetter()));

            // Set the value
            valueLabel.setText(String.valueOf(tile.getValue()));

            // Special styling for blank tiles
            if (tile.isBlank()) {
                letterLabel.setTextFill(Color.BLUE);
                valueLabel.setText("0");
            } else {
                letterLabel.setTextFill(Color.BLACK);
            }

            // Style the tile
            letterLabel.setStyle("-fx-background-color: #CD7F32; -fx-padding: 5; -fx-background-radius: 3;");
            premiumLabel.setText("");

            // Reset the border
            setBorder(new Border(new BorderStroke(
                    Color.DARKGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0.5))));

            // Use tile background color
            setBackground(new Background(new BackgroundFill(
                    Color.BURLYWOOD, CornerRadii.EMPTY, Insets.EMPTY)));
        }

        /**
         * Updates the view to show an empty square with its premium type.
         */
        private void updateEmptySquare() {
            // Clear letter and value
            letterLabel.setText("");
            valueLabel.setText("");
            letterLabel.setStyle("");

            // Reset the border
            setBorder(new Border(new BorderStroke(
                    Color.DARKGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0.5))));

            // Set the premium label based on square type
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

            // Set background color based on square type
            Color backgroundColor = getSquareBackgroundColor(squareType);
            setBackground(new Background(new BackgroundFill(
                    backgroundColor, CornerRadii.EMPTY, Insets.EMPTY)));
        }

        /**
         * Gets the appropriate background color for a square type.
         *
         * @param squareType The square type
         * @return The background color
         */
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

        /**
         * Checks if a tile placement at this position would be valid.
         *
         * @return true if placement would be valid, false otherwise
         */
        private boolean isValidPlacement() {
            Board board = controller.getBoard();

            // Can't place on occupied squares
            if (board.getSquare(row, col).hasTile() || controller.hasTemporaryTileAt(row, col)) {
                return false;
            }

            // First move must be on center square
            if (board.isEmpty() && controller.getTemporaryPlacements().isEmpty()) {
                return row == GameConstants.CENTER_SQUARE && col == GameConstants.CENTER_SQUARE;
            }

            // Check if the placement aligns with existing temporary placements
            if (!controller.getTemporaryPlacements().isEmpty()) {
                return controller.isValidTemporaryPlacement(row, col);
            }

            // Otherwise, must be adjacent to an existing tile
            return board.hasAdjacentTile(row, col);
        }

        /**
         * Sets up drag-and-drop functionality for this square.
         */
        private void setupDropTarget() {
            // Handle drag over events
            setOnDragOver(event -> {
                // Only accept if from another source and placement is valid
                if (event.getGestureSource() != this && isValidPlacement()) {
                    event.acceptTransferModes(TransferMode.MOVE);

                    // Highlight the square
                    setStyle("-fx-border-color: gold; -fx-border-width: 2;");

                    // Check for direction constraints
                    if (!controller.getTemporaryPlacements().isEmpty()) {
                        Move.Direction direction = controller.determineDirection();
                        if (direction != null) {
                            setStyle("-fx-border-color: gold; -fx-border-width: 2;");
                        }
                    }
                }
                event.consume();
            });

            // Handle drag entered events
            setOnDragEntered(event -> {
                if (event.getGestureSource() != this && isValidPlacement()) {
                    setStyle("-fx-border-color: gold; -fx-border-width: 2;");
                }
                event.consume();
            });

            // Handle drag exited events
            setOnDragExited(event -> {
                setStyle("-fx-border-color: darkgray; -fx-border-width: 0.5;");
                event.consume();
            });

            // Handle drop events
            setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;

                if (db.hasString() && isValidPlacement()) {
                    try {
                        // The Dragboard contains the index of the tile in the rack
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