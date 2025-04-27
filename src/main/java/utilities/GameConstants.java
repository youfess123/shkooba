package utilities;

/**
 * Constants used throughout the Scrabble game.
 * Centralizing constants here makes it easier to modify game parameters.
 */
public final class GameConstants {

    /**
     * The size of the Scrabble board (number of squares in each direction).
     * Standard Scrabble uses a 15x15 board.
     */
    public static final int BOARD_SIZE = 15;

    /**
     * The index of the center square on the board (0-based).
     * For a 15x15 board, this is 7.
     */
    public static final int CENTER_SQUARE = BOARD_SIZE / 2;

    /**
     * The number of tiles each player can have in their rack.
     * Standard Scrabble uses 7 tiles.
     */
    public static final int RACK_CAPACITY = 7;

    /**
     * The bonus points awarded for using all tiles in a single move ("bingo").
     * Standard Scrabble awards 50 points.
     */
    public static final int BINGO_BONUS = 50;

    /**
     * The minimum number of players required for a game.
     */
    public static final int MIN_PLAYERS = 2;

    /**
     * The maximum number of players allowed in a game.
     */
    public static final int MAX_PLAYERS = 4;

    /**
     * The number of consecutive passes that will end the game.
     * This occurs when all players pass in sequence once or twice.
     */
    public static final int CONSECUTIVE_PASSES_TO_END = 6;

    /**
     * The path to the default dictionary file.
     */
    public static final String DEFAULT_DICTIONARY = "src/main/resources/Dictionary.txt";

    public static final int AI_EASY = 1;


    public static final int AI_MEDIUM = 2;

    public static final int AI_HARD = 3;


    public static final int SQUARE_SIZE = 40;


    public static final int TILE_SIZE = 36;


    private void Constants() {
        throw new AssertionError("Constants should not be instantiated");
    }
}