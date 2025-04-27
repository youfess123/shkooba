package utilities;


import model.*;

import java.awt.Point;
import java.util.*;
import model.Dictionary;

/**
 * Utility class for finding possible word placements on a Scrabble board.
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
     * Finds placements at a specific anchor point.
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

        // For each letter in the rack
        for (char letter : getUniqueLetters(rackLetters)) {
            // Try to form a word with this letter at the anchor
            findWordsWithLetterAt(row, col, letter, direction, rackLetters, rack, placements);
        }
    }

    /**
     * Finds words that can be formed with a specific letter at an anchor position.
     *
     * @param row The row coordinate
     * @param col The column coordinate
     * @param letter The letter to place at the anchor
     * @param direction The direction to search
     * @param rackLetters The letters in the rack
     * @param rack The player's rack
     * @param placements The list to add placements to
     */
    private void findWordsWithLetterAt(int row, int col, char letter, Move.Direction direction,
                                       String rackLetters, Rack rack, List<WordPlacement> placements) {
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

        // Get existing letters around this position
        String[] partialWords = getPartialWordsAt(row, col, direction);
        String prefix = partialWords[0];
        String suffix = partialWords[1];

        // Find a tile in the rack with this letter
        int letterIndex = -1;
        for (int i = 0; i < rack.size(); i++) {
            if (rack.getTile(i).getLetter() == letter) {
                letterIndex = i;
                break;
            }
        }
        if (letterIndex == -1) return; // Letter not in rack

        // Form the potential word
        String potentialWord = prefix + letter + suffix;
        if (potentialWord.length() < 2) return; // Too short

        // Calculate start position
        int startRow, startCol;
        if (direction == Move.Direction.HORIZONTAL) {
            startRow = row;
            startCol = col - prefix.length();
        } else {
            startRow = row - prefix.length();
            startCol = col;
        }
        if (startRow < 0 || startCol < 0) return; // Invalid start position

        // Verify we have the tiles needed
        List<Tile> tilesNeeded = new ArrayList<>();
        Set<Point> newTilePositions = new HashSet<>();

        // Add the anchor tile
        Tile anchorTile = rack.getTile(letterIndex);
        tilesNeeded.add(anchorTile);
        newTilePositions.add(new Point(row, col));
        tempBoard.placeTile(row, col, anchorTile);

        // Check if we have the tiles for the prefix
        for (int i = 0; i < prefix.length(); i++) {
            int r = direction == Move.Direction.HORIZONTAL ? startRow : startRow + i;
            int c = direction == Move.Direction.HORIZONTAL ? startCol + i : startCol;

            if (board.getSquare(r, c).hasTile()) {
                // If the square already has a tile, check if it matches what we need
                if (board.getSquare(r, c).getTile().getLetter() != prefix.charAt(i)) {
                    return; // Tile doesn't match
                }
            } else {
                // We need to place a tile here
                char neededLetter = prefix.charAt(i);
                Tile tileThatMatches = null;

                // Find a tile in the rack that matches this letter
                for (int j = 0; j < rack.size(); j++) {
                    Tile rackTile = rack.getTile(j);
                    if (rackTile.getLetter() == neededLetter && !tilesNeeded.contains(rackTile)) {
                        tileThatMatches = rackTile;
                        break;
                    }
                }

                if (tileThatMatches == null) {
                    return; // Don't have the required tile
                }

                tilesNeeded.add(tileThatMatches);
                tempBoard.placeTile(r, c, tileThatMatches);
                newTilePositions.add(new Point(r, c));
            }
        }

        // Check if we have the tiles for the suffix
        for (int i = 0; i < suffix.length(); i++) {
            int r = direction == Move.Direction.HORIZONTAL ? row : row + i + 1;
            int c = direction == Move.Direction.HORIZONTAL ? col + i + 1 : col;

            if (board.getSquare(r, c).hasTile()) {
                // If the square already has a tile, check if it matches what we need
                if (board.getSquare(r, c).getTile().getLetter() != suffix.charAt(i)) {
                    return; // Tile doesn't match
                }
            } else {
                // We need to place a tile here
                char neededLetter = suffix.charAt(i);
                Tile tileThatMatches = null;

                // Find a tile in the rack that matches this letter
                for (int j = 0; j < rack.size(); j++) {
                    Tile rackTile = rack.getTile(j);
                    if (rackTile.getLetter() == neededLetter && !tilesNeeded.contains(rackTile)) {
                        tileThatMatches = rackTile;
                        break;
                    }
                }

                if (tileThatMatches == null) {
                    return; // Don't have the required tile
                }

                tilesNeeded.add(tileThatMatches);
                tempBoard.placeTile(r, c, tileThatMatches);
                newTilePositions.add(new Point(r, c));
            }
        }

        // Validate the word with the dictionary
        if (!dictionary.isValidWord(potentialWord)) {
            return; // Not a valid word
        }

        // Check for cross-words
        List<String> crossWords = new ArrayList<>();
        for (Point p : newTilePositions) {
            String crossWord;
            if (direction == Move.Direction.HORIZONTAL) {
                crossWord = getWordAt(tempBoard, p.x, p.y, Move.Direction.VERTICAL);
            } else {
                crossWord = getWordAt(tempBoard, p.x, p.y, Move.Direction.HORIZONTAL);
            }

            if (crossWord.length() >= 2) {
                if (!dictionary.isValidWord(crossWord)) {
                    return; // Invalid cross-word
                }
                crossWords.add(crossWord);
            }
        }

        // Calculate score
        int score = calculateScore(tempBoard, startRow, startCol, potentialWord,
                direction, newTilePositions, crossWords);

        // Create and add the word placement
        WordPlacement placement = new WordPlacement(
                potentialWord, startRow, startCol, direction,
                tilesNeeded, score, crossWords
        );

        placements.add(placement);
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
     * Gets a word at a specific position in a specific direction.
     *
     * @param board The game board
     * @param row The row coordinate
     * @param col The column coordinate
     * @param direction The direction to search
     * @return The word string
     */
    private String getWordAt(Board board, int row, int col, Move.Direction direction) {
        // Find the starting position of the word
        int startRow, startCol;

        if (direction == Move.Direction.HORIZONTAL) {
            startRow = row;
            startCol = col;

            // Move to the leftmost letter
            while (startCol > 0 && board.getSquare(startRow, startCol - 1).hasTile()) {
                startCol--;
            }
        } else {
            startRow = row;
            startCol = col;

            // Move to the topmost letter
            while (startRow > 0 && board.getSquare(startRow - 1, startCol).hasTile()) {
                startRow--;
            }
        }

        // Build the word
        StringBuilder word = new StringBuilder();
        int currentRow = startRow;
        int currentCol = startCol;

        while (currentRow < Board.SIZE && currentCol < Board.SIZE) {
            Square square = board.getSquare(currentRow, currentCol);

            if (!square.hasTile()) {
                break;
            }

            word.append(square.getTile().getLetter());

            if (direction == Move.Direction.HORIZONTAL) {
                currentCol++;
            } else {
                currentRow++;
            }
        }

        return word.toString();
    }

    /**
     * Calculates the score for a word placement.
     *
     * @param board The game board
     * @param startRow The starting row
     * @param startCol The starting column
     * @param word The word string
     * @param direction The direction of placement
     * @param newTilePositions The positions of new tiles
     * @param crossWords The cross-words formed
     * @return The total score
     */
    private int calculateScore(Board board, int startRow, int startCol, String word,
                               Move.Direction direction, Set<Point> newTilePositions,
                               List<String> crossWords) {
        int totalScore = 0;

        int wordScore = 0;
        int wordMultiplier = 1;

        int currentRow = startRow;
        int currentCol = startCol;

        for (int i = 0; i < word.length(); i++) {
            Square square = board.getSquare(currentRow, currentCol);
            Point currentPoint = new Point(currentRow, currentCol);

            Tile tile = square.getTile();
            // Set letterValue to 0 for blank tiles regardless of the letter
            int letterValue = tile.isBlank() ? 0 : tile.getValue();
            int letterScore = letterValue;

            if (newTilePositions.contains(currentPoint)) {
                if (square.getSquareType() == Square.SquareType.DOUBLE_LETTER) {
                    letterScore = letterValue * 2;
                } else if (square.getSquareType() == Square.SquareType.TRIPLE_LETTER) {
                    letterScore = letterValue * 3;
                }

                if (square.getSquareType() == Square.SquareType.DOUBLE_WORD ||
                        square.getSquareType() == Square.SquareType.CENTER) {
                    wordMultiplier *= 2;
                } else if (square.getSquareType() == Square.SquareType.TRIPLE_WORD) {
                    wordMultiplier *= 3;
                }
            }

            wordScore += letterScore;

            if (direction == Move.Direction.HORIZONTAL) {
                currentCol++;
            } else {
                currentRow++;
            }
        }

        totalScore += wordScore * wordMultiplier;

        // Add scores for crossing words
        for (String crossWord : crossWords) {
            int crossWordScore = calculateCrossWordScore(board, crossWord);
            totalScore += crossWordScore;
        }

        if (newTilePositions.size() == 7) {
            totalScore += GameConstants.BINGO_BONUS; // 50 points bingo bonus
        }

        return totalScore;
    }

    /**
     * Calculates the score for a cross-word.
     *
     * @param board The game board
     * @param crossWord The cross-word string
     * @return The score for the cross-word
     */
    private int calculateCrossWordScore(Board board, String crossWord) {
        // Find the word on the board
        Point wordPosition = findWordPosition(board, crossWord);
        if (wordPosition == null) return 0;

        int row = wordPosition.x;
        int col = wordPosition.y;
        boolean isHorizontal = wordOrientation(board, row, col, crossWord);

        int score = 0;
        int wordMultiplier = 1;

        // Calculate the score for the cross word
        for (int i = 0; i < crossWord.length(); i++) {
            Square square = board.getSquare(row, col);
            Tile tile = square.getTile();

            // Ensure blank tiles have a value of 0
            int letterValue = tile.isBlank() ? 0 : tile.getValue();
            int letterScore = letterValue;

            if (!square.isPremiumUsed()) {
                if (square.getSquareType() == Square.SquareType.DOUBLE_LETTER) {
                    letterScore = letterValue * 2;
                } else if (square.getSquareType() == Square.SquareType.TRIPLE_LETTER) {
                    letterScore = letterValue * 3;
                }

                if (square.getSquareType() == Square.SquareType.DOUBLE_WORD ||
                        square.getSquareType() == Square.SquareType.CENTER) {
                    wordMultiplier *= 2;
                } else if (square.getSquareType() == Square.SquareType.TRIPLE_WORD) {
                    wordMultiplier *= 3;
                }
            }

            score += letterScore;

            if (isHorizontal) {
                col++;
            } else {
                row++;
            }
        }

        return score * wordMultiplier;
    }

    /**
     * Finds the position of a word on the board.
     *
     * @param board The game board
     * @param word The word to find
     * @return The position point, or null if not found
     */
    private Point findWordPosition(Board board, String word) {
        // Check horizontal words
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (getWordAt(board, r, c, Move.Direction.HORIZONTAL).equals(word)) {
                    return new Point(r, c);
                }
            }
        }

        // Check vertical words
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (getWordAt(board, r, c, Move.Direction.VERTICAL).equals(word)) {
                    return new Point(r, c);
                }
            }
        }

        return null;
    }

    /**
     * Determines the orientation of a word on the board.
     *
     * @param board The game board
     * @param row The starting row
     * @param col The starting column
     * @param word The word to check
     * @return true if horizontal, false if vertical
     */
    private boolean wordOrientation(Board board, int row, int col, String word) {
        return getWordAt(board, row, col, Move.Direction.HORIZONTAL).equals(word);
    }

    /**
     * Gets the unique letters in a string.
     *
     * @param letters The string of letters
     * @return A set of unique letters
     */
    private Set<Character> getUniqueLetters(String letters) {
        Set<Character> unique = new HashSet<>();
        for (char c : letters.toCharArray()) {
            unique.add(c);
        }
        return unique;
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
}