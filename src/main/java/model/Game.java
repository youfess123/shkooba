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
    private final List<Move> moveHistory;

    private int currentPlayerIndex;
    private int aiDifficulty = GameConstants.AI_EASY;
    private boolean gameOver;
    private int consecutivePasses;

    // Initialization and setup
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

    public void setAiDifficulty(int difficulty) {
        this.aiDifficulty = Math.max(1, Math.min(3, difficulty));
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

    // Player management
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public void nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        logger.info("Current player is now: " + getCurrentPlayer().getName());
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

    // Move execution
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
        if (!WordValidator.isValidPlaceMove(move, board, dictionary)) {
            logger.warning("Invalid place move");
            return false;
        }

        Player player = move.getPlayer();
        Board tempBoard = Board.copyBoard(board);
        List<Point> newTilePositions = new ArrayList<>();

        placeTilesOnBoard(tempBoard, move, newTilePositions);

        List<String> formedWords = WordValidator.validateWords(
                tempBoard, move, newTilePositions, dictionary);

        if (formedWords.isEmpty()) {
            logger.warning("No valid words formed");
            return false;
        }

        move.setFormedWords(formedWords);

        Set<Point> newPositionsSet = new HashSet<>(newTilePositions);
        int score = ScoreCalculator.calculateMoveScore(move, tempBoard, formedWords, newPositionsSet);
        move.setScore(score);

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

        if (tileBag.getTileCount() < 1) {
            logger.warning("Not enough tiles in bag for exchange");
            return false;
        }

        if (!player.getRack().removeTiles(tilesToExchange)) {
            logger.warning("Failed to remove tiles from rack");
            return false;
        }

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

    // Game state management
    public boolean checkGameOver() {
        for (Player player : players) {
            if (player.isOutOfTiles() && tileBag.isEmpty()) {
                gameOver = true;
                logger.info("Game over: " + player.getName() + " is out of tiles and bag is empty");
                return true;
            }
        }

        if (consecutivePasses >= GameConstants.CONSECUTIVE_PASSES_TO_END) {
            gameOver = true;
            logger.info("Game over: " + consecutivePasses + " consecutive passes");
            return true;
        }

        return false;
    }

    private void finalizeGameScore() {
        Player outPlayer = null;

        for (Player player : players) {
            if (player.isOutOfTiles()) {
                outPlayer = player;
                break;
            }
        }

        if (outPlayer != null) {
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
            for (Player player : players) {
                int rackValue = player.getRackValue();
                player.addScore(-rackValue);
                logger.info(player.getName() + " loses " + rackValue +
                        " points for tiles left in rack");
            }
        }

        StringBuilder sb = new StringBuilder("Final scores:");
        for (Player player : players) {
            sb.append(" ").append(player.getName()).append(": ").append(player.getScore());
        }
        logger.info(sb.toString());
    }

    // Accessors
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

    public List<Move> getMoveHistory() {
        return Collections.unmodifiableList(moveHistory);
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getAiDifficulty() {
        return aiDifficulty;
    }
}