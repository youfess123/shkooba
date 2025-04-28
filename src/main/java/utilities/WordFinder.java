package utilities;

import model.*;
import model.Dictionary;

import java.awt.Point;
import java.util.*;
import java.util.logging.Logger;

/**
 * Utility class for finding possible word placements on a Scrabble board using GADDAG.
 */
public class WordFinder {
    private static final Logger logger = Logger.getLogger(WordFinder.class.getName());

    private final Dictionary dictionary;
    private final Board board;

    public WordFinder(Dictionary dictionary, Board board) {
        this.dictionary = dictionary;
        this.board = board;
    }

    /**
     * Represents a word placement on the board.
     */
    public static class WordPlacement {
        private final String word;
        private final int row;
        private final int col;
        private final Move.Direction direction;
        private final List<Tile> tilesNeeded;
        private final int score;
        private final List<String> crossWords;

        public WordPlacement(String word, int row, int col, Move.Direction direction,
                             List<Tile> tilesNeeded, int score, List<String> crossWords) {
            this.word = word;
            this.row = row;
            this.col = col;
            this.direction = direction;
            this.tilesNeeded = new ArrayList<>(tilesNeeded);
            this.score = score;
            this.crossWords = new ArrayList<>(crossWords);
        }

        public Move toMove(Player player) {
            Move move = Move.createPlaceMove(player, row, col, direction);
            move.addTiles(tilesNeeded);

            List<String> allWords = new ArrayList<>(crossWords);
            allWords.add(0, word);
            move.setFormedWords(allWords);

            move.setScore(score);
            return move;
        }

        // Getters
        public String getWord() { return word; }
        public int getRow() { return row; }
        public int getCol() { return col; }
        public Move.Direction getDirection() { return direction; }
        public int getScore() { return score; }

        @Override
        public String toString() {
            return String.format("%s at (%d,%d) %s for %d points",
                    word, row + 1, col + 1,
                    direction == Move.Direction.HORIZONTAL ? "horizontal" : "vertical",
                    score);
        }
    }

