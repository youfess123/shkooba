package utilities;
import model.Dictionary;
import model.Board;
import model.Move;
import model.Tile;

import java.awt.Point;
import java.util.*;
import java.util.logging.Logger;

public final class WordValidator {
    private static final Logger logger = Logger.getLogger(WordValidator.class.getName());

    private WordValidator() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Validates words formed by a move, using GADDAG for efficient validation.
     *
     * @param board The game board
     * @param move The move to validate
     * @param newTilePositions The positions of newly placed tiles
     * @param dictionary The game dictionary
     * @return A list of formed words, empty if invalid
     */
    public static List<String> validateWords(Board board, Move move, List<Point> newTilePositions, Dictionary dictionary) {
        // Use the GADDAG for word validation
        return dictionary.getGaddag().validateWords(board, move, newTilePositions);
    }

    /**
     * Validates if a move is a valid place move, using GADDAG for validation.
     *
     * @param move The move to validate
     * @param board The game board
     * @param dictionary The game dictionary
     * @return true if valid, false otherwise
     */
    public static boolean isValidPlaceMove(Move move, Board board, Dictionary dictionary) {
        // Use the GADDAG for move validation
        return dictionary.getGaddag().validateWordPlacement(board, move);
    }

    /**
     * Finds the main word formed by a move.
     *
     * @param board The game board
     * @param move The move
     * @return The main word string
     */
    private static String findMainWord(Board board, Move move) {
        int row = move.getStartRow();
        int col = move.getStartCol();
        Move.Direction direction = move.getDirection();

        if (direction == Move.Direction.HORIZONTAL) {
            int startCol = BoardUtils.findWordStart(board, row, col, true);
            return BoardUtils.getWordAt(board, row, startCol, Move.Direction.HORIZONTAL);
        } else {
            int startRow = BoardUtils.findWordStart(board, row, col, false);
            return BoardUtils.getWordAt(board, startRow, col, Move.Direction.VERTICAL);
        }
    }

    /**
     * Finds a cross word at a position.
     *
     * @param board The game board
     * @param direction The main word direction
     * @param position The position to check
     * @return The cross word
     */
    private static String findCrossWord(Board board, Move.Direction direction, Point position) {
        if (direction == Move.Direction.HORIZONTAL) {
            int startRow = BoardUtils.findWordStart(board, position.x, position.y, false);
            return BoardUtils.getWordAt(board, startRow, position.y, Move.Direction.VERTICAL);
        } else {
            int startCol = BoardUtils.findWordStart(board, position.x, position.y, true);
            return BoardUtils.getWordAt(board, position.x, startCol, Move.Direction.HORIZONTAL);
        }
    }

    public static Point findWordPosition(Board board, String word) {
        return ScoreCalculator.findWordPosition(board, word);
    }

    public static boolean isWordHorizontal(Board board, String word, Point position) {
        return ScoreCalculator.isWordHorizontal(board, word, position);
    }

    private static boolean containsPoint(List<Point> points, Point target) {
        for (Point p : points) {
            if (p.x == target.x && p.y == target.y) {
                return true;
            }
        }
        return false;
    }
}