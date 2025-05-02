package utilities;

import model.*;
import model.Dictionary;
import java.awt.Point;
import java.util.*;
import java.util.logging.Logger;

public class WordFinder {
    private static final Logger logger = Logger.getLogger(WordFinder.class.getName());
    private final Dictionary dictionary;
    private final Board board;

    // Core constructor
    public WordFinder(Dictionary dictionary, Board board) {
        this.dictionary = dictionary;
        this.board = board;
    }

    // Main placement finding methods
    public List<WordPlacement> findAllPlacements(Rack rack) {
        List<WordPlacement> placements = new ArrayList<>();
        String rackLetters = getAvailableLetters(rack);
        List<Tile> rackTiles = new ArrayList<>(rack.getTiles());

        if (board.isEmpty()) {
            findPlacementsForFirstMove(rackLetters, rackTiles, placements);
            return placements;
        }

        List<Point> anchorPoints = findAnchorPoints();
        logger.info("Found " + anchorPoints.size() + " anchor points");

        for (Point anchor : anchorPoints) {
            int row = anchor.x;
            int col = anchor.y;

            String[] hContext = getWordContext(row, col, Move.Direction.HORIZONTAL);
            findPlacements(row, col, Move.Direction.HORIZONTAL,
                    hContext[0], hContext[1], rackLetters, rackTiles, placements);

            String[] vContext = getWordContext(row, col, Move.Direction.VERTICAL);
            findPlacements(row, col, Move.Direction.VERTICAL,
                    vContext[0], vContext[1], rackLetters, rackTiles, placements);
        }

        placements.sort(Comparator.comparing(WordPlacement::getScore).reversed());
        return placements;
    }

