package model;


import utilities.GameConstants;
import utilities.ScoreCalculator;
import utilities.WordValidator;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

public class Game {
    private static final Logger logger = Logger.getLogger(Game.class.getName());
    private static final int EMPTY_RACK_BONUS = 50;

    private final Board board;
    private final TileBag tileBag;
    private final List<Player> players;
    private final Dictionary dictionary;
    private int currentPlayerIndex;
    private boolean gameOver;
    private int consecutivePasses;
    private final List<Move> moveHistory;

    public Game(InputStream dictionaryStream, String dictionaryName) throws IOException {
        this.board = new Board();
        this.tileBag = new TileBag();
        this.players = new ArrayList<>();
        this.dictionary = new Dictionary(dictionaryStream, dictionaryName);
        this.currentPlayerIndex = 0;
        this.gameOver = false;
        this.consecutivePasses = 0;
        this.moveHistory = new ArrayList<>();
    }

    public void addPlayer(Player player) {
        if (players.size() >= GameConstants.MAX_PLAYERS) {
            logger.warning("Cannot add more than " + GameConstants.MAX_PLAYERS + " players");
            return;
        }

        players.add(player);
        logger.info("Added player: " + player.getName());
    }

    public void start() {
        if (players.size() < GameConstants.MIN_PLAYERS) {
            throw new IllegalStateException("Cannot start game with fewer than " +
                    GameConstants.MIN_PLAYERS + " players");
        }

        tileBag.shuffle();

        for (Player player : players) {
            fillRack(player);
        }

        currentPlayerIndex = (int) (Math.random() * players.size());
        gameOver = false;
        consecutivePasses = 0;
        moveHistory.clear();

        logger.info("Game started with " + players.size() + " players");
        logger.info("First player: " + getCurrentPlayer().getName());
    }

    public void fillRack(Player player) {
        Rack rack = player.getRack();
        int tilesToDraw = rack.getEmptySlots();

        if (tilesToDraw == 0) {
            return;
        }

        List<Tile> drawnTiles = tileBag.drawTiles(tilesToDraw);
        rack.addTiles(drawnTiles);

        logger.fine("Filled " + player.getName() + "'s rack with " + drawnTiles.size() + " tiles");
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public void nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        logger.info("Current player is now: " + getCurrentPlayer().getName());
    }

    public Board getBoard() {
        return board;
    }

    public TileBag getTileBag() {
        return tileBag;
    }

