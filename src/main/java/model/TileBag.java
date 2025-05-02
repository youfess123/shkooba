package model;

import java.util.*;
import java.util.logging.Logger;

public class TileBag {
    private static final Logger logger = Logger.getLogger(TileBag.class.getName());
    private final List<Tile> tiles;
    private final Random random;
    private static final Map<Character, LetterInfo> LETTER_DATA = initializeLetterData();

    // Initialization methods
    public TileBag() {
        this.tiles = new ArrayList<>();
        this.random = new Random();
        initializeTiles();
        shuffle();
    }

    private void initializeTiles() {
        for (Map.Entry<Character, LetterInfo> entry : LETTER_DATA.entrySet()) {
            char letter = entry.getKey();
            LetterInfo info = entry.getValue();

            for (int i = 0; i < info.getCount(); i++) {
                tiles.add(new Tile(letter, info.getValue()));
            }
        }

        logger.info("Initialized tile bag with " + tiles.size() + " tiles");
    }

    private static Map<Character, LetterInfo> initializeLetterData() {
        Map<Character, LetterInfo> data = new HashMap<>();

        // One-point letters
        data.put('E', new LetterInfo(12, 1));
        data.put('A', new LetterInfo(9, 1));
        data.put('I', new LetterInfo(9, 1));
        data.put('O', new LetterInfo(8, 1));
        data.put('N', new LetterInfo(6, 1));
        data.put('R', new LetterInfo(6, 1));
        data.put('T', new LetterInfo(6, 1));
        data.put('L', new LetterInfo(4, 1));
        data.put('S', new LetterInfo(4, 1));
        data.put('U', new LetterInfo(4, 1));

        // Two-point letters
        data.put('D', new LetterInfo(4, 2));
        data.put('G', new LetterInfo(3, 2));

        // Three-point letters
        data.put('B', new LetterInfo(2, 3));
        data.put('C', new LetterInfo(2, 3));
        data.put('M', new LetterInfo(2, 3));
        data.put('P', new LetterInfo(2, 3));

        // Four-point letters
        data.put('F', new LetterInfo(2, 4));
        data.put('H', new LetterInfo(2, 4));
        data.put('V', new LetterInfo(2, 4));
        data.put('W', new LetterInfo(2, 4));
        data.put('Y', new LetterInfo(2, 4));

        // Five-point letter
        data.put('K', new LetterInfo(1, 5));

        // Eight-point letters
        data.put('J', new LetterInfo(1, 8));
        data.put('X', new LetterInfo(1, 8));

        // Ten-point letters
        data.put('Q', new LetterInfo(1, 10));
        data.put('Z', new LetterInfo(1, 10));

        // Blank tiles
        data.put('*', new LetterInfo(2, 0));

        return Collections.unmodifiableMap(data);
    }

    // Tile management methods
    public void shuffle() {
        Collections.shuffle(tiles, random);
        logger.fine("Shuffled tile bag");
    }

    public void returnTiles(List<Tile> tilesToReturn) {
        if (tilesToReturn == null || tilesToReturn.isEmpty()) {
            logger.warning("No tiles to return to bag");
            return;
        }

        int beforeCount = tiles.size();
        this.tiles.addAll(tilesToReturn);
        int afterCount = tiles.size();
        logger.fine("Added " + (afterCount - beforeCount) + " tiles to bag");
        shuffle();
    }

    public List<Tile> drawTiles(int count) {
        List<Tile> drawnTiles = new ArrayList<>();

        if (count <= 0) {
            logger.warning("Invalid request to draw " + count + " tiles");
            return drawnTiles;
        }

        logger.fine("Attempting to draw " + count + " tiles. Available: " + tiles.size());

        int tilesToDraw = Math.min(count, tiles.size());
        for (int i = 0; i < tilesToDraw; i++) {
            Tile tile = drawTile();
            if (tile != null) {
                drawnTiles.add(tile);
            }
        }

        logger.fine("Drew " + drawnTiles.size() + " tiles. Remaining: " + tiles.size());
        return drawnTiles;
    }

    public Tile drawTile() {
        if (tiles.isEmpty()) {
            logger.warning("Attempted to draw from empty tile bag");
            return null;
        }
        return tiles.remove(tiles.size() - 1);
    }

    // Accessors
    public int getTileCount() {
        return tiles.size();
    }

    public boolean isEmpty() {
        return tiles.isEmpty();
    }

    // Inner class for letter information
    public static class LetterInfo {
        private final int count;
        private final int value;

        public LetterInfo(int count, int value) {
            this.count = count;
            this.value = value;
        }

        public int getCount() {
            return count;
        }

        public int getValue() {
            return value;
        }
    }
}