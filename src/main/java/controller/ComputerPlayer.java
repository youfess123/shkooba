package controller;

import model.*;
import utilities.GameConstants;
import utilities.WordFinder;
import utilities.WordFinder.WordPlacement;
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

    // Main move generation
    public Move generateMove(Game game) {
        logger.info("Computer player generating move at difficulty " + difficultyLevel);

        if (player.getRack().isEmpty()) {
            logger.info("Computer has no tiles, passing");
            return Move.createPassMove(player);
        }

        try {
            WordFinder wordFinder = new WordFinder(game.getDictionary(), game.getBoard());
            List<WordPlacement> placements = wordFinder.findAllPlacements(player.getRack());
            logger.info("Found " + placements.size() + " possible placements");

            if (placements.isEmpty()) {
                return generateFallbackMove(game);
            }

            List<Move> possibleMoves = new ArrayList<>();
            for (WordPlacement placement : placements) {
                possibleMoves.add(placement.toMove(player));
            }

            possibleMoves.sort(Comparator.comparing(Move::getScore).reversed());

            int movsToLog = Math.min(3, possibleMoves.size());
            for (int i = 0; i < movsToLog; i++) {
                Move move = possibleMoves.get(i);
                logger.info(String.format("Potential move %d: %d points - %s",
                        i+1, move.getScore(),
                        move.getFormedWords().isEmpty() ? "No words" : String.join(", ", move.getFormedWords())));
            }

            Move selectedMove = selectMoveByDifficulty(possibleMoves);
            logger.info("Computer selected move with score: " + selectedMove.getScore());

            return selectedMove;
        } catch (Exception e) {
            logger.severe("Error generating computer move: " + e.getMessage());
            return Move.createPassMove(player);
        }
    }

    private Move selectMoveByDifficulty(List<Move> possibleMoves) {
        if (possibleMoves.isEmpty()) {
            throw new IllegalArgumentException("No possible moves to select from");
        }

        switch (difficultyLevel) {
            case GameConstants.AI_EASY:
                if (possibleMoves.size() > 2 && random.nextDouble() < 0.7) {
                    int startIdx = possibleMoves.size() / 2;
                    return possibleMoves.get(startIdx + random.nextInt(possibleMoves.size() - startIdx));
                } else {
                    return possibleMoves.get(random.nextInt(possibleMoves.size()));
                }

            case GameConstants.AI_MEDIUM:
                int mediumCutoff = Math.max(1, (int)(possibleMoves.size() * 0.6));
                return possibleMoves.get(random.nextInt(mediumCutoff));

            case GameConstants.AI_HARD:
                int hardCutoff = Math.min(3, possibleMoves.size());
                return possibleMoves.get(random.nextInt(hardCutoff));

            default:
                return possibleMoves.get(0);
        }
    }

    // Fallback move generation when no word placements are possible
    private Move generateFallbackMove(Game game) {
        if (game.getTileBag().getTileCount() >= 7) {
            logger.info("Computer: Generating exchange move");
            List<Tile> tilesToExchange = selectTilesToExchange();

            if (!tilesToExchange.isEmpty()) {
                logger.info("Computer exchanging " + tilesToExchange.size() + " tiles");
                return Move.createExchangeMove(player, tilesToExchange);
            }
        }

        logger.info("Computer: Generating pass move");
        return Move.createPassMove(player);
    }

    private List<Tile> selectTilesToExchange() {
        Rack rack = player.getRack();
        List<Tile> availableTiles = new ArrayList<>(rack.getTiles());
        List<Tile> tilesToExchange = new ArrayList<>();

        Map<Tile, Integer> tileScores = new HashMap<>();
        Map<Character, Integer> letterCounts = new HashMap<>();
        int vowelCount = 0;

        for (Tile tile : availableTiles) {
            char letter = tile.getLetter();
            letterCounts.put(letter, letterCounts.getOrDefault(letter, 0) + 1);

            if (isVowel(letter)) {
                vowelCount++;
            }
        }

        for (Tile tile : availableTiles) {
            char letter = tile.getLetter();
            int score = 0;

            if (tile.getValue() >= 8) score -= 10;
            else if (tile.getValue() >= 4) score -= 5;

            if (!isVowel(letter) && letterCounts.get(letter) > 2) score -= 8;

            if (isVowel(letter)) {
                if (vowelCount <= 2) score += 10;
                else if (vowelCount > 3) score -= 5;
            }

            if (letter == 'Q' || letter == 'Z' || letter == 'X' || letter == 'J') score -= 7;

            tileScores.put(tile, score);
        }

        availableTiles.sort(Comparator.comparing(tile -> tileScores.getOrDefault(tile, 0)));
        int numToExchange = determineExchangeCount();

        for (int i = 0; i < numToExchange && i < availableTiles.size(); i++) {
            int score = tileScores.getOrDefault(availableTiles.get(i), 0);
            if (score < 0) {
                tilesToExchange.add(availableTiles.get(i));
            }
        }

        if (tilesToExchange.isEmpty() && !availableTiles.isEmpty()) {
            tilesToExchange.add(availableTiles.get(0));
        }

        return tilesToExchange;
    }

    private int determineExchangeCount() {
        return switch (difficultyLevel) {
            case GameConstants.AI_EASY -> 4;
            case GameConstants.AI_MEDIUM -> 3;
            case GameConstants.AI_HARD -> 2;
            default -> 3;
        };
    }

    // Utility methods
    private boolean isVowel(char letter) {
        letter = Character.toUpperCase(letter);
        return letter == 'A' || letter == 'E' || letter == 'I' || letter == 'O' || letter == 'U';
    }
}