package controller;


import model.*;
import model.Dictionary;
import utilities.BoardUtils;
import utilities.GameConstants;

import java.awt.Point;
import java.util.*;
import java.util.logging.Logger;

public class ComputerPlayer {
    private static final Logger logger = Logger.getLogger(ComputerPlayer.class.getName());

    private final Player player;
    private final Random random;
    private final int difficultyLevel;

    public ComputerPlayer(Player player, int difficultyLevel) {
        this.player = player;
        this.random = new Random();
        this.difficultyLevel = Math.max(1, Math.min(3, difficultyLevel));
        player.setComputer(true);
    }

    public Player getPlayer() {
        return player;
    }

    // In ComputerPlayer.java

    // Make sure the generateMove method is robust
    public Move generateMove(Game game) {
        try {
            logger.info("Computer player generating move at difficulty " + difficultyLevel);

            if (player.getRack().size() == 0) {
                logger.info("Computer has no tiles, passing");
                return Move.createPassMove(player);
            }

            List<Move> possibleMoves = findPossibleMoves(game);
            logger.info("Found " + possibleMoves.size() + " possible moves");

            if (possibleMoves.isEmpty()) {
                logger.info("No possible word placements found, using fallback");
                return generateFallbackMove(game);
            }

            // Sort moves by score in descending order
            possibleMoves.sort(Comparator.comparing(Move::getScore).reversed());

            // Log scores to help debug AI behavior
            for (int i = 0; i < Math.min(3, possibleMoves.size()); i++) {
                Move m = possibleMoves.get(i);
                logger.info("Potential move " + i + ": " + m.getScore() + " points - "
                        + (m.getFormedWords().isEmpty() ? "No words" : String.join(", ", m.getFormedWords())));
            }

            Move selectedMove = selectMoveByDifficulty(possibleMoves);
            logger.info("Computer selected move with score: " + selectedMove.getScore());

            return selectedMove;
        } catch (Exception e) {
            logger.severe("Error generating computer move: " + e.getMessage() + "\n" + e.getStackTrace()[0]);
            return Move.createPassMove(player);
        }
    }

    private Move selectMoveByDifficulty(List<Move> possibleMoves) {
        // Higher difficulty levels select better moves
        switch (difficultyLevel) {
            case GameConstants.AI_EASY: // Easy - random move with preference for lower scores
                return selectEasyMove(possibleMoves);
            case GameConstants.AI_MEDIUM: // Medium - random from top half
                return selectMediumMove(possibleMoves);
            case GameConstants.AI_HARD: // Hard - random from top 3
                return selectHardMove(possibleMoves);
            default:
                return selectMediumMove(possibleMoves);
        }
    }

    private Move selectEasyMove(List<Move> possibleMoves) {
        // Easy AI chooses randomly but prefers lower scoring moves
        int size = possibleMoves.size();
        int index = random.nextInt(size);

        // 70% chance to pick from the bottom half if available
        if (size > 1 && random.nextDouble() < 0.7) {
            index = size / 2 + random.nextInt(size - size / 2);
        }

        return possibleMoves.get(Math.min(index, size - 1));
    }

    private Move selectMediumMove(List<Move> possibleMoves) {
        // Medium AI chooses randomly from the top half
        int mediumCutoff = Math.max(1, possibleMoves.size() / 2);
        return possibleMoves.get(random.nextInt(mediumCutoff));
    }

    private Move selectHardMove(List<Move> possibleMoves) {
        // Hard AI chooses randomly from the top 3 (or fewer if not enough moves)
        int hardCutoff = Math.min(3, possibleMoves.size());
        return possibleMoves.get(random.nextInt(hardCutoff));
    }

    private Move generateFallbackMove(Game game) {
        try {
            // Try to exchange tiles if there are enough in the bag
            if (game.getTileBag().getTileCount() >= 7) {
                logger.info("Computer: Generating exchange move");
                List<Tile> tilesToExchange = selectOptimalTilesToExchange();

                if (!tilesToExchange.isEmpty()) {
                    logger.info("Computer exchanging " + tilesToExchange.size() + " tiles");
                    return Move.createExchangeMove(player, tilesToExchange);
                }
            }

            // If exchange isn't possible, pass
            logger.info("Computer: Generating pass move");
            return Move.createPassMove(player);
        } catch (Exception e) {
            logger.severe("Error generating fallback move: " + e.getMessage());
            return Move.createPassMove(player);
        }
    }

