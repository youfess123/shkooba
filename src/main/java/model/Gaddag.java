package model;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * GADDAG data structure for efficient word lookup in Scrabble.
 *
 * A GADDAG is a specialized directed acyclic word graph that allows for
 * efficient finding of all valid words that can be placed through a specific
 * position on the Scrabble board.
 */
public class Gaddag {
    // Delimiter used to mark the pivot point in word encodings
    private static final char DELIMITER = '+';

    // Root node of the GADDAG
    private final Node root;

    /**
     * Creates a new empty GADDAG.
     */
    public Gaddag() {
        this.root = new Node();
    }

    /**
     * Inserts a word into the GADDAG.
     *
     * For each position in the word, we create an entry starting from
     * that position, going backwards to the start, then the delimiter,
     * then forwards to the end.
     *
     * @param word The word to insert
     */
    public void insert(String word) {
        word = word.toUpperCase();

        if (word.length() < 2) {
            return; // Skip single letter words
        }

        // Insert all possible ways to build this word
        for (int i = 0; i < word.length(); i++) {
            StringBuilder sequence = new StringBuilder();

            // Add reversed prefix (letters before the current position, in reverse)
            for (int j = i; j > 0; j--) {
                sequence.append(word.charAt(j - 1));
            }

            // Add delimiter
            sequence.append(DELIMITER);

            // Add suffix (letters from current position to end)
            sequence.append(word.substring(i));

            // Insert this sequence
            insertSequence(sequence.toString());
        }

        // Also insert from start to end (for playing from scratch)
        insertSequence(DELIMITER + word);
    }

    /**
     * Inserts a sequence into the GADDAG.
     *
     * @param sequence The sequence to insert
     */
    private void insertSequence(String sequence) {
        Node current = root;

        for (int i = 0; i < sequence.length(); i++) {
            char c = sequence.charAt(i);
            current = current.getOrCreateChild(c);
        }

        current.setWord(true);
    }

    /**
     * Checks if a word exists in the dictionary.
     *
     * @param word The word to check
     * @return true if the word exists, false otherwise
     */
    public boolean contains(String word) {
        word = word.toUpperCase();

        if (word.isEmpty()) {
            return false;
        }

        // For lookup, we use the sequence starting with the delimiter
        Node current = root;
        String sequence = DELIMITER + word;

        for (int i = 0; i < sequence.length(); i++) {
            current = current.getChild(sequence.charAt(i));
            if (current == null) {
                return false;
            }
        }

        return current.isWord();
    }

    /**
     * Finds all valid words that can be formed using the given rack
     * with the given anchor letter at the anchor position.
     *
     * @param rack The letters available in the player's rack
     * @param anchor The letter at the anchor position
     * @param allowLeft Whether letters can be placed to the left of the anchor
     * @param allowRight Whether letters can be placed to the right of the anchor
     * @return A set of valid words
     */
    public Set<String> getWordsFrom(String rack, char anchor, boolean allowLeft, boolean allowRight) {
        Set<String> words = new HashSet<>();
        StringBuilder currentWord = new StringBuilder();
        currentWord.append(anchor);

        // Convert rack to a map of letter -> count
        Map<Character, Integer> rackMap = new HashMap<>();
        for (char c : rack.toUpperCase().toCharArray()) {
            rackMap.put(c, rackMap.getOrDefault(c, 0) + 1);
        }

        // Start from the anchor letter in the GADDAG
        Node current = root.getChild(anchor);
        if (current == null) {
            return words; // Anchor letter not in GADDAG
        }

        // Perform depth-first search to find all valid words
        dfs(current, currentWord, rackMap, words, allowLeft, allowRight, false);

        return words;
    }

    /**
     * Helper method to perform depth-first search in the GADDAG.
     *
     * @param node Current node in the GADDAG
     * @param wordBuilder Current word being built
     * @param rack Available letters in the rack
     * @param validWords Set to collect valid words
     * @param allowLeft Whether to allow exploring left of pivot
     * @param allowRight Whether to allow exploring right of pivot
     * @param passedDelimiter Whether we've passed the delimiter
     */
    private void dfs(Node node, StringBuilder wordBuilder, Map<Character, Integer> rack,
                     Set<String> validWords, boolean allowLeft, boolean allowRight, boolean passedDelimiter) {

        // If we have a complete word after passing the delimiter, add it
        if (node.isWord() && passedDelimiter) {
            validWords.add(wordBuilder.toString());
        }

        // Explore all child nodes
        for (Map.Entry<Character, Node> entry : node.getChildren().entrySet()) {
            char c = entry.getKey();
            Node child = entry.getValue();

            if (c == DELIMITER) {
                // We've hit the delimiter - move to right side exploration
                if (allowLeft) {
                    dfs(child, wordBuilder, rack, validWords, allowLeft, allowRight, true);
                }
            } else if (!passedDelimiter && allowLeft) {
                // We're exploring to the left of the pivot
                if (rack.getOrDefault(c, 0) > 0) {
                    // We have this letter in our rack
                    rack.put(c, rack.get(c) - 1); // Use the letter
                    wordBuilder.insert(0, c);     // Add to start of word

                    dfs(child, wordBuilder, rack, validWords, allowLeft, allowRight, passedDelimiter);

                    // Backtrack
                    wordBuilder.deleteCharAt(0);
                    rack.put(c, rack.get(c) + 1);
                }
            } else if (passedDelimiter && allowRight) {
                // We're exploring to the right of the pivot
                if (rack.getOrDefault(c, 0) > 0) {
                    // We have this letter in our rack
                    rack.put(c, rack.get(c) - 1);  // Use the letter
                    wordBuilder.append(c);         // Add to end of word

                    dfs(child, wordBuilder, rack, validWords, allowLeft, allowRight, passedDelimiter);

                    // Backtrack
                    wordBuilder.deleteCharAt(wordBuilder.length() - 1);
                    rack.put(c, rack.get(c) + 1);
                }
            }
        }
    }