    private void findPlacementsForFirstMove(String rackLetters, List<Tile> rackTiles,
                                            List<WordPlacement> placements) {
        int center = GameConstants.CENTER_SQUARE;
        Gaddag gaddag = dictionary.getGaddag();

        for (char letter : getUniqueLetters(rackLetters)) {
            Set<String> words = gaddag.getWordsFrom(rackLetters, letter, true, true);

            for (String word : words) {
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

    private void findPlacements(int row, int col, Move.Direction direction,
                                String prefix, String suffix,
                                String rackLetters, List<Tile> rackTiles,
                                List<WordPlacement> placements) {

        logger.fine("Finding placements at " + row + "," + col + " " + direction +
                " with prefix: '" + prefix + "', suffix: '" + suffix + "'");

        StringBuilder allLettersBuilder = new StringBuilder(rackLetters);
        allLettersBuilder.append(prefix).append(suffix);
        String allLetters = allLettersBuilder.toString();

        for (char letter : getUniqueLetters(rackLetters)) {
            Set<String> words = dictionary.getGaddag().getWordsFrom(
                    allLetters, letter, true, true);

            for (String word : words) {
                if (word.length() < 2) continue;

                for (int pos = 0; pos < word.length(); pos++) {
                    if (word.charAt(pos) != letter) continue;

                    int startRow = direction == Move.Direction.HORIZONTAL ? row : row - pos;
                    int startCol = direction == Move.Direction.HORIZONTAL ? col - pos : col;

                    if (startRow < 0 || startCol < 0 ||
                            (direction == Move.Direction.HORIZONTAL && startCol + word.length() > Board.SIZE) ||
                            (direction == Move.Direction.VERTICAL && startRow + word.length() > Board.SIZE)) {
                        continue;
                    }

                    if (!isPlacementCompatible(word, direction, startRow, startCol)) {
                        continue;
                    }

                    Board tempBoard = board.copy();
                    List<Point> newPositions = new ArrayList<>();
                    List<Tile> tilesNeeded = placeTilesOnTempBoard(
                            word, direction, startRow, startCol, rackTiles, tempBoard, newPositions);

                    if (tilesNeeded == null || tilesNeeded.isEmpty()) {
                        continue;
                    }

                    Move testMove = createTestMove(direction, startRow, startCol);
                    List<String> formedWords = WordValidator.validateWords(
                            tempBoard, testMove, newPositions, dictionary);

                    if (formedWords.isEmpty()) {
                        continue;
                    }

                    Set<Point> newPositionsSet = new HashSet<>(newPositions);
                    int score = ScoreCalculator.calculateMoveScore(
                            testMove, tempBoard, formedWords, newPositionsSet);

                    placements.add(new WordPlacement(
                            word, startRow, startCol, direction, tilesNeeded, score, formedWords));
                }
            }
        }
    }

    // Board context methods
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

    private String[] getWordContext(int row, int col, Move.Direction direction) {
        String prefix = "";
        String suffix = "";

        if (direction == Move.Direction.HORIZONTAL) {
            StringBuilder prefixBuilder = new StringBuilder();
            int c = col - 1;
            while (c >= 0 && board.getSquare(row, c).hasTile()) {
                prefixBuilder.insert(0, board.getSquare(row, c).getTile().getLetter());
                c--;
            }
            prefix = prefixBuilder.toString();

            StringBuilder suffixBuilder = new StringBuilder();
            c = col + 1;
            while (c < Board.SIZE && board.getSquare(row, c).hasTile()) {
                suffixBuilder.append(board.getSquare(row, c).getTile().getLetter());
                c++;
            }
            suffix = suffixBuilder.toString();
        } else {
            StringBuilder prefixBuilder = new StringBuilder();
            int r = row - 1;
            while (r >= 0 && board.getSquare(r, col).hasTile()) {
                prefixBuilder.insert(0, board.getSquare(r, col).getTile().getLetter());
                r--;
            }
            prefix = prefixBuilder.toString();

            StringBuilder suffixBuilder = new StringBuilder();
            r = row + 1;
            while (r < Board.SIZE && board.getSquare(r, col).hasTile()) {
                suffixBuilder.append(board.getSquare(r, col).getTile().getLetter());
                r++;
            }
            suffix = suffixBuilder.toString();
        }

        return new String[] { prefix, suffix };
    }

    private String[] getPartialWordsAt(int row, int col, Move.Direction direction) {
        StringBuilder prefix = new StringBuilder();
        StringBuilder suffix = new StringBuilder();

        if (direction == Move.Direction.HORIZONTAL) {
            int c = col - 1;
            while (c >= 0 && board.getSquare(row, c).hasTile()) {
                prefix.insert(0, board.getSquare(row, c).getTile().getLetter());
                c--;
            }

            c = col + 1;
            while (c < Board.SIZE && board.getSquare(row, c).hasTile()) {
                suffix.append(board.getSquare(row, c).getTile().getLetter());
                c++;
            }
        } else {
            int r = row - 1;
            while (r >= 0 && board.getSquare(r, col).hasTile()) {
                prefix.insert(0, board.getSquare(r, col).getTile().getLetter());
                r--;
            }

            r = row + 1;
            while (r < Board.SIZE && board.getSquare(r, col).hasTile()) {
                suffix.append(board.getSquare(r, col).getTile().getLetter());
                r++;
            }
        }

        return new String[] {prefix.toString(), suffix.toString()};
    }

    private boolean hasAdjacentTile(int row, int col) {
        if (row > 0 && board.getSquare(row - 1, col).hasTile()) {
            return true;
        }
        if (row < Board.SIZE - 1 && board.getSquare(row + 1, col).hasTile()) {
            return true;
        }
        if (col > 0 && board.getSquare(row, col - 1).hasTile()) {
            return true;
        }
        if (col < Board.SIZE - 1 && board.getSquare(row, col + 1).hasTile()) {
            return true;
        }

        return false;
    }

    // Word validation methods
    private boolean isPlacementCompatible(String word, Move.Direction direction,
                                          int startRow, int startCol) {
        int row = startRow;
        int col = startCol;

        for (int i = 0; i < word.length(); i++) {
            if (row >= Board.SIZE || col >= Board.SIZE) {
                return false;
            }

            if (board.getSquare(row, col).hasTile()) {
                char boardLetter = board.getSquare(row, col).getTile().getLetter();
                if (boardLetter != word.charAt(i)) {
                    return false;
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

    private boolean isWordCompatible(String word, Move.Direction direction,
                                     int startRow, int startCol,
                                     String prefix, String suffix) {
        int anchorRow = direction == Move.Direction.HORIZONTAL ? startRow : startRow + prefix.length();
        int anchorCol = direction == Move.Direction.HORIZONTAL ? startCol + prefix.length() : startCol;

        if (!prefix.isEmpty()) {
            if (word.length() <= prefix.length() ||
                    !word.substring(0, prefix.length()).equals(prefix)) {
                return false;
            }
        }

        if (!suffix.isEmpty()) {
            int suffixStart = direction == Move.Direction.HORIZONTAL ?
                    anchorCol - startCol + 1 : anchorRow - startRow + 1;

            if (word.length() < suffixStart + suffix.length() ||
                    !word.substring(suffixStart).equals(suffix)) {
                return false;
            }
        }

        int row = startRow;
        int col = startCol;

        for (int i = 0; i < word.length(); i++) {
            if (row >= Board.SIZE || col >= Board.SIZE) return false;

            if (board.getSquare(row, col).hasTile()) {
                if (board.getSquare(row, col).getTile().getLetter() != word.charAt(i)) {
                    return false;
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

    // Tile placement and scoring methods
    private Move createTestMove(Move.Direction direction, int startRow, int startCol) {
        return Move.createPlaceMove(new Player("temp"), startRow, startCol, direction);
    }

    private List<Tile> placeTilesOnTempBoard(String word, Move.Direction direction,
                                             int startRow, int startCol, List<Tile> rackTiles,
                                             Board tempBoard, List<Point> newPositions) {
        List<Tile> tilesNeeded = new ArrayList<>();
        List<Tile> availableTiles = new ArrayList<>(rackTiles);

        int row = startRow;
        int col = startCol;

        for (int i = 0; i < word.length(); i++) {
            char letter = word.charAt(i);

            if (row >= Board.SIZE || col >= Board.SIZE) {
                return null;
            }

            if (tempBoard.getSquare(row, col).hasTile()) {
                Tile existingTile = tempBoard.getSquare(row, col).getTile();
                if (existingTile.getLetter() != letter) {
                    return null;
                }
            } else {
                Tile tileToUse = findTileForLetter(letter, availableTiles);
                if (tileToUse == null) {
                    return null;
                }

                availableTiles.remove(tileToUse);

                if (tileToUse.isBlank() && tileToUse.getLetter() == '*') {
                    Tile blankWithLetter = Tile.createBlankTile(letter);
                    tilesNeeded.add(blankWithLetter);
                    tempBoard.placeTile(row, col, blankWithLetter);
                } else {
                    tilesNeeded.add(tileToUse);
                    tempBoard.placeTile(row, col, tileToUse);
                }

                newPositions.add(new Point(row, col));
            }

            if (direction == Move.Direction.HORIZONTAL) {
                col++;
            } else {
                row++;
            }
        }

        return tilesNeeded;
    }

    private int calculateFirstMoveScore(String word, List<Tile> tiles) {
        int score = 0;
        for (Tile tile : tiles) {
            score += tile.getValue();
        }

        score *= 2;  // Double for center square

        if (tiles.size() == 7) {
            score += GameConstants.BINGO_BONUS;
        }

        return score;
    }

    // Tile and letter handling methods
    private Tile findTileForLetter(char letter, List<Tile> availableTiles) {
        for (Tile tile : availableTiles) {
            if (tile.getLetter() == letter) {
                return tile;
            }
        }

        for (Tile tile : availableTiles) {
            if (tile.isBlank() && tile.getLetter() == '*') {
                return tile;
            }
        }

        return null;
    }

    private List<Tile> getTilesForWord(String word, List<Tile> availableTiles) {
        Map<Character, List<Tile>> tilesByLetter = new HashMap<>();
        for (Tile tile : availableTiles) {
            char letter = tile.getLetter();
            tilesByLetter.computeIfAbsent(letter, k -> new ArrayList<>()).add(tile);
        }

        List<Tile> result = new ArrayList<>();

        for (char c : word.toCharArray()) {
            if (tilesByLetter.containsKey(c) && !tilesByLetter.get(c).isEmpty()) {
                Tile tile = tilesByLetter.get(c).remove(0);
                result.add(tile);
            } else if (tilesByLetter.containsKey('*') && !tilesByLetter.get('*').isEmpty()) {
                tilesByLetter.get('*').remove(0);
                result.add(Tile.createBlankTile(c));
            } else {
                return null;
            }
        }

        return result;
    }

    private String getAvailableLetters(Rack rack) {
        StringBuilder sb = new StringBuilder();
        for (Tile tile : rack.getTiles()) {
            sb.append(tile.getLetter());
        }
        return sb.toString();
    }

    private Set<Character> getUniqueLetters(String letters) {
        Set<Character> uniqueLetters = new HashSet<>();
        for (char c : letters.toCharArray()) {
            uniqueLetters.add(c);
        }
        return uniqueLetters;
    }

    // WordPlacement inner class
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
}