    private List<Tile> selectOptimalTilesToExchange() {
        Rack rack = player.getRack();
        List<Tile> availableTiles = new ArrayList<>(rack.getTiles());
        List<Tile> tilesToExchange = new ArrayList<>();

        // Score each tile based on usefulness
        Map<Tile, Double> tileScores = scoreTilesForExchange(availableTiles);

        // Sort tiles by score (lowest = exchange first)
        availableTiles.sort(Comparator.comparing(tile -> tileScores.getOrDefault(tile, 0.0)));

        // Determine number of tiles to exchange based on difficulty
        int numToExchange = determineExchangeCount();

        // Select lowest-scoring tiles to exchange
        for (int i = 0; i < numToExchange && i < availableTiles.size(); i++) {
            if (tileScores.getOrDefault(availableTiles.get(i), 0.0) < 0) {
                tilesToExchange.add(availableTiles.get(i));
            }
        }

        return tilesToExchange;
    }

    private Map<Tile, Double> scoreTilesForExchange(List<Tile> availableTiles) {
        Map<Tile, Double> tileScores = new HashMap<>();
        Map<Character, Integer> letterCounts = countLetters(availableTiles);
        int vowelCount = countVowels(availableTiles);

        for (Tile tile : availableTiles) {
            char letter = tile.getLetter();
            double score = 0;

            // High value tiles might be hard to place
            if (tile.getValue() >= 8) {
                score -= 10;
            } else if (tile.getValue() >= 4) {
                score -= 5;
            }

            // Too many of the same consonant is bad
            if (!isVowel(letter) && letterCounts.get(letter) > 2) {
                score -= 8;
            }

            // Balance vowels (2-3 is good)
            if (isVowel(letter)) {
                if (vowelCount <= 2) {
                    score += 10; // Keep vowels if we have few
                } else if (vowelCount > 3) {
                    score -= 5;  // Exchange vowels if we have many
                }
            }

            // Hard-to-use letters
            if (letter == 'Q' || letter == 'Z' || letter == 'X' || letter == 'J') {
                score -= 7;
            }

            tileScores.put(tile, score);
        }

        return tileScores;
    }

    private int determineExchangeCount() {
        switch (difficultyLevel) {
            case GameConstants.AI_EASY: return 4; // Easy - exchange more tiles
            case GameConstants.AI_MEDIUM: return 3; // Medium
            case GameConstants.AI_HARD: return 2; // Hard - exchange fewer tiles
            default: return 3;
        }
    }

    private Map<Character, Integer> countLetters(List<Tile> tiles) {
        Map<Character, Integer> counts = new HashMap<>();
        for (Tile tile : tiles) {
            char letter = tile.getLetter();
            counts.put(letter, counts.getOrDefault(letter, 0) + 1);
        }
        return counts;
    }

    private List<Move> findPossibleMoves(Game game) {
        List<Move> possibleMoves = new ArrayList<>();
        Board board = game.getBoard();
        Dictionary dictionary = game.getDictionary();
        Rack rack = player.getRack();

        // Special case for empty board
        if (board.isEmpty()) {
            findMovesForEmptyBoard(game, possibleMoves);
            return possibleMoves;
        }

        // Find anchor points (empty squares adjacent to placed tiles)
        List<Point> anchorPoints = BoardUtils.findAnchorPoints(board);
        logger.fine("Found " + anchorPoints.size() + " anchor points");

        // For each anchor point, find possible placements
        for (Point anchor : anchorPoints) {
            findPlacementsAt(game, anchor.x, anchor.y, Move.Direction.HORIZONTAL, possibleMoves);
            findPlacementsAt(game, anchor.x, anchor.y, Move.Direction.VERTICAL, possibleMoves);
        }

        return possibleMoves;
    }

    private void findMovesForEmptyBoard(Game game, List<Move> possibleMoves) {
        Dictionary dictionary = game.getDictionary();
        Rack rack = player.getRack();
        String rackLetters = getTilesAsString(rack.getTiles());

        // Simple approach: try all permutations of rack letters through center
        findValidWordsFromRack(rackLetters, dictionary, possibleMoves);
    }

    // Find valid words that can be formed with the rack letters
    private void findValidWordsFromRack(String letters, Dictionary dictionary, List<Move> moves) {
        Set<String> words = new HashSet<>();
        findWordsHelper("", letters, dictionary, words);

        // Try placing each word through the center
        for (String word : words) {
            if (word.length() < 2) continue;

            // Try horizontal placements
            for (int i = 0; i < word.length(); i++) {
                int col = GameConstants.CENTER_SQUARE - i;
                if (col >= 0 && col + word.length() <= Board.SIZE) {
                    Move move = createMoveFromWord(word, GameConstants.CENTER_SQUARE, col,
                            Move.Direction.HORIZONTAL);
                    if (move != null) {
                        moves.add(move);
                    }
                }
            }

            // Try vertical placements
            for (int i = 0; i < word.length(); i++) {
                int row = GameConstants.CENTER_SQUARE - i;
                if (row >= 0 && row + word.length() <= Board.SIZE) {
                    Move move = createMoveFromWord(word, row, GameConstants.CENTER_SQUARE,
                            Move.Direction.VERTICAL);
                    if (move != null) {
                        moves.add(move);
                    }
                }
            }
        }
    }