    /**
     * Finds valid words that can be placed at a specific position on the board.
     *
     * @param board The game board
     * @param row The row coordinate
     * @param col The column coordinate
     * @param rack The player's rack
     * @param direction The direction to search (horizontal or vertical)
     * @return A map of valid words to their starting positions
     */
    public Map<String, Point> findValidWordsAt(Board board, int row, int col, String rack, Move.Direction direction) {
        Map<String, Point> validWords = new HashMap<>();

        // Skip if position already has a tile
        if (board.getSquare(row, col).hasTile()) {
            return validWords;
        }

        // Get letters already on the board around this position
        String[] partialWords = getPartialWordsAt(board, row, col, direction);
        String prefix = partialWords[0];
        String suffix = partialWords[1];

        // Skip if isolated and not the first move
        if (prefix.isEmpty() && suffix.isEmpty() && !board.isEmpty() &&
                !hasAdjacentTiles(board, row, col)) {
            return validWords;
        }

        // First move must go through center
        if (board.isEmpty() && (row != 7 || col != 7)) {
            return validWords;
        }

        // Try each letter in the rack
        for (char letter : getUniqueLetters(rack)) {
            String word = prefix + letter + suffix;

            if (word.length() >= 2 && contains(word)) {
                // Calculate the starting position of the word
                int startRow = direction == Move.Direction.HORIZONTAL ? row : row - prefix.length();
                int startCol = direction == Move.Direction.HORIZONTAL ? col - prefix.length() : col;

                validWords.put(word, new java.awt.Point(startRow, startCol));
            }
        }

        return validWords;
    }

    /**
     * Gets the partial words (prefix and suffix) at a specific board position.
     *
     * @param board The game board
     * @param row The row coordinate
     * @param col The column coordinate
     * @param direction The direction (horizontal or vertical)
     * @return An array with [prefix, suffix]
     */
    private String[] getPartialWordsAt(Board board, int row, int col, Move.Direction direction) {
        StringBuilder prefix = new StringBuilder();
        StringBuilder suffix = new StringBuilder();

        if (direction == Move.Direction.HORIZONTAL) {
            // Get letters to the left
            int c = col - 1;
            while (c >= 0 && board.getSquare(row, c).hasTile()) {
                prefix.insert(0, board.getSquare(row, c).getTile().getLetter());
                c--;
            }

            // Get letters to the right
            c = col + 1;
            while (c < Board.SIZE && board.getSquare(row, c).hasTile()) {
                suffix.append(board.getSquare(row, c).getTile().getLetter());
                c++;
            }
        } else { // VERTICAL
            // Get letters above
            int r = row - 1;
            while (r >= 0 && board.getSquare(r, col).hasTile()) {
                prefix.insert(0, board.getSquare(r, col).getTile().getLetter());
                r--;
            }

            // Get letters below
            r = row + 1;
            while (r < Board.SIZE && board.getSquare(r, col).hasTile()) {
                suffix.append(board.getSquare(r, col).getTile().getLetter());
                r++;
            }
        }

        return new String[] {prefix.toString(), suffix.toString()};
    }

    /**
     * Checks if a position has adjacent tiles.
     *
     * @param board The game board
     * @param row The row coordinate
     * @param col The column coordinate
     * @return true if has adjacent tiles, false otherwise
     */
    private boolean hasAdjacentTiles(Board board, int row, int col) {
        // Check all four adjacent positions
        if (row > 0 && board.getSquare(row - 1, col).hasTile()) return true;
        if (row < Board.SIZE - 1 && board.getSquare(row + 1, col).hasTile()) return true;
        if (col > 0 && board.getSquare(row, col - 1).hasTile()) return true;
        if (col < Board.SIZE - 1 && board.getSquare(row, col + 1).hasTile()) return true;

        return false;
    }

