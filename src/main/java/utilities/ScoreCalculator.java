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

    private ScoreCalculator() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static int calculateMoveScore(Move move, Board board, List<String> formedWords,
                                         Set<Point> newTilePositions) {
        int totalScore = 0;

        // Calculate score for each formed word
        for (String word : formedWords) {
            // Find the position and orientation of the word on the board
            Point wordPos = findWordPosition(board, word);
            if (wordPos == null) continue;

            boolean isHorizontal = isWordHorizontal(board, word, wordPos);

            // Calculate the score for this word
            int wordScore = calculateWordScore(
                    word, wordPos.x, wordPos.y, isHorizontal, board, newTilePositions);
            totalScore += wordScore;

            logger.fine("Word '" + word + "' scored " + wordScore + " points");
        }

        // Add bingo bonus if all 7 tiles were used
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

        // Process each letter in the word
        for (int i = 0; i < word.length(); i++) {
            Square square = board.getSquare(row, col);
            Point currentPoint = new Point(row, col);
            Tile tile = square.getTile();

            // Get letter value, accounting for blank tiles
            int letterValue = tile.isBlank() ? 0 : tile.getValue();
            int effectiveValue = letterValue;

            // Apply premium square effects for newly placed tiles
            if (newTilePositions.contains(currentPoint) && !square.isPremiumUsed()) {
                Square.SquareType squareType = square.getSquareType();

                // Apply letter multipliers
                switch (squareType) {
                    case DOUBLE_LETTER:
                        effectiveValue = letterValue * 2;
                        break;
                    case TRIPLE_LETTER:
                        effectiveValue = letterValue * 3;
                        break;
                }

                // Collect word multipliers
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

            // Move to next position
            if (isHorizontal) {
                col++;
            } else {
                row++;
            }
        }

        // Apply word multiplier
        return score * wordMultiplier;
    }

    public static String getScoreBreakdown(String word, int startRow, int startCol, boolean isHorizontal,
                                           Board board, Set<Point> newTilePositions) {
        StringBuilder breakdown = new StringBuilder();
        int score = 0;
        int wordMultiplier = 1;
        int row = startRow;
        int col = startCol;

        breakdown.append("Score for '").append(word).append("': ");

        for (int i = 0; i < word.length(); i++) {
            Square square = board.getSquare(row, col);
            Point currentPoint = new Point(row, col);
            Tile tile = square.getTile();

            int letterValue = tile.isBlank() ? 0 : tile.getValue();
            int effectiveValue = letterValue;

            // Apply multipliers only for newly placed tiles
            if (newTilePositions.contains(currentPoint) && !square.isPremiumUsed()) {
                if (square.getSquareType() == Square.SquareType.DOUBLE_LETTER) {
                    effectiveValue = letterValue * 2;
                    breakdown.append(tile.getLetter()).append("(").append(letterValue).append("×2) + ");
                } else if (square.getSquareType() == Square.SquareType.TRIPLE_LETTER) {
                    effectiveValue = letterValue * 3;
                    breakdown.append(tile.getLetter()).append("(").append(letterValue).append("×3) + ");
                } else {
                    breakdown.append(tile.getLetter()).append("(").append(letterValue).append(") + ");
                }

                if (square.getSquareType() == Square.SquareType.DOUBLE_WORD ||
                        square.getSquareType() == Square.SquareType.CENTER) {
                    wordMultiplier *= 2;
                } else if (square.getSquareType() == Square.SquareType.TRIPLE_WORD) {
                    wordMultiplier *= 3;
                }
            } else {
                breakdown.append(tile.getLetter()).append("(").append(letterValue).append(") + ");
            }

            score += effectiveValue;

            if (isHorizontal) {
                col++;
            } else {
                row++;
            }
        }

        // Remove the trailing " + "
        if (breakdown.length() > 3) {
            breakdown.setLength(breakdown.length() - 3);
        }

        int finalScore = score * wordMultiplier;

        if (wordMultiplier > 1) {
            breakdown.append(" = ").append(score).append(" × ").append(wordMultiplier);
        }

        breakdown.append(" = ").append(finalScore);

        return breakdown.toString();
    }

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

        // Check if the word fits horizontally
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