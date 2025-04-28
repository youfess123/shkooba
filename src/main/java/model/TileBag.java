package model;



import java.util.*;
import java.util.logging.Logger;

/**
 * Represents the bag of tiles in Scrabble.
 * Contains all tiles that haven't been distributed to players or placed on the board.
 */
public class TileBag {
    private static final Logger logger = Logger.getLogger(TileBag.class.getName());
    private final List<Tile> tiles;
    private final Random random;

    /**
     * Information about a letter in Scrabble, including its count and point value.
     */
    public static class LetterInfo {
        private final int count;
        private final int value;

        /**
         * Creates new letter information.
         *
         * @param count The number of tiles for this letter
         * @param value The point value of this letter
         */
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

    /**
     * Standard English Scrabble letter distribution and point values.
     */
    private static final Map<Character, LetterInfo> LETTER_DATA = initializeLetterData();

    /**
     * Initializes the standard letter distribution data.
     */
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

    /**
     * Creates a new tile bag with the standard distribution of tiles.
     */
    public TileBag() {
        this.tiles = new ArrayList<>();
        this.random = new Random();
        initializeTiles();
        shuffle();
    }

    /**
     * Initializes the bag with the standard set of tiles.
     */
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

    /**
     * Randomly shuffles the tiles in the bag.
     */
    public void shuffle() {
        Collections.shuffle(tiles, random);
        logger.fine("Shuffled tile bag");
    }

    /**
     * Returns tiles to the bag and shuffles.
     *
     * @param tilesToReturn The tiles to return to the bag
     */
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

    /**
     * Draws the specified number of tiles from the bag.
     *
     * @param count The number of tiles to draw
     * @return A list of drawn tiles, may be fewer than requested if the bag is depleted
     */
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

    /**
     * Draws a single tile from the bag.
     *
     * @return The drawn tile, or null if the bag is empty
     */
    public Tile drawTile() {
        if (tiles.isEmpty()) {
            logger.warning("Attempted to draw from empty tile bag");
            return null;
        }
        return tiles.remove(tiles.size() - 1);
    }

    /**
     * Gets the number of tiles remaining in the bag.
     *
     * @return The number of tiles
     */
    public int getTileCount() {
        return tiles.size();
    }

    /**
     * Checks if the bag is empty.
     *
     * @return true if the bag has no tiles, false otherwise
     */
    public boolean isEmpty() {
        return tiles.isEmpty();
    }

}