    private void findWordsHelper(String current, String remaining, Dictionary dictionary, Set<String> words) {
        // Recursive helper to find all word permutations
        if (current.length() > 1 && dictionary.isValidWord(current)) {
            words.add(current);
        }

        if (remaining.isEmpty()) return;

        for (int i = 0; i < remaining.length(); i++) {
            char c = remaining.charAt(i);
            String newCurrent = current + c;
            String newRemaining = remaining.substring(0, i) + remaining.substring(i + 1);
            findWordsHelper(newCurrent, newRemaining, dictionary, words);
        }
    }

    private Move createMoveFromWord(String word, int row, int col, Move.Direction direction) {
        Move move = Move.createPlaceMove(player, row, col, direction);

        // Get tiles needed for the word
        List<Tile> tilesNeeded = new ArrayList<>();
        for (char c : word.toCharArray()) {
            Tile tile = findTileWithLetter(player.getRack().getTiles(), c);
            if (tile == null) return null; // Can't form this word
            tilesNeeded.add(tile);
        }

        move.addTiles(tilesNeeded);

        // Set formed words and score (simple approximation)
        List<String> formedWords = new ArrayList<>();
        formedWords.add(word);
        move.setFormedWords(formedWords);

        // Calculate a score estimate
        int score = 0;
        for (Tile tile : tilesNeeded) {
            score += tile.getValue();
        }
        move.setScore(score);

        return move;
    }

    private void findPlacementsAt(Game game, int row, int col, Move.Direction direction,
                                  List<Move> possibleMoves) {
        // This would be a complex algorithm to find all possible word placements
        // For simplicity, we'll implement a basic version that tries letters from the rack

        Board board = game.getBoard();
        Dictionary dictionary = game.getDictionary();
        Rack rack = player.getRack();

        // Get partial words at this position
        String[] partialWords = BoardUtils.getPartialWordsAt(board, row, col, direction);
        String prefix = partialWords[0];
        String suffix = partialWords[1];

        // For each letter in the rack, see if we can form a valid word
        for (Tile tile : rack.getTiles()) {
            String potentialWord = prefix + tile.getLetter() + suffix;

            if (potentialWord.length() >= 2 && dictionary.isValidWord(potentialWord)) {
                // Create a move
                int startRow = direction == Move.Direction.HORIZONTAL ? row : row - prefix.length();
                int startCol = direction == Move.Direction.HORIZONTAL ? col - prefix.length() : col;

                Move move = Move.createPlaceMove(player, startRow, startCol, direction);
                move.addTiles(Collections.singletonList(tile));

                // Add the word
                List<String> words = new ArrayList<>();
                words.add(potentialWord);
                move.setFormedWords(words);

                // Calculate score (simplified)
                int score = tile.getValue();
                if (direction == Move.Direction.HORIZONTAL && board.getSquare(row, col).getSquareType() == Square.SquareType.DOUBLE_LETTER) {
                    score *= 2;
                } else if (direction == Move.Direction.HORIZONTAL && board.getSquare(row, col).getSquareType() == Square.SquareType.TRIPLE_LETTER) {
                    score *= 3;
                }
                move.setScore(score);

                possibleMoves.add(move);
            }
        }
    }

    private Tile findTileWithLetter(List<Tile> tiles, char letter) {
        for (Tile tile : tiles) {
            if (tile.getLetter() == letter) {
                return tile;
            }
        }
        return null;
    }

    // Utility methods

    private boolean isVowel(char letter) {
        letter = Character.toUpperCase(letter);
        return letter == 'A' || letter == 'E' || letter == 'I' || letter == 'O' || letter == 'U';
    }

    private int countVowels(List<Tile> tiles) {
        int count = 0;
        for (Tile tile : tiles) {
            if (isVowel(tile.getLetter())) {
                count++;
            }
        }
        return count;
    }

    private String getTilesAsString(List<Tile> tiles) {
        StringBuilder sb = new StringBuilder();
        for (Tile tile : tiles) {
            sb.append(tile.getLetter());
        }
        return sb.toString();
    }
}