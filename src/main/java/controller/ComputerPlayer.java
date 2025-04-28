package controller;

import model.*;
import utilities.GameConstants;
import utilities.WordFinder;
import utilities.WordFinder.WordPlacement;

import java.util.*;
import java.util.logging.Logger;

/**
 * AI player that utilizes GADDAG for efficient move generation.
 */
public class ComputerPlayer {
    private static final Logger logger = Logger.getLogger(ComputerPlayer.class.getName());

    private final Player player;
    private final Random random;
    private final int difficultyLevel;

    /**
     * Creates a new computer player.
     *
     * @param player The player model
     * @param difficultyLevel The difficulty level (1-3)
     */
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
     * Generates a move for the computer player.
     *
     * @param game The current game state
     * @return The move to execute
     */
    public Move generateMove(Game game) {
        logger.info("Computer player generating move at difficulty " + difficultyLevel);

        if (player.getRack().isEmpty()) {
            logger.info("Computer has no tiles, passing");
            return Move.createPassMove(player);
        }

        try {
            // Create a new WordFinder for current board state
            WordFinder wordFinder = new WordFinder(game.getDictionary(), game.getBoard());

            // Get all possible placements
            List<WordPlacement> placements = wordFinder.findAllPlacements(player.getRack());
            logger.info("Found " + placements.size() + " possible placements");

            if (placements.isEmpty()) {
                return generateFallbackMove(game);
            }

            // Convert to moves
            List<Move> possibleMoves = new ArrayList<>();
            for (WordPlacement placement : placements) {
                possibleMoves.add(placement.toMove(player));
            }

            // Sort by score (descending)
            possibleMoves.sort(Comparator.comparing(Move::getScore).reversed());

            // Log top moves for debugging
            int movsToLog = Math.min(3, possibleMoves.size());
            for (int i = 0; i < movsToLog; i++) {
                Move move = possibleMoves.get(i);
                logger.info(String.format("Potential move %d: %d points - %s",
                        i+1, move.getScore(),
                        move.getFormedWords().isEmpty() ? "No words" : String.join(", ", move.getFormedWords())));
            }

            // Select based on difficulty
            Move selectedMove = selectMoveByDifficulty(possibleMoves);
            logger.info("Computer selected move with score: " + selectedMove.getScore());

            return selectedMove;
        } catch (Exception e) {
            logger.severe("Error generating computer move: " + e.getMessage());
            return Move.createPassMove(player);
        }
    }

    /**
     * Selects a move based on difficulty level.
     */
    private Move selectMoveByDifficulty(List<Move> possibleMoves) {
        if (possibleMoves.isEmpty()) {
            throw new IllegalArgumentException("No possible moves to select from");
        }

        switch (difficultyLevel) {
            case GameConstants.AI_EASY:
                // Easy - pick somewhat randomly, biased toward easier moves
                if (possibleMoves.size() > 2 && random.nextDouble() < 0.7) {
                    // 70% chance to pick from bottom half
                    int startIdx = possibleMoves.size() / 2;
                    return possibleMoves.get(startIdx + random.nextInt(possibleMoves.size() - startIdx));
                } else {
                    return possibleMoves.get(random.nextInt(possibleMoves.size()));
                }

            case GameConstants.AI_MEDIUM:
                // Medium - pick from top 60% of moves
                int mediumCutoff = Math.max(1, (int)(possibleMoves.size() * 0.6));
                return possibleMoves.get(random.nextInt(mediumCutoff));

            case GameConstants.AI_HARD:
                // Hard - pick from top 3 moves
                int hardCutoff = Math.min(3, possibleMoves.size());
                return possibleMoves.get(random.nextInt(hardCutoff));

            default:
                return possibleMoves.get(0); // Default to best move
        }
    }

    /**
     * Generates a fallback move when no word placements are found.
     */
    private Move generateFallbackMove(Game game) {
        // Try to exchange tiles if there are enough in the bag
        if (game.getTileBag().getTileCount() >= 7) {
            logger.info("Computer: Generating exchange move");
            List<Tile> tilesToExchange = selectTilesToExchange();

            if (!tilesToExchange.isEmpty()) {
                logger.info("Computer exchanging " + tilesToExchange.size() + " tiles");
                return Move.createExchangeMove(player, tilesToExchange);
            }
        }

        // If exchange isn't possible, pass
        logger.info("Computer: Generating pass move");
        return Move.createPassMove(player);
    }

    /**
     * Selects tiles to exchange based on optimal strategy.
     */
    private List<Tile> selectTilesToExchange() {
        Rack rack = player.getRack();
        List<Tile> availableTiles = new ArrayList<>(rack.getTiles());
        List<Tile> tilesToExchange = new ArrayList<>();

        // Score each tile based on usefulness
        Map<Tile, Integer> tileScores = new HashMap<>();
        Map<Character, Integer> letterCounts = new HashMap<>();
        int vowelCount = 0;

        // Count letters and vowels
        for (Tile tile : availableTiles) {
            char letter = tile.getLetter();
            letterCounts.put(letter, letterCounts.getOrDefault(letter, 0) + 1);

            if (isVowel(letter)) {
                vowelCount++;
            }
        }

        // Score each tile
        for (Tile tile : availableTiles) {
            char letter = tile.getLetter();
            int score = 0;

            // High value tiles are harder to place
            if (tile.getValue() >= 8) score -= 10;
            else if (tile.getValue() >= 4) score -= 5;

            // Too many duplicates of consonants is bad
            if (!isVowel(letter) && letterCounts.get(letter) > 2) score -= 8;

            // Balance vowels
            if (isVowel(letter)) {
                if (vowelCount <= 2) score += 10; // Keep vowels if few
                else if (vowelCount > 3) score -= 5; // Exchange if too many
            }

            // Hard to use letters
            if (letter == 'Q' || letter == 'Z' || letter == 'X' || letter == 'J') score -= 7;

            tileScores.put(tile, score);
        }

        // Sort tiles by score (ascending = worse first)
        availableTiles.sort(Comparator.comparing(tile -> tileScores.getOrDefault(tile, 0)));

        // Determine how many to exchange
        int numToExchange = determineExchangeCount();

        // Take the lowest scoring tiles
        for (int i = 0; i < numToExchange && i < availableTiles.size(); i++) {
            int score = tileScores.getOrDefault(availableTiles.get(i), 0);
            if (score < 0) {
                tilesToExchange.add(availableTiles.get(i));
            }
        }

        // Always exchange at least one tile if possible
        if (tilesToExchange.isEmpty() && !availableTiles.isEmpty()) {
            tilesToExchange.add(availableTiles.get(0));
        }

        return tilesToExchange;
    }

    /**
     * Determines how many tiles to exchange based on difficulty.
     */
    private int determineExchangeCount() {
        return switch (difficultyLevel) {
            case GameConstants.AI_EASY -> 4;    // Easy - exchange more tiles
            case GameConstants.AI_MEDIUM -> 3;  // Medium
            case GameConstants.AI_HARD -> 2;    // Hard - exchange fewer tiles
            default -> 3;
        };
    }

    /**
     * Checks if a letter is a vowel.
     */
    private boolean isVowel(char letter) {
        letter = Character.toUpperCase(letter);
        return letter == 'A' || letter == 'E' || letter == 'I' || letter == 'O' || letter == 'U';
    }
}