package controller;

import model.*;
import model.Dictionary;
import utilities.BoardUtils;
import utilities.GameConstants;
import utilities.WordFinder;
import utilities.WordFinder.WordPlacement;

import java.awt.Point;
import java.util.*;
import java.util.logging.Logger;

public class ComputerPlayer {
    private static final Logger logger = Logger.getLogger(ComputerPlayer.class.getName());

    private final Player player;
    private final Random random;
    private final int difficultyLevel;
    private WordFinder wordFinder;

    public ComputerPlayer(Player player, int difficultyLevel) {
        this.player = player;
        this.random = new Random();
        this.difficultyLevel = Math.max(1, Math.min(3, difficultyLevel));
        player.setComputer(true);
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * Initializes the WordFinder if needed.
     *
     * @param game The current game
     */
    private void ensureWordFinderInitialized(Game game) {
        if (wordFinder == null) {
            wordFinder = new WordFinder(game.getDictionary(), game.getBoard());
        }
    }

    /**
     * Generates a move for the computer player using GADDAG-based word finding.
     *
     * @param game The current game
     * @return The generated move
     */
    public Move generateMove(Game game) {
        try {
            logger.info("Computer player generating move at difficulty " + difficultyLevel);

            if (player.getRack().isEmpty()) {
                logger.info("Computer has no tiles, passing");
                return Move.createPassMove(player);
            }

            // Ensure WordFinder is initialized
            ensureWordFinderInitialized(game);

            // Use WordFinder to get all possible placements
            List<WordPlacement> possiblePlacements = wordFinder.findAllPlacements(player.getRack());

            // Convert to moves
            List<Move> possibleMoves = new ArrayList<>();
            for (WordPlacement placement : possiblePlacements) {
                possibleMoves.add(placement.toMove(player));
            }

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
        // First, ensure moves are sorted by score in descending order
        possibleMoves.sort(Comparator.comparing(Move::getScore).reversed());

        switch (difficultyLevel) {
            case 1: // Easy - pick a random move, but bias toward easier ones
                if (possibleMoves.size() > 2 && random.nextDouble() < 0.7) {
                    // 70% chance to pick from bottom half for easy mode
                    int startIdx = possibleMoves.size() / 2;
                    return possibleMoves.get(startIdx + random.nextInt(possibleMoves.size() - startIdx));
                } else {
                    return possibleMoves.get(random.nextInt(possibleMoves.size()));
                }

            case 2: // Medium - pick from top 60% of moves
                int mediumCutoff = Math.max(1, (int)(possibleMoves.size() * 0.6));
                return possibleMoves.get(random.nextInt(mediumCutoff));

            case 3: // Hard - pick from top 3 moves
                int hardCutoff = Math.min(3, possibleMoves.size());
                return possibleMoves.get(random.nextInt(hardCutoff));

            default:
                return possibleMoves.getFirst(); // Default to best move
        }
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
        return switch (difficultyLevel) {
            case GameConstants.AI_EASY -> 4; // Easy - exchange more tiles
            case GameConstants.AI_MEDIUM -> 3; // Medium
            case GameConstants.AI_HARD -> 2; // Hard - exchange fewer tiles
            default -> 3;
        };
    }

    private Map<Character, Integer> countLetters(List<Tile> tiles) {
        Map<Character, Integer> counts = new HashMap<>();
        for (Tile tile : tiles) {
            char letter = tile.getLetter();
            counts.put(letter, counts.getOrDefault(letter, 0) + 1);
        }
        return counts;
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