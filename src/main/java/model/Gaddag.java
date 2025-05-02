package model;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Enhanced GADDAG data structure for efficient word lookup and validation in Scrabble.
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
     * Validates a word placement on the board.
     *
     * @param board The game board
     * @param move The move to validate
     * @return true if valid, false otherwise
     */
    public boolean validateWordPlacement(Board board, Move move) {
        if (move.getTiles().isEmpty()) {
            return false;
        }

        Board tempBoard = new Board();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Square square = board.getSquare(r, c);
                if (square.hasTile()) {
                    tempBoard.placeTile(r, c, square.getTile());
                }
            }
        }

        // Place tiles on temporary board
        List<Point> newTilePositions = new ArrayList<>();
        int row = move.getStartRow();
        int col = move.getStartCol();
        Move.Direction direction = move.getDirection();

        int currentRow = row;
        int currentCol = col;

        for (Tile tile : move.getTiles()) {
            // Skip over existing tiles
            while (currentRow < Board.SIZE && currentCol < Board.SIZE &&
                    tempBoard.getSquare(currentRow, currentCol).hasTile()) {
                if (direction == Move.Direction.HORIZONTAL) {
                    currentCol++;
                } else {
                    currentRow++;
                }
            }

            if (currentRow >= Board.SIZE || currentCol >= Board.SIZE) {
                return false; // Ran off the board
            }

            tempBoard.placeTile(currentRow, currentCol, tile);
            newTilePositions.add(new Point(currentRow, currentCol));

            if (direction == Move.Direction.HORIZONTAL) {
                currentCol++;
            } else {
                currentRow++;
            }
        }

        // First move must cover center square
        if (board.isEmpty()) {
            boolean touchesCenter = false;
            for (Point p : newTilePositions) {
                if (p.x == 7 && p.y == 7) { // Center is at (7,7)
                    touchesCenter = true;
                    break;
                }
            }
            if (!touchesCenter) {
                return false;
            }
        }
        List<String> formedWords = validateWords(tempBoard, move, newTilePositions);

        if (formedWords.isEmpty()) {
            return false;
        }
        return true;
    }

    public List<String> validateWords(Board board, Move move, List<Point> newTilePositions) {
        List<String> formedWords = new ArrayList<>();

        String mainWord = findMainWord(board, move);

        if (mainWord.length() < 2 || !contains(mainWord)) {
            return formedWords; // Empty list indicates invalid placement
        }

        formedWords.add(mainWord);

        // Check all crossing words
        for (Point p : newTilePositions) {
            String crossWord = findCrossWord(board, move.getDirection(), p);

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
     * Finds the main word formed by a move.
     *
     * @param board The board
     * @param move The move
     * @return The main word string
     */
    private String findMainWord(Board board, Move move) {
        int row = move.getStartRow();
        int col = move.getStartCol();
        Move.Direction direction = move.getDirection();

        if (direction == Move.Direction.HORIZONTAL) {
            int startCol = findWordStart(board, row, col, true);
            return getWordAt(board, row, startCol, Move.Direction.HORIZONTAL);
        } else {
            int startRow = findWordStart(board, row, col, false);
            return getWordAt(board, startRow, col, Move.Direction.VERTICAL);
        }
    }

    /**
     * Finds a crossing word at a position.
     *
     * @param board The board
     * @param direction The direction of the main word
     * @param position The position to check
     * @return The crossing word string
     */
    private String findCrossWord(Board board, Move.Direction direction, Point position) {
        if (direction == Move.Direction.HORIZONTAL) {
            int startRow = findWordStart(board, position.x, position.y, false);
            return getWordAt(board, startRow, position.y, Move.Direction.VERTICAL);
        } else {
            int startCol = findWordStart(board, position.x, position.y, true);
            return getWordAt(board, position.x, startCol, Move.Direction.HORIZONTAL);
        }
    }

    /**
     * Finds the starting position of a word.
     *
     * @param board The board
     * @param row The row
     * @param col The column
     * @param isHorizontal Whether searching horizontally
     * @return The starting position
     */
    private int findWordStart(Board board, int row, int col, boolean isHorizontal) {
        int position = isHorizontal ? col : row;
        while (position > 0) {
            int prevPos = position - 1;
            Square square = isHorizontal ?
                    board.getSquare(row, prevPos) :
                    board.getSquare(prevPos, col);
            if (!square.hasTile()) {
                break;
            }
            position = prevPos;
        }
        return position;
    }

    /**
     * Gets a word starting at a position.
     *
     * @param board The board
     * @param row The starting row
     * @param col The starting column
     * @param direction The direction
     * @return The word string
     */
    private String getWordAt(Board board, int row, int col, Move.Direction direction) {
        StringBuilder word = new StringBuilder();
        int currentRow = row;
        int currentCol = col;

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