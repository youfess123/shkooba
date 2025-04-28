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
    // Remove the wordFinder as a field - we'll create a new one each time

    public ComputerPlayer(Player player, int difficultyLevel) {
        this.player = player;
        this.random = new Random();
        this.difficultyLevel = Math.max(1, Math.min(3, difficultyLevel));
        player.setComputer(true);
    }

    public Player getPlayer() {
        return player;
    }

    public Move generateMove(Game game) {
        try {
            logger.info("Computer player generating move at difficulty " + difficultyLevel);

            if (player.getRack().isEmpty()) {
                logger.info("Computer has no tiles, passing");
                return Move.createPassMove(player);
            }

            // CRITICAL FIX: Create a fresh WordFinder with the current board state every time
            WordFinder wordFinder = new WordFinder(game.getDictionary(), game.getBoard());

            // If first move, try simpler approach to guarantee a valid move
            if (game.getBoard().isEmpty()) {
                return findFirstMove(game);
            }

            // For all other moves, use WordFinder
            List<WordPlacement> possiblePlacements = wordFinder.findAllPlacements(player.getRack());

            // Convert to moves
            List<Move> possibleMoves = new ArrayList<>();
            for (WordPlacement placement : possiblePlacements) {
                possibleMoves.add(placement.toMove(player));
            }

            logger.info("Found " + possibleMoves.size() + " possible moves");

            if (possibleMoves.isEmpty()) {
                // Try a backup strategy - direct word formation
                List<Move> backupMoves = findBackupMoves(game);

                if (!backupMoves.isEmpty()) {
                    logger.info("Found " + backupMoves.size() + " backup moves");
                    possibleMoves = backupMoves;
                } else {
                    logger.info("No possible word placements found, using fallback");
                    return generateFallbackMove(game);
                }
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

    // Simple guaranteed first move
    private Move findFirstMove(Game game) {
        Dictionary dictionary = game.getDictionary();
        Rack rack = player.getRack();

        // Just try all 2-letter words through center
        for (int i = 0; i < rack.size(); i++) {
            for (int j = 0; j < rack.size(); j++) {
                if (i != j) {
                    Tile t1 = rack.getTile(i);
                    Tile t2 = rack.getTile(j);
                    String word = "" + t1.getLetter() + t2.getLetter();

                    if (dictionary.isValidWord(word)) {
                        Move move = Move.createPlaceMove(player, 7, 7, Move.Direction.HORIZONTAL);
                        List<Tile> tiles = new ArrayList<>();
                        tiles.add(t1);
                        tiles.add(t2);
                        move.addTiles(tiles);

                        List<String> words = new ArrayList<>();
                        words.add(word);
                        move.setFormedWords(words);
                        move.setScore(t1.getValue() + t2.getValue());

                        return move;
                    }
                }
            }
        }

        // If no valid word, just place a single letter
        Move move = Move.createPlaceMove(player, 7, 7, Move.Direction.HORIZONTAL);
        move.addTiles(Collections.singletonList(rack.getTile(0)));
        return move;
    }

    // Backup strategy for finding moves
    private List<Move> findBackupMoves(Game game) {
        List<Move> moves = new ArrayList<>();

        // Simple implementation: try to add one letter to existing words
        Board board = game.getBoard();
        Dictionary dictionary = game.getDictionary();
        Rack rack = player.getRack();

        // Find all occupied squares
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Square square = board.getSquare(row, col);
                if (square.hasTile()) {
                    // Check all four adjacent positions
                    tryPositionForBackupMove(moves, board, dictionary, rack, row-1, col, Move.Direction.VERTICAL);
                    tryPositionForBackupMove(moves, board, dictionary, rack, row+1, col, Move.Direction.VERTICAL);
                    tryPositionForBackupMove(moves, board, dictionary, rack, row, col-1, Move.Direction.HORIZONTAL);
                    tryPositionForBackupMove(moves, board, dictionary, rack, row, col+1, Move.Direction.HORIZONTAL);
                }
            }
        }

        return moves;
    }

    private void tryPositionForBackupMove(List<Move> moves, Board board, Dictionary dictionary,
                                          Rack rack, int row, int col, Move.Direction direction) {
        // Skip invalid positions
        if (row < 0 || row >= Board.SIZE || col < 0 || col >= Board.SIZE) {
            return;
        }

        // Skip occupied squares
        if (board.getSquare(row, col).hasTile()) {
            return;
        }

        // Try each tile in the rack
        for (int i = 0; i < rack.size(); i++) {
            Tile tile = rack.getTile(i);

            // Create a temporary move
            int startRow = direction == Move.Direction.HORIZONTAL ? row : row;
            int startCol = direction == Move.Direction.HORIZONTAL ? col : col;

            Move move = Move.createPlaceMove(player, startRow, startCol, direction);
            move.addTiles(Collections.singletonList(tile));

            // Create a temporary board with the move applied
            Board tempBoard = new Board();
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    Square square = board.getSquare(r, c);
                    if (square.hasTile()) {
                        tempBoard.placeTile(r, c, square.getTile());
                    }
                }
            }
            tempBoard.placeTile(row, col, tile);

            // Check if it forms valid words
            List<Point> newTilePositions = new ArrayList<>();
            newTilePositions.add(new Point(row, col));
            List<String> formedWords = utilities.WordValidator.validateWords(tempBoard, move, newTilePositions, dictionary);

            if (!formedWords.isEmpty()) {
                // Calculate score
                Set<Point> newPosSet = new HashSet<>(newTilePositions);
                int score = utilities.ScoreCalculator.calculateMoveScore(move, tempBoard, formedWords, newPosSet);

                move.setFormedWords(formedWords);
                move.setScore(score);

                // Add to possible moves
                moves.add(move);
            }
        }
    }

    // Rest of the methods remain the same...
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
        // Existing implementation
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

    // Rest of the class remains the same...
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
}