    /**
     * Gets the unique letters in a string.
     *
     * @param rack The string containing letters
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
     * Finds the starting position of a word.
     *
     * @param board The game board
     * @param row The row coordinate
     * @param col The column coordinate
     * @param isHorizontal Whether the word is horizontal
     * @return The starting position of the word
     */
    private int findWordStart(Board board, int row, int col, boolean isHorizontal) {
        int position = isHorizontal ? col : row;

        while (position > 0) {
            int prevPos = position - 1;
            Square square = isHorizontal ? board.getSquare(row, prevPos) : board.getSquare(prevPos, col);

            if (!square.hasTile()) {
                break;
            }
            position = prevPos;
        }

        return position;
    }

    /**
     * Validates a move by checking that all words formed are valid.
     *
     * @param board The game board
     * @param move The move to validate
     * @return A list of formed words, or empty if the move is invalid
     */
    public List<String> validateMove(Board board, Move move) {
        List<String> formedWords = new ArrayList<>();

        // Create a temporary board for validation
        Board tempBoard = new Board();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Square square = board.getSquare(r, c);
                if (square.hasTile()) {
                    tempBoard.placeTile(r, c, square.getTile());
                }
            }
        }

        int row = move.getStartRow();
        int col = move.getStartCol();
        Move.Direction direction = move.getDirection();

        // Place tiles on the temporary board
        List<java.awt.Point> newTilePositions = new ArrayList<>();
        int currentRow = row;
        int currentCol = col;

        for (Tile tile : move.getTiles()) {
            // Skip existing tiles
            while (currentRow < Board.SIZE && currentCol < Board.SIZE &&
                    tempBoard.getSquare(currentRow, currentCol).hasTile()) {
                if (direction == Move.Direction.HORIZONTAL) {
                    currentCol++;
                } else {
                    currentRow++;
                }
            }

            if (currentRow < Board.SIZE && currentCol < Board.SIZE) {
                tempBoard.placeTile(currentRow, currentCol, tile);
                newTilePositions.add(new java.awt.Point(currentRow, currentCol));

                // Move to next position
                if (direction == Move.Direction.HORIZONTAL) {
                    currentCol++;
                } else {
                    currentRow++;
                }
            }
        }

        // Find and validate the main word
        String mainWord;
        if (direction == Move.Direction.HORIZONTAL) {
            mainWord = getWordAt(tempBoard, row, findWordStart(tempBoard, row, col, true), true);
        } else {
            mainWord = getWordAt(tempBoard, findWordStart(tempBoard, row, col, false), col, false);
        }

        // Validate the main word
        if (mainWord.length() < 2 || !contains(mainWord)) {
            return formedWords; // Invalid main word
        }

        formedWords.add(mainWord);

        // Check all crossing words
        for (java.awt.Point p : newTilePositions) {
            String crossWord;
            if (direction == Move.Direction.HORIZONTAL) {
                crossWord = getWordAt(tempBoard, findWordStart(tempBoard, p.x, p.y, false), p.y, false);
            } else {
                crossWord = getWordAt(tempBoard, p.x, findWordStart(tempBoard, p.x, p.y, true), true);
            }

            if (crossWord.length() >= 2) {
                if (!contains(crossWord)) {
                    return new ArrayList<>(); // Invalid crossing word
                }
                formedWords.add(crossWord);
            }
        }

        return formedWords;
    }

    /**
     * Gets a word at a specific position in a specific direction.
     *
     * @param board The game board
     * @param row The starting row
     * @param col The starting column
     * @param isHorizontal Whether the word is horizontal
     * @return The word string
     */
    private String getWordAt(Board board, int row, int col, boolean isHorizontal) {
        StringBuilder word = new StringBuilder();

        int currentRow = row;
        int currentCol = col;

        while (currentRow < Board.SIZE && currentCol < Board.SIZE) {
            Square square = board.getSquare(currentRow, currentCol);

            if (!square.hasTile()) {
                break;
            }

            word.append(square.getTile().getLetter());

            if (isHorizontal) {
                currentCol++;
            } else {
                currentRow++;
            }
        }

        return word.toString();
    }

    /**
     * Node class representing a position in the GADDAG.
     */
    private static class Node {
        private final Map<Character, Node> children;
        private boolean isWord;

        public Node() {
            this.children = new HashMap<>();
            this.isWord = false;
        }

        public Map<Character, Node> getChildren() {
            return children;
        }

        public Node getChild(char c) {
            return children.get(c);
        }

        public Node getOrCreateChild(char c) {
            return children.computeIfAbsent(c, k -> new Node());
        }

        public boolean isWord() {
            return isWord;
        }

        public void setWord(boolean isWord) {
            this.isWord = isWord;
        }
    }
}