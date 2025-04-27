package utilities;
import model.Dictionary;
import model.Board;
import model.Move;
import model.Tile;
import utilities.BoardUtils;
import utilities.ScoreCalculator;

import java.awt.Point;
import java.util.*;
import java.util.logging.Logger;

public final class WordValidator {
    private static final Logger logger = Logger.getLogger(WordValidator.class.getName());

    private WordValidator() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static List<String> validateWords(Board board, Move move, List<Point> newTilePositions, Dictionary dictionary) {
        List<String> formedWords = new ArrayList<>();

        // Find the main word
        String mainWord = findMainWord(board, move);

        if (mainWord.length() < 2 || !dictionary.isValidWord(mainWord)) {
            logger.fine("Invalid main word: " + mainWord);
            return formedWords; // Empty list indicates invalid placement
        }

        formedWords.add(mainWord);

        // Check all crossing words
        for (Point p : newTilePositions) {
            String crossWord = findCrossWord(board, move.getDirection(), p);

            if (crossWord.length() >= 2) {
                if (!dictionary.isValidWord(crossWord)) {
                    logger.fine("Invalid cross word: " + crossWord);
                    return new ArrayList<>(); // Invalid crossing word
                }
                formedWords.add(crossWord);
            }
        }

        return formedWords;
    }

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

    private static String findCrossWord(Board board, Move.Direction direction, Point position) {
        if (direction == Move.Direction.HORIZONTAL) {
            int startRow = BoardUtils.findWordStart(board, position.x, position.y, false);
            return BoardUtils.getWordAt(board, startRow, position.y, Move.Direction.VERTICAL);
        } else {
            int startCol = BoardUtils.findWordStart(board, position.x, position.y, true);
            return BoardUtils.getWordAt(board, position.x, startCol, Move.Direction.HORIZONTAL);
        }
    }

    public static boolean isValidPlaceMove(Move move, Board board, Dictionary dictionary) {
        int startRow = move.getStartRow();
        int startCol = move.getStartCol();
        Move.Direction direction = move.getDirection();
        List<Tile> tiles = move.getTiles();

        // Basic validations
        if (tiles.isEmpty() ||
                startRow < 0 || startRow >= Board.SIZE ||
                startCol < 0 || startCol >= Board.SIZE) {
            return false;
        }

        // First move must cover center square
        if (board.isEmpty()) {
            boolean touchesCenter = BoardUtils.touchesCenterSquare(move);
            return touchesCenter;
        }

        // Place tiles temporarily on a board copy to validate
        Board tempBoard = BoardUtils.copyBoard(board);
        List<Point> newTilePositions = placeTilesTemporarily(tempBoard, move);

        if (newTilePositions.isEmpty()) {
            return false; // Couldn't place all tiles
        }

        // Check if connected to existing tiles
        boolean connectsToExisting = checkConnectsToExisting(board, newTilePositions);

        // Validate formed words
        List<String> formedWords = validateWords(tempBoard, move, newTilePositions, dictionary);

        return !formedWords.isEmpty() && (connectsToExisting ||
                hasConnectionThroughWords(board, tempBoard, formedWords, newTilePositions));
    }

    private static List<Point> placeTilesTemporarily(Board tempBoard, Move move) {
        int currentRow = move.getStartRow();
        int currentCol = move.getStartCol();
        Move.Direction direction = move.getDirection();
        List<Tile> tiles = move.getTiles();
        List<Point> newTilePositions = new ArrayList<>();

        for (Tile tile : tiles) {
            // Skip occupied squares
            while (currentRow < Board.SIZE && currentCol < Board.SIZE &&
                    tempBoard.getSquare(currentRow, currentCol).hasTile()) {
                if (direction == Move.Direction.HORIZONTAL) {
                    currentCol++;
                } else {
                    currentRow++;
                }
            }

            // Check if we went out of bounds
            if (currentRow >= Board.SIZE || currentCol >= Board.SIZE) {
                return new ArrayList<>(); // Invalid placement
            }

            // Place the tile
            tempBoard.placeTile(currentRow, currentCol, tile);
            newTilePositions.add(new Point(currentRow, currentCol));

            // Move to next position
            if (direction == Move.Direction.HORIZONTAL) {
                currentCol++;
            } else {
                currentRow++;
            }
        }

        return newTilePositions;
    }

    private static boolean checkConnectsToExisting(Board board, List<Point> newTilePositions) {
        for (Point p : newTilePositions) {
            if (BoardUtils.hasAdjacentTile(board, p.x, p.y)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasConnectionThroughWords(Board board, Board tempBoard,
                                                     List<String> formedWords, List<Point> newTilePositions) {
        for (String word : formedWords) {
            Point wordPos = ScoreCalculator.findWordPosition(tempBoard, word);
            if (wordPos == null) continue;

            boolean isHorizontal = ScoreCalculator.isWordHorizontal(tempBoard, word, wordPos);

            // Check if any tile in the word is on the original board
            if (wordContainsExistingTile(board, tempBoard, wordPos, isHorizontal, word.length(), newTilePositions)) {
                return true;
            }
        }
        return false;
    }

    private static boolean wordContainsExistingTile(Board board, Board tempBoard, Point wordPos,
                                                    boolean isHorizontal, int wordLength, List<Point> newTilePositions) {
        int row = wordPos.x;
        int col = wordPos.y;

        for (int i = 0; i < wordLength; i++) {
            Point p = new Point(row, col);

            // If square has a tile in original board and is not one of our newly placed tiles
            if (board.getSquare(row, col).hasTile() && !containsPoint(newTilePositions, p)) {
                return true;
            }

            if (isHorizontal) {
                col++;
            } else {
                row++;
            }
        }
        return false;
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