package utilities;

import model.*;

import java.awt.Point;
import java.util.*;
import model.Dictionary;

/**
 * Enhanced utility class for finding possible word placements on a Scrabble board.
 * Uses the GADDAG data structure for efficient word finding.
 */
public class WordFinder {
    private final Dictionary dictionary;
    private final Board board;

    /**
     * Creates a new WordFinder.
     *
     * @param dictionary The game dictionary
     * @param board The game board
     */
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

        /**
         * Creates a new WordPlacement.
         *
         * @param word The word to place
         * @param row The starting row
         * @param col The starting column
         * @param direction The direction of placement
         * @param tilesNeeded The tiles needed from the rack
         * @param score The score for this placement
         * @param crossWords Any cross-words formed
         */
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

        /**
         * Converts this placement to a move for a player.
         *
         * @param player The player making the move
         * @return The move
         */
        public Move toMove(Player player) {
            Move move = Move.createPlaceMove(player, row, col, direction);
            move.addTiles(tilesNeeded);

            List<String> allWords = new ArrayList<>(crossWords);
            allWords.add(0, word); // Main word first
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
     *
     * @param rack The player's rack
     * @return A list of possible placements
     */
    public List<WordPlacement> findAllPlacements(Rack rack) {
        List<WordPlacement> placements = new ArrayList<>();
        Gaddag gaddag = dictionary.getGaddag();

        // Convert rack to string for easier processing
        StringBuilder rackLetters = new StringBuilder();
        for (int i = 0; i < rack.size(); i++) {
            Tile tile = rack.getTile(i);
            rackLetters.append(tile.getLetter());
        }
        String rackStr = rackLetters.toString();

        // Special case for first move
        if (board.isEmpty()) {
            findPlacementsForFirstMove(rackStr, rack, placements);
        } else {
            // Find anchor points (empty squares adjacent to placed tiles)
            List<Point> anchorPoints = findAnchorPoints();

            // For each anchor point, try to place words
            for (Point anchor : anchorPoints) {
                findPlacementsAtAnchor(anchor.x, anchor.y, Move.Direction.HORIZONTAL,
                        rackStr, rack, placements);

                findPlacementsAtAnchor(anchor.x, anchor.y, Move.Direction.VERTICAL,
                        rackStr, rack, placements);
            }
        }

        // Sort by score (descending)
        placements.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));
        return placements;
    }

    /**
     * Finds anchor points on the board.
     *
     * @return A list of anchor points
     */
    private List<Point> findAnchorPoints() {
        List<Point> anchors = new ArrayList<>();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                if (board.getSquare(row, col).hasTile()) {
                    continue;
                }
                if (hasAdjacentTile(row, col)) {
                    anchors.add(new Point(row, col));
                }
            }
        }
        return anchors;
    }

    /**
     * Checks if a position has adjacent tiles.
     *
     * @param row The row coordinate
     * @param col The column coordinate
     * @return true if has adjacent tiles, false otherwise
     */
    private boolean hasAdjacentTile(int row, int col) {
        if (row > 0 && board.getSquare(row - 1, col).hasTile()) return true;
        if (row < Board.SIZE - 1 && board.getSquare(row + 1, col).hasTile()) return true;
        if (col > 0 && board.getSquare(row, col - 1).hasTile()) return true;
        if (col < Board.SIZE - 1 && board.getSquare(row, col + 1).hasTile()) return true;
        return false;
    }

    /**
     * Finds placements for the first move (must go through center).
     *
     * @param rackLetters The letters in the rack
     * @param rack The player's rack
     * @param placements The list to add placements to
     */
    private void findPlacementsForFirstMove(String rackLetters, Rack rack, List<WordPlacement> placements) {
        // Extract GADDAG from dictionary
        Gaddag gaddag = dictionary.getGaddag();

        // Try to place words horizontally through center
        for (int length = 2; length <= Math.min(rackLetters.length(), 7); length++) {
            // For each possible word length
            generateWords("", rackLetters, length, (word) -> {
                // For each potential word of this length
                if (gaddag.contains(word)) {
                    // For each possible position of the word through center
                    for (int i = 0; i < word.length(); i++) {
                        int startCol = GameConstants.CENTER_SQUARE - i;
                        if (startCol >= 0 && startCol + word.length() <= Board.SIZE) {
                            // Create and validate placement
                            tryCreatePlacement(word, GameConstants.CENTER_SQUARE, startCol,
                                    Move.Direction.HORIZONTAL, rack, placements);
                        }
                    }
                }
            });
        }

        // Try to place words vertically through center
        for (int length = 2; length <= Math.min(rackLetters.length(), 7); length++) {
            // For each possible word length
            generateWords("", rackLetters, length, (word) -> {
                // For each potential word of this length
                if (gaddag.contains(word)) {
                    // For each possible position of the word through center
                    for (int i = 0; i < word.length(); i++) {
                        int startRow = GameConstants.CENTER_SQUARE - i;
                        if (startRow >= 0 && startRow + word.length() <= Board.SIZE) {
                            // Create and validate placement
                            tryCreatePlacement(word, startRow, GameConstants.CENTER_SQUARE,
                                    Move.Direction.VERTICAL, rack, placements);
                        }
                    }
                }
            });
        }
    }

    /**
     * Interface for word generation callback.
     */
    private interface WordCallback {
        void onWord(String word);
    }

    /**
     * Recursively generates all possible words of a given length from a set of letters.
     *
     * @param current Current word being built
     * @param letters Available letters
     * @param length Target word length
     * @param callback Function to call for each valid word
     */
    private void generateWords(String current, String letters, int length, WordCallback callback) {
        if (current.length() == length) {
            callback.onWord(current);
            return;
        }

        for (int i = 0; i < letters.length(); i++) {
            char letter = letters.charAt(i);
            String newCurrent = current + letter;
            String newLetters = letters.substring(0, i) + letters.substring(i + 1);
            generateWords(newCurrent, newLetters, length, callback);
        }
    }

    /**
     * Finds placements at a specific anchor point using GADDAG.
     *
     * @param row The row coordinate
     * @param col The column coordinate
     * @param direction The direction to search
     * @param rackLetters The letters in the rack
     * @param rack The player's rack
     * @param placements The list to add placements to
     */
    private void findPlacementsAtAnchor(int row, int col, Move.Direction direction,
                                        String rackLetters, Rack rack, List<WordPlacement> placements) {
        // Get partial words at this position
        String[] partialWords = getPartialWordsAt(row, col, direction);
        String prefix = partialWords[0];
        String suffix = partialWords[1];

        // Create a temporary board for testing
        Board tempBoard = new Board();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Square square = board.getSquare(r, c);
                if (square.hasTile()) {
                    tempBoard.placeTile(r, c, square.getTile());
                }
            }
        }

        // For each letter in the rack
        for (char letter : getUniqueLetters(rackLetters)) {
            // Use GADDAG to find all words with this letter at the anchor
            Set<String> possibleWords = dictionary.getGaddag().getWordsFrom(
                    rackLetters, letter, !prefix.isEmpty(), !suffix.isEmpty());

            for (String word : possibleWords) {
                // Check if this word can be placed with the current constraints
                if (canPlaceWord(word, prefix, suffix, letter, row, col, direction)) {
                    // Calculate starting position
                    int startRow, startCol;
                    if (direction == Move.Direction.HORIZONTAL) {
                        startRow = row;
                        int letterPosition = word.indexOf(letter);
                        startCol = col - letterPosition;
                    } else {
                        int letterPosition = word.indexOf(letter);
                        startRow = row - letterPosition;
                        startCol = col;
                    }

                    // Ensure the placement is valid
                    if (isValidPlacementStart(startRow, startCol, word.length(), direction)) {
                        // Get tiles needed for this word
                        List<Tile> tilesNeeded = getTilesForWord(word, rack, direction, startRow, startCol);
                        if (tilesNeeded != null) {
                            // Calculate score
                            List<String> crossWords = findCrossWords(tempBoard, word, direction, startRow, startCol);
                            if (!crossWords.isEmpty()) {
                                int score = calculateScore(word, crossWords, tilesNeeded, direction, startRow, startCol);

                                // Create placement
                                WordPlacement placement = new WordPlacement(
                                        word, startRow, startCol, direction, tilesNeeded, score, crossWords);
                                placements.add(placement);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if a word can be placed with given constraints.
     */
    private boolean canPlaceWord(String word, String prefix, String suffix, char anchorLetter,
                                 int row, int col, Move.Direction direction) {
        // Check if the word contains the anchor letter
        if (word.indexOf(anchorLetter) == -1) {
            return false;
        }

        // Check if the word can fit on the board
        int letterPos = word.indexOf(anchorLetter);
        int startRow = direction == Move.Direction.HORIZONTAL ? row : row - letterPos;
        int startCol = direction == Move.Direction.HORIZONTAL ? col - letterPos : col;

        if (startRow < 0 || startCol < 0 ||
                (direction == Move.Direction.HORIZONTAL && startCol + word.length() > Board.SIZE) ||
                (direction == Move.Direction.VERTICAL && startRow + word.length() > Board.SIZE)) {
            return false;
        }

        // Check if the word is compatible with existing prefix/suffix
        if (!prefix.isEmpty()) {
            if (letterPos < prefix.length()) {
                return false; // Word doesn't have enough space for prefix
            }
            // Check if prefix matches
            for (int i = 0; i < prefix.length(); i++) {
                if (word.charAt(letterPos - prefix.length() + i) != prefix.charAt(i)) {
                    return false;
                }
            }
        }

        if (!suffix.isEmpty()) {
            if (letterPos + 1 + suffix.length() > word.length()) {
                return false; // Word doesn't have enough space for suffix
            }
            // Check if suffix matches
            for (int i = 0; i < suffix.length(); i++) {
                if (word.charAt(letterPos + 1 + i) != suffix.charAt(i)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks if placement starts at a valid position.
     */
    private boolean isValidPlacementStart(int startRow, int startCol, int wordLength, Move.Direction direction) {
        if (startRow < 0 || startCol < 0) {
            return false;
        }

        if (direction == Move.Direction.HORIZONTAL && startCol + wordLength > Board.SIZE) {
            return false;
        }

        if (direction == Move.Direction.VERTICAL && startRow + wordLength > Board.SIZE) {
            return false;
        }

        return true;
    }

    /**
     * Finds cross-words formed by a placement.
     */
    private List<String> findCrossWords(Board board, String word, Move.Direction direction,
                                        int startRow, int startCol) {
        List<String> crossWords = new ArrayList<>();

        // Implement cross-word finding logic here
        // This is a simplified version - a complete implementation would need more detail

        return crossWords;
    }

    /**
     * Calculates score for a word placement.
     */
    private int calculateScore(String word, List<String> crossWords, List<Tile> tiles,
                               Move.Direction direction, int startRow, int startCol) {
        // Implement score calculation based on board premium squares
        // This is a simplified version - a complete implementation would need more detail

        int score = 0;
        for (Tile tile : tiles) {
            score += tile.getValue();
        }

        // Add bingo bonus if using all 7 tiles
        if (tiles.size() == 7) {
            score += 50;
        }

        return score;
    }

    /**
     * Gets the partial words (prefix and suffix) at a specific position.
     *
     * @param row The row coordinate
     * @param col The column coordinate
     * @param direction The direction to search
     * @return Array with [prefix, suffix]
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
     * Gets the unique letters in a string.
     *
     * @param rack The string of letters
     * @return A set of unique letters
     */
    private Set<Character> getUniqueLetters(String rack) {
        Set<Character> letters = new HashSet<>();
        for (char c : rack.toCharArray()) {
            letters.add(Character.toUpperCase(c));
        }
        return letters;
    }

    /**
     * Creates and validates a placement.
     *
     * @param word The word to place
     * @param row The starting row
     * @param col The starting column
     * @param direction The direction of placement
     * @param rack The player's rack
     * @param placements The list to add the placement to
     */
    private void tryCreatePlacement(String word, int row, int col, Move.Direction direction,
                                    Rack rack, List<WordPlacement> placements) {
        // Get tiles for this word
        List<Tile> tilesNeeded = getTilesForWord(word, rack.getTiles());
        if (tilesNeeded == null) return; // Don't have the tiles

        // Place tiles on a temporary board to validate
        Board tempBoard = new Board();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Square square = board.getSquare(r, c);
                if (square.hasTile()) {
                    tempBoard.placeTile(r, c, square.getTile());
                }
            }
        }

        // Place the new word
        Set<Point> newPositions = new HashSet<>();
        int currentRow = row;
        int currentCol = col;

        for (int i = 0; i < word.length(); i++) {
            if (!tempBoard.getSquare(currentRow, currentCol).hasTile()) {
                Tile tile = null;
                for (Tile t : tilesNeeded) {
                    if (t.getLetter() == word.charAt(i)) {
                        tile = t;
                        tilesNeeded.remove(t);
                        break;
                    }
                }

                if (tile != null) {
                    tempBoard.placeTile(currentRow, currentCol, tile);
                    newPositions.add(new Point(currentRow, currentCol));
                }
            }

            if (direction == Move.Direction.HORIZONTAL) {
                currentCol++;
            } else {
                currentRow++;
            }
        }

        // No cross-words for first move
        List<String> crossWords = new ArrayList<>();

        // Calculate score
        int score = calculateScore(tempBoard, row, col, word, direction, newPositions, crossWords);

        // Create the placement
        WordPlacement placement = new WordPlacement(
                word, row, col, direction,
                getTilesForWord(word, rack.getTiles()), // Get fresh tiles
                score, crossWords
        );

        placements.add(placement);
    }

    /**
     * Calculates the score for a word placement.
     */
    private int calculateScore(Board board, int row, int col, String word,
                               Move.Direction direction, Set<Point> newPositions,
                               List<String> crossWords) {
        // Simplified score calculation
        int score = 0;
        for (int i = 0; i < word.length(); i++) {
            int r = direction == Move.Direction.HORIZONTAL ? row : row + i;
            int c = direction == Move.Direction.HORIZONTAL ? col + i : col;

            if (r < Board.SIZE && c < Board.SIZE) {
                Square square = board.getSquare(r, c);
                if (square.hasTile()) {
                    Tile tile = square.getTile();
                    score += tile.getValue();
                }
            }
        }

        // Add bingo bonus if all 7 tiles used
        if (newPositions.size() == 7) {
            score += 50;
        }

        return score;
    }

    /**
     * Gets the tiles needed to form a word.
     *
     * @param word The word to form
     * @param rackTiles The available tiles
     * @return The needed tiles, or null if not possible
     */
    private List<Tile> getTilesForWord(String word, List<Tile> rackTiles) {
        // Group tiles by letter
        Map<Character, List<Tile>> availableTiles = new HashMap<>();
        for (Tile tile : rackTiles) {
            char letter = tile.getLetter();
            if (!availableTiles.containsKey(letter)) {
                availableTiles.put(letter, new ArrayList<>());
            }
            availableTiles.get(letter).add(tile);
        }

        List<Tile> result = new ArrayList<>();

        // Try to match each letter in the word
        for (char c : word.toCharArray()) {
            if (availableTiles.containsKey(c) && !availableTiles.get(c).isEmpty()) {
                // Use a regular tile if available
                Tile tile = availableTiles.get(c).remove(0);
                result.add(tile);
            } else if (availableTiles.containsKey('*') && !availableTiles.get('*').isEmpty()) {
                // Use a blank tile if available
                Tile blankTile = availableTiles.get('*').remove(0);
                Tile letterTile = Tile.createBlankTile(c);
                result.add(letterTile);
            } else {
                // Can't form the word with available tiles
                return null;
            }
        }

        return result;
    }

    private List<Tile> getTilesForWord(String word, Rack rack, Move.Direction direction, int startRow, int startCol) {
        // Implementation similar to getTilesForWord(String, List<Tile>)
        // but with additional checks for tiles already on the board
        return getTilesForWord(word, rack.getTiles());
    }
}