    public Dictionary getDictionary() {
        return dictionary;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public List<Move> getMoveHistory() {
        return Collections.unmodifiableList(moveHistory);
    }

    // In Game.java

    public boolean executeMove(Move move) {
        if (gameOver) {
            logger.warning("Game is already over");
            return false;
        }

        if (move.getPlayer() != getCurrentPlayer()) {
            logger.warning("Not this player's turn: " + move.getPlayer().getName() +
                    " attempted to move during " + getCurrentPlayer().getName() + "'s turn");
            return false;
        }

        var success = false;

        try {
            switch (move.getType()) {
                case PLACE:
                    success = executePlaceMove(move);
                    break;
                case EXCHANGE:
                    success = executeExchangeMove(move);
                    break;
                case PASS:
                    success = executePassMove(move);
                    break;
                default:
                    logger.warning("Unknown move type: " + move.getType());
                    return false;
            }
        } catch (Exception e) {
            logger.severe("Error executing move: " + e.getMessage());
            return false;
        }

        if (success) {
            moveHistory.add(move);
            logger.info("Move executed: " + move);

            if (checkGameOver()) {
                finalizeGameScore();
                return true;
            }

            nextPlayer();
            logger.info("Next player is: " + getCurrentPlayer().getName() +
                    (getCurrentPlayer().isComputer() ? " (Computer)" : " (Human)"));
        }

        return success;
    }

    private boolean executePlaceMove(Move move) {
        // Validate the move
        if (!WordValidator.isValidPlaceMove(move, board, dictionary)) {
            logger.warning("Invalid place move");
            return false;
        }

        Player player = move.getPlayer();
        Board tempBoard = Board.copyBoard(board);
        List<Point> newTilePositions = new ArrayList<>();

        // Place tiles on temp board to validate words and calculate score
        placeTilesOnBoard(tempBoard, move, newTilePositions);

        // Validate words formed
        List<String> formedWords = WordValidator.validateWords(
                tempBoard, move, newTilePositions, dictionary);

        if (formedWords.isEmpty()) {
            logger.warning("No valid words formed");
            return false;
        }

        move.setFormedWords(formedWords);

        // Calculate score
        Set<Point> newPositionsSet = new HashSet<>(newTilePositions);
        int score = ScoreCalculator.calculateMoveScore(move, tempBoard, formedWords, newPositionsSet);
        move.setScore(score);

        // Apply the move to the actual board and update the player's rack
        for (int i = 0; i < move.getTiles().size(); i++) {
            Tile tile = move.getTiles().get(i);
            if (i < newTilePositions.size()) {
                Point position = newTilePositions.get(i);
                board.placeTile(position.x, position.y, tile);
                board.getSquare(position.x, position.y).usePremium();
                player.getRack().removeTile(tile);
            }
        }

        player.addScore(score);
        consecutivePasses = 0;
        fillRack(player);

        // Add bonus for using all tiles if the bag is empty
        if (player.getRack().isEmpty() && tileBag.isEmpty()) {
            player.addScore(EMPTY_RACK_BONUS);
            logger.info(player.getName() + " received " + EMPTY_RACK_BONUS +
                    " point bonus for using all tiles");
        }

        return true;
    }

    private void placeTilesOnBoard(Board targetBoard, Move move, List<Point> newTilePositions) {
        int row = move.getStartRow();
        int col = move.getStartCol();
        Move.Direction direction = move.getDirection();

        int currentRow = row;
        int currentCol = col;

        for (Tile tile : move.getTiles()) {
            // Skip over existing tiles
            while (currentRow < Board.SIZE && currentCol < Board.SIZE &&
                    targetBoard.getSquare(currentRow, currentCol).hasTile()) {
                if (direction == Move.Direction.HORIZONTAL) {
                    currentCol++;
                } else {
                    currentRow++;
                }
            }

            if (currentRow < Board.SIZE && currentCol < Board.SIZE) {
                targetBoard.placeTile(currentRow, currentCol, tile);
                newTilePositions.add(new Point(currentRow, currentCol));

                if (direction == Move.Direction.HORIZONTAL) {
                    currentCol++;
                } else {
                    currentRow++;
                }
            }
        }
    }

    private boolean executeExchangeMove(Move move) {
        Player player = move.getPlayer();
        List<Tile> tilesToExchange = move.getTiles();

        // Check if there are enough tiles in the bag for exchange
        if (tileBag.getTileCount() < 1) {
            logger.warning("Not enough tiles in bag for exchange");
            return false;
        }

        // Remove tiles from player's rack
        if (!player.getRack().removeTiles(tilesToExchange)) {
            logger.warning("Failed to remove tiles from rack");
            return false;
        }

        // Draw new tiles and return the exchanged ones to the bag
        int numTilesToDraw = tilesToExchange.size();
        List<Tile> newTiles = tileBag.drawTiles(numTilesToDraw);
        player.getRack().addTiles(newTiles);
        tileBag.returnTiles(tilesToExchange);

        consecutivePasses = 0;
        logger.info(player.getName() + " exchanged " + tilesToExchange.size() + " tiles");

        return true;
    }

    private boolean executePassMove(Move move) {
        consecutivePasses++;
        logger.info(move.getPlayer().getName() + " passed. Consecutive passes: " + consecutivePasses);
        return true;
    }

    public boolean checkGameOver() {
        // Check if any player is out of tiles
        for (Player player : players) {
            if (player.isOutOfTiles() && tileBag.isEmpty()) {
                gameOver = true;
                logger.info("Game over: " + player.getName() + " is out of tiles and bag is empty");
                return true;
            }
        }

        // Check for too many consecutive passes
        if (consecutivePasses >= GameConstants.CONSECUTIVE_PASSES_TO_END) {
            gameOver = true;
            logger.info("Game over: " + consecutivePasses + " consecutive passes");
            return true;
        }

        return false;
    }

    private void finalizeGameScore() {
        Player outPlayer = null;

        // Find player who went out (if any)
        for (Player player : players) {
            if (player.isOutOfTiles()) {
                outPlayer = player;
                break;
            }
        }

        if (outPlayer != null) {
            // Player who went out gets points from other players' racks
            int bonusPoints = 0;
            for (Player player : players) {
                if (player != outPlayer) {
                    int rackValue = player.getRackValue();
                    player.addScore(-rackValue);
                    bonusPoints += rackValue;
                    logger.info(player.getName() + " loses " + rackValue +
                            " points for tiles left in rack");
                }
            }
            outPlayer.addScore(bonusPoints);
            logger.info(outPlayer.getName() + " gains " + bonusPoints +
                    " points from other players' racks");
        } else {
            // Game ended due to passes - everyone loses points for tiles in rack
            for (Player player : players) {
                int rackValue = player.getRackValue();
                player.addScore(-rackValue);
                logger.info(player.getName() + " loses " + rackValue +
                        " points for tiles left in rack");
            }
        }

        // Log final scores
        StringBuilder sb = new StringBuilder("Final scores:");
        for (Player player : players) {
            sb.append(" ").append(player.getName()).append(": ").append(player.getScore());
        }
        logger.info(sb.toString());
    }
}