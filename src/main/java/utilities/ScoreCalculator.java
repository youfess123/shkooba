package utilities;

import model.Board;
import model.Move;
import model.Square;
import model.Tile;
import java.awt.Point;
import java.util.*;
import java.util.logging.Logger;

public final class ScoreCalculator {
    private static final Logger logger = Logger.getLogger(ScoreCalculator.class.getName());

    // Prevent instantiation
    private ScoreCalculator() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    // Move scoring methods
    public static int calculateMoveScore(Move move, Board board, List<String> formedWords,
                                         Set<Point> newTilePositions) {
        int totalScore = 0;

        for (String word : formedWords) {
            Point wordPos = findWordPosition(board, word);
            if (wordPos == null) continue;

            boolean isHorizontal = isWordHorizontal(board, word, wordPos);
            int wordScore = calculateWordScore(word, wordPos.x, wordPos.y, isHorizontal, board, newTilePositions);
            totalScore += wordScore;

            logger.fine("Word '" + word + "' scored " + wordScore + " points");
        }

        if (move.getTiles().size() == GameConstants.RACK_CAPACITY) {
            totalScore += GameConstants.BINGO_BONUS;
            logger.info("Bingo bonus of " + GameConstants.BINGO_BONUS + " points awarded");
        }

        return totalScore;
    }

    public static int calculateWordScore(String word, int startRow, int startCol, boolean isHorizontal,
                                         Board board, Set<Point> newTilePositions) {
        int score = 0;
        int wordMultiplier = 1;
        int row = startRow;
        int col = startCol;

        for (int i = 0; i < word.length(); i++) {
            Square square = board.getSquare(row, col);
            Point currentPoint = new Point(row, col);
            Tile tile = square.getTile();

            int letterValue = tile.isBlank() ? 0 : tile.getValue();
            int effectiveValue = letterValue;

            if (newTilePositions.contains(currentPoint) && !square.isPremiumUsed()) {
                Square.SquareType squareType = square.getSquareType();

                switch (squareType) {
                    case DOUBLE_LETTER:
                        effectiveValue = letterValue * 2;
                        break;
                    case TRIPLE_LETTER:
                        effectiveValue = letterValue * 3;
                        break;
                }

                switch (squareType) {
                    case DOUBLE_WORD:
                    case CENTER:
                        wordMultiplier *= 2;
                        break;
                    case TRIPLE_WORD:
                        wordMultiplier *= 3;
                        break;
                }
            }

            score += effectiveValue;

            if (isHorizontal) {
                col++;
            } else {
                row++;
            }
        }

        return score * wordMultiplier;
    }

    // Word finding methods
    public static Point findWordPosition(Board board, String word) {
        // Check horizontal words
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                if (col + word.length() <= Board.SIZE) {
                    boolean match = true;
                    for (int i = 0; i < word.length(); i++) {
                        Square square = board.getSquare(row, col + i);
                        if (!square.hasTile() || square.getTile().getLetter() != word.charAt(i)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        return new Point(row, col);
                    }
                }
            }
        }

        // Check vertical words
        for (int col = 0; col < Board.SIZE; col++) {
            for (int row = 0; row < Board.SIZE; row++) {
                if (row + word.length() <= Board.SIZE) {
                    boolean match = true;
                    for (int i = 0; i < word.length(); i++) {
                        Square square = board.getSquare(row + i, col);
                        if (!square.hasTile() || square.getTile().getLetter() != word.charAt(i)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        return new Point(row, col);
                    }
                }
            }
        }

        return null;
    }

    public static boolean isWordHorizontal(Board board, String word, Point position) {
        int row = position.x;
        int col = position.y;

        if (col + word.length() <= Board.SIZE) {
            boolean match = true;
            for (int i = 0; i < word.length(); i++) {
                Square square = board.getSquare(row, col + i);
                if (!square.hasTile() || square.getTile().getLetter() != word.charAt(i)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }

        return false;
    }
}