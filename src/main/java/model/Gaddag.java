package model;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Enhanced GADDAG data structure for efficient word lookup and validation in Scrabble.
 */
public class Gaddag {
    private static final char DELIMITER = '+';
    private final Node root;

    // Initialization
    public Gaddag() {
        this.root = new Node();
    }

    // Word insertion methods
    public void insert(String word) {
        word = word.toUpperCase();

        if (word.length() < 2) {
            return;
        }

        for (int i = 0; i < word.length(); i++) {
            StringBuilder sequence = new StringBuilder();

            for (int j = i; j > 0; j--) {
                sequence.append(word.charAt(j - 1));
            }

            sequence.append(DELIMITER);
            sequence.append(word.substring(i));
            insertSequence(sequence.toString());
        }

        insertSequence(DELIMITER + word);
    }

    private void insertSequence(String sequence) {
        Node current = root;

        for (int i = 0; i < sequence.length(); i++) {
            char c = sequence.charAt(i);
            current = current.getOrCreateChild(c);
        }

        current.setWord(true);
    }

    // Word validation methods
    public boolean contains(String word) {
        word = word.toUpperCase();

        if (word.isEmpty()) {
            return false;
        }

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

    // Word placement validation
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

        List<Point> newTilePositions = new ArrayList<>();
        int row = move.getStartRow();
        int col = move.getStartCol();
        Move.Direction direction = move.getDirection();

        int currentRow = row;
        int currentCol = col;

        for (Tile tile : move.getTiles()) {
            while (currentRow < Board.SIZE && currentCol < Board.SIZE &&
                    tempBoard.getSquare(currentRow, currentCol).hasTile()) {
                if (direction == Move.Direction.HORIZONTAL) {
                    currentCol++;
                } else {
                    currentRow++;
                }
            }

            if (currentRow >= Board.SIZE || currentCol >= Board.SIZE) {
                return false;
            }

            tempBoard.placeTile(currentRow, currentCol, tile);
            newTilePositions.add(new Point(currentRow, currentCol));

            if (direction == Move.Direction.HORIZONTAL) {
                currentCol++;
            } else {
                currentRow++;
            }
        }

        if (board.isEmpty()) {
            boolean touchesCenter = false;
            for (Point p : newTilePositions) {
                if (p.x == 7 && p.y == 7) {
                    touchesCenter = true;
                    break;
                }
            }
            if (!touchesCenter) {
                return false;
            }
        }

        List<String> formedWords = validateWords(tempBoard, move, newTilePositions);
        return !formedWords.isEmpty();
    }

    public List<String> validateWords(Board board, Move move, List<Point> newTilePositions) {
        List<String> formedWords = new ArrayList<>();

        String mainWord = findMainWord(board, move);

        if (mainWord.length() < 2 || !contains(mainWord)) {
            return formedWords;
        }

        formedWords.add(mainWord);

        for (Point p : newTilePositions) {
            String crossWord = findCrossWord(board, move.getDirection(), p);

            if (crossWord.length() >= 2) {
                if (!contains(crossWord)) {
                    return new ArrayList<>();
                }
                formedWords.add(crossWord);
            }
        }

        return formedWords;
    }

    // Word finding methods
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

    private String findCrossWord(Board board, Move.Direction direction, Point position) {
        if (direction == Move.Direction.HORIZONTAL) {
            int startRow = findWordStart(board, position.x, position.y, false);
            return getWordAt(board, startRow, position.y, Move.Direction.VERTICAL);
        } else {
            int startCol = findWordStart(board, position.x, position.y, true);
            return getWordAt(board, position.x, startCol, Move.Direction.HORIZONTAL);
        }
    }

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

    // Word generation from rack
    public Set<String> getWordsFrom(String rack, char anchor, boolean allowLeft, boolean allowRight) {
        Set<String> words = new HashSet<>();
        StringBuilder currentWord = new StringBuilder();
        currentWord.append(anchor);

        Map<Character, Integer> rackMap = new HashMap<>();
        for (char c : rack.toUpperCase().toCharArray()) {
            rackMap.put(c, rackMap.getOrDefault(c, 0) + 1);
        }

        Node current = root.getChild(anchor);
        if (current == null) {
            return words;
        }

        dfs(current, currentWord, rackMap, words, allowLeft, allowRight, false);
        return words;
    }

    private void dfs(Node node, StringBuilder wordBuilder, Map<Character, Integer> rack,
                     Set<String> validWords, boolean allowLeft, boolean allowRight, boolean passedDelimiter) {

        if (node.isWord() && passedDelimiter) {
            validWords.add(wordBuilder.toString());
        }

        for (Map.Entry<Character, Node> entry : node.getChildren().entrySet()) {
            char c = entry.getKey();
            Node child = entry.getValue();

            if (c == DELIMITER) {
                if (allowLeft) {
                    dfs(child, wordBuilder, rack, validWords, allowLeft, allowRight, true);
                }
            } else if (!passedDelimiter && allowLeft) {
                if (rack.getOrDefault(c, 0) > 0) {
                    rack.put(c, rack.get(c) - 1);
                    wordBuilder.insert(0, c);

                    dfs(child, wordBuilder, rack, validWords, allowLeft, allowRight, passedDelimiter);

                    wordBuilder.deleteCharAt(0);
                    rack.put(c, rack.get(c) + 1);
                }
            } else if (passedDelimiter && allowRight) {
                if (rack.getOrDefault(c, 0) > 0) {
                    rack.put(c, rack.get(c) - 1);
                    wordBuilder.append(c);

                    dfs(child, wordBuilder, rack, validWords, allowLeft, allowRight, passedDelimiter);

                    wordBuilder.deleteCharAt(wordBuilder.length() - 1);
                    rack.put(c, rack.get(c) + 1);
                }
            }
        }
    }

    // Node inner class
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