    /**
     * Finds all possible word placements for a rack.
     * @param rack The player's rack
     * @return A list of possible placements
     */
    public List<WordPlacement> findAllPlacements(Rack rack) {
        List<WordPlacement> placements = new ArrayList<>();
        Gaddag gaddag = dictionary.getGaddag();
        String rackLetters = getAvailableLetters(rack);
        List<Tile> rackTiles = new ArrayList<>(rack.getTiles());

        // Special case for first move
        if (board.isEmpty()) {
            findPlacementsForFirstMove(rackLetters, rackTiles, placements);
            return placements;
        }

        // Find anchor points
        Map<Point, String[]> horizontalContext = new HashMap<>();
        Map<Point, String[]> verticalContext = new HashMap<>();

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                if (board.getSquare(row, col).hasTile()) continue;
                if (!hasAdjacentTile(row, col)) continue;

                // Valid anchor point - find prefixes and suffixes
                Point point = new Point(row, col);
                horizontalContext.put(point, getPartialWordsAt(row, col, Move.Direction.HORIZONTAL));
                verticalContext.put(point, getPartialWordsAt(row, col, Move.Direction.VERTICAL));
            }
        }

        // For each anchor point, try all possible placements
        for (Point anchor : horizontalContext.keySet()) {
            // Try horizontal placements
            String[] hContext = horizontalContext.get(anchor);
            findPlacements(anchor.x, anchor.y, Move.Direction.HORIZONTAL,
                    hContext[0], hContext[1], rackLetters, rackTiles, placements);

            // Try vertical placements
            String[] vContext = verticalContext.get(anchor);
            findPlacements(anchor.x, anchor.y, Move.Direction.VERTICAL,
                    vContext[0], vContext[1], rackLetters, rackTiles, placements);
        }

        // Sort by score
        placements.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));
        return placements;
    }

    /**
     * Finds word placements at an anchor point in a specific direction.
     */
    private void findPlacements(int row, int col, Move.Direction direction,
                                String prefix, String suffix,
                                String rackLetters, List<Tile> rackTiles,
                                List<WordPlacement> placements) {
        // Try to use each letter in the rack as an anchor
        for (char letter : getUniqueLetters(rackLetters)) {
            Set<String> words = dictionary.getGaddag().getWordsFrom(
                    rackLetters, letter, true, true);

            for (String word : words) {
                // Check if word can be placed with the anchor letter at this position
                for (int pos = 0; pos < word.length(); pos++) {
                    if (word.charAt(pos) != letter) continue;

                    // Calculate start position
                    int startRow = direction == Move.Direction.HORIZONTAL ? row : row - pos;
                    int startCol = direction == Move.Direction.HORIZONTAL ? col - pos : col;

                    // Skip invalid positions
                    if (startRow < 0 || startCol < 0 ||
                            (direction == Move.Direction.HORIZONTAL && startCol + word.length() > Board.SIZE) ||
                            (direction == Move.Direction.VERTICAL && startRow + word.length() > Board.SIZE)) {
                        continue;
                    }

                    // Check if the word is compatible with existing tiles and context
                    if (!isWordCompatible(word, direction, startRow, startCol, prefix, suffix)) {
                        continue;
                    }

                    // Try to place the word
                    Board tempBoard = board.copy();
                    List<Point> newPositions = new ArrayList<>();
                    List<Tile> tilesNeeded = placeTilesOnTempBoard(
                            word, direction, startRow, startCol, rackTiles, tempBoard, newPositions);

                    if (tilesNeeded == null) continue; // Don't have the needed tiles

                    // Validate the placement
                    List<String> formedWords = WordValidator.validateWords(
                            tempBoard, createTestMove(direction, startRow, startCol), newPositions, dictionary);

                    if (formedWords.isEmpty()) continue; // Invalid placement

                    // Calculate score
                    Set<Point> newPositionsSet = new HashSet<>(newPositions);
                    int score = ScoreCalculator.calculateMoveScore(
                            createTestMove(direction, startRow, startCol),
                            tempBoard, formedWords, newPositionsSet);

                    // Create placement
                    placements.add(new WordPlacement(
                            word, startRow, startCol, direction, tilesNeeded, score, formedWords));
                }
            }
        }
    }

    /**
     * Creates a test move for validation and scoring.
     */
    private Move createTestMove(Move.Direction direction, int startRow, int startCol) {
        return Move.createPlaceMove(new Player("temp"), startRow, startCol, direction);
    }

    /**
     * Places tiles on a temporary board for testing.
     * @return List of tiles needed from rack, or null if not possible
     */
    private List<Tile> placeTilesOnTempBoard(String word, Move.Direction direction,
                                             int startRow, int startCol, List<Tile> rackTiles,
                                             Board tempBoard, List<Point> newPositions) {
        List<Tile> tilesNeeded = new ArrayList<>();
        List<Tile> availableTiles = new ArrayList<>(rackTiles);

        int row = startRow;
        int col = startCol;

        for (int i = 0; i < word.length(); i++) {
            char letter = word.charAt(i);

            if (tempBoard.getSquare(row, col).hasTile()) {
                // Check if existing tile matches needed letter
                Tile existingTile = tempBoard.getSquare(row, col).getTile();
                if (existingTile.getLetter() != letter) {
                    return null; // Mismatch
                }
            } else {
                // Need a new tile from rack
                Tile tileToUse = findTileForLetter(letter, availableTiles);
                if (tileToUse == null) return null; // Don't have needed tile

                // Remove from available tiles
                availableTiles.remove(tileToUse);

                // Add to tiles needed
                tilesNeeded.add(tileToUse);

                // Place on temp board
                tempBoard.placeTile(row, col, tileToUse);
                newPositions.add(new Point(row, col));
            }

            // Move to next position
            if (direction == Move.Direction.HORIZONTAL) {
                col++;
            } else {
                row++;
            }
        }

        return tilesNeeded;
    }

    /**
     * Finds a tile for a letter from available tiles.
     * Prefers exact matches but will use blanks if necessary.
     */
    private Tile findTileForLetter(char letter, List<Tile> availableTiles) {
        // First try to find an exact match
        for (Tile tile : availableTiles) {
            if (tile.getLetter() == letter) {
                return tile;
            }
        }

        // If no exact match, try to use a blank tile
        for (Tile tile : availableTiles) {
            if (tile.isBlank() && tile.getLetter() == '*') {
                return Tile.createBlankTile(letter);
            }
        }

        return null; // No suitable tile found
    }

    /**
     * Checks if a word is compatible with existing context.
     */
    private boolean isWordCompatible(String word, Move.Direction direction,
                                     int startRow, int startCol,
                                     String prefix, String suffix) {
        // Calculate where the anchor is
        int anchorRow = direction == Move.Direction.HORIZONTAL ? startRow : startRow + prefix.length();
        int anchorCol = direction == Move.Direction.HORIZONTAL ? startCol + prefix.length() : startCol;

        // Check prefix compatibility
        if (!prefix.isEmpty()) {
            if (word.length() <= prefix.length() ||
                    !word.substring(0, prefix.length()).equals(prefix)) {
                return false;
            }
        }

        // Check suffix compatibility
        if (!suffix.isEmpty()) {
            int suffixStart = direction == Move.Direction.HORIZONTAL ?
                    anchorCol - startCol + 1 : anchorRow - startRow + 1;

            if (word.length() < suffixStart + suffix.length() ||
                    !word.substring(suffixStart).equals(suffix)) {
                return false;
            }
        }

        // Check compatibility with existing tiles on the board
        int row = startRow;
        int col = startCol;

        for (int i = 0; i < word.length(); i++) {
            if (row >= Board.SIZE || col >= Board.SIZE) return false;

            if (board.getSquare(row, col).hasTile()) {
                if (board.getSquare(row, col).getTile().getLetter() != word.charAt(i)) {
                    return false; // Mismatch with existing tile
                }
            }

            if (direction == Move.Direction.HORIZONTAL) {
                col++;
            } else {
                row++;
            }
        }

        return true;
    }

    /**
     * Finds placements for the first move (must go through center).
     */
    private void findPlacementsForFirstMove(String rackLetters, List<Tile> rackTiles,
                                            List<WordPlacement> placements) {
        int center = GameConstants.CENTER_SQUARE;
        Gaddag gaddag = dictionary.getGaddag();

        // Try each letter as an anchor at the center
        for (char letter : getUniqueLetters(rackLetters)) {
            // Get words that can be formed with this letter
            Set<String> words = gaddag.getWordsFrom(rackLetters, letter, true, true);

            for (String word : words) {
                // Find all positions where this letter occurs
                for (int pos = 0; pos < word.length(); pos++) {
                    if (word.charAt(pos) != letter) continue;

                    // Try horizontal placement
                    int startCol = center - pos;
                    if (startCol >= 0 && startCol + word.length() <= Board.SIZE) {
                        List<Tile> tiles = getTilesForWord(word, rackTiles);
                        if (tiles != null) {
                            int score = calculateFirstMoveScore(word, tiles);
                            placements.add(new WordPlacement(
                                    word, center, startCol, Move.Direction.HORIZONTAL,
                                    tiles, score, Collections.emptyList()));
                        }
                    }

                    // Try vertical placement
                    int startRow = center - pos;
                    if (startRow >= 0 && startRow + word.length() <= Board.SIZE) {
                        List<Tile> tiles = getTilesForWord(word, rackTiles);
                        if (tiles != null) {
                            int score = calculateFirstMoveScore(word, tiles);
                            placements.add(new WordPlacement(
                                    word, startRow, center, Move.Direction.VERTICAL,
                                    tiles, score, Collections.emptyList()));
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculates score for first move.
     */
    private int calculateFirstMoveScore(String word, List<Tile> tiles) {
        int score = 0;
        for (Tile tile : tiles) {
            score += tile.getValue();
        }

        // Double for center square
        score *= 2;

        // Add bingo bonus if using 7 tiles
        if (tiles.size() == 7) {
            score += GameConstants.BINGO_BONUS;
        }

        return score;
    }

    /**
     * Gets the partial words (prefix and suffix) at a specific position.
     */
    private String[] getPartialWordsAt(int row, int col, Move.Direction direction) {
        StringBuilder prefix = new StringBuilder();
        StringBuilder suffix = new StringBuilder();

        if (direction == Move.Direction.HORIZONTAL) {
            // Get prefix (letters to the left)
            int c = col - 1;
            while (c >= 0 && board.getSquare(row, c).hasTile()) {
                prefix.insert(0, board.getSquare(row, c).getTile().getLetter());
                c--;
            }

            // Get suffix (letters to the right)
            c = col + 1;
            while (c < Board.SIZE && board.getSquare(row, c).hasTile()) {
                suffix.append(board.getSquare(row, c).getTile().getLetter());
                c++;
            }
        } else { // VERTICAL
            // Get prefix (letters above)
            int r = row - 1;
            while (r >= 0 && board.getSquare(r, col).hasTile()) {
                prefix.insert(0, board.getSquare(r, col).getTile().getLetter());
                r--;
            }

            // Get suffix (letters below)
            r = row + 1;
            while (r < Board.SIZE && board.getSquare(r, col).hasTile()) {
                suffix.append(board.getSquare(r, col).getTile().getLetter());
                r++;
            }
        }

        return new String[] {prefix.toString(), suffix.toString()};
    }

    /**
     * Gets tiles needed for a word from available tiles.
     */
    private List<Tile> getTilesForWord(String word, List<Tile> availableTiles) {
        // Group tiles by letter
        Map<Character, List<Tile>> tilesByLetter = new HashMap<>();
        for (Tile tile : availableTiles) {
            char letter = tile.getLetter();
            tilesByLetter.computeIfAbsent(letter, k -> new ArrayList<>()).add(tile);
        }

        List<Tile> result = new ArrayList<>();

        for (char c : word.toCharArray()) {
            if (tilesByLetter.containsKey(c) && !tilesByLetter.get(c).isEmpty()) {
                // Use matching tile
                Tile tile = tilesByLetter.get(c).remove(0);
                result.add(tile);
            } else if (tilesByLetter.containsKey('*') && !tilesByLetter.get('*').isEmpty()) {
                // Use blank tile
                tilesByLetter.get('*').remove(0);
                result.add(Tile.createBlankTile(c));
            } else {
                return null; // Can't form word
            }
        }

        return result;
    }

    /**
     * Gets all letters in the rack as a string.
     */
    private String getAvailableLetters(Rack rack) {
        StringBuilder sb = new StringBuilder();
        for (Tile tile : rack.getTiles()) {
            sb.append(tile.getLetter());
        }
        return sb.toString();
    }

    /**
     * Gets the unique letters in a string.
     */
    private Set<Character> getUniqueLetters(String letters) {
        Set<Character> uniqueLetters = new HashSet<>();
        for (char c : letters.toCharArray()) {
            uniqueLetters.add(c);
        }
        return uniqueLetters;
    }

    /**
     * Checks if a position has adjacent tiles.
     */
    private boolean hasAdjacentTile(int row, int col) {
        if (row > 0 && board.getSquare(row - 1, col).hasTile()) return true;
        if (row < Board.SIZE - 1 && board.getSquare(row + 1, col).hasTile()) return true;
        if (col > 0 && board.getSquare(row, col - 1).hasTile()) return true;
        if (col < Board.SIZE - 1 && board.getSquare(row, col + 1).hasTile()) return true;
        return false;
    }
}