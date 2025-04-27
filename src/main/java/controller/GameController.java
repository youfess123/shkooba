package controller;


import javafx.application.Platform;
import model.*;
import utilities.GameConstants;

import java.awt.Point;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class GameController {
    private static final Logger logger = Logger.getLogger(GameController.class.getName());

    private final Game game;
    private final MoveHandler moveHandler;
    private final TilePlacer tilePlacer;
    private final List<ComputerPlayer> computerPlayers;
    private final ExecutorService executor;

    private boolean gameInProgress;
    private volatile boolean computerMoveInProgress;

    // View update listeners
    private Runnable boardUpdateListener;
    private Runnable rackUpdateListener;
    private Runnable playerUpdateListener;
    private Runnable gameOverListener;

    public GameController(Game game) {
        this.game = game;
        this.moveHandler = new MoveHandler(game);
        this.tilePlacer = new TilePlacer();
        this.computerPlayers = new ArrayList<>();
        this.executor = Executors.newSingleThreadExecutor();
        this.gameInProgress = false;
        this.computerMoveInProgress = false;

        // Initialize computer players
        for (Player player : game.getPlayers()) {
            if (player.isComputer()) {
                computerPlayers.add(new ComputerPlayer(player, GameConstants.AI_MEDIUM));
            }
        }
    }

    public void startGame() {
        game.start();
        gameInProgress = true;
        updateBoard();
        updateRack();
        updateCurrentPlayer();
        makeComputerMoveIfNeeded();
    }

    public boolean makeMove(Move move) {
        if (!gameInProgress) {
            return false;
        }

        boolean success = game.executeMove(move);
        if (success) {
            logger.info("Move executed: " + move.getType() + " by " + move.getPlayer().getName());

            // Clear selections
            tilePlacer.clearSelectedTiles();

            // Update views
            updateBoard();
            updateRack();
            updateCurrentPlayer();


            // Check game over
            if (game.isGameOver()) {
                gameInProgress = false;
                if (gameOverListener != null) {
                    gameOverListener.run();
                }
                return true;
            }

            // Trigger computer move if needed
            makeComputerMoveIfNeeded();
        }

        return success;
    }

    public void makeComputerMoveIfNeeded() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer.isComputer() && !computerMoveInProgress) {
            logger.info("Computer's turn - preparing move");
            computerMoveInProgress = true;
            updateCurrentPlayer();

            ComputerPlayer computerPlayer = getComputerPlayerFor(currentPlayer);
            if (computerPlayer == null) {
                logger.warning("Computer player not found");
                Move passMove = Move.createPassMove(currentPlayer);
                makeMove(passMove);
                computerMoveInProgress = false;
                return;
            }

            // Set up emergency timeout
            ScheduledExecutorService emergencyTimer = setupEmergencyTimer(currentPlayer);

            // Execute computer move on a separate thread
            executeComputerMove(computerPlayer, currentPlayer, emergencyTimer);
        }
    }

    private ComputerPlayer getComputerPlayerFor(Player player) {
        for (ComputerPlayer cp : computerPlayers) {
            if (cp.getPlayer() == player) {
                return cp;
            }
        }
        return null;
    }

    private ScheduledExecutorService setupEmergencyTimer(Player currentPlayer) {
        ScheduledExecutorService emergencyTimer = Executors.newSingleThreadScheduledExecutor();
        emergencyTimer.schedule(() -> {
            if (computerMoveInProgress) {
                logger.warning("Computer move taking too long - forcing PASS");
                Platform.runLater(() -> {
                    Move passMove = Move.createPassMove(currentPlayer);
                    makeMove(passMove);
                    computerMoveInProgress = false;
                });
            }
        }, 5, TimeUnit.SECONDS);

        return emergencyTimer;
    }

    private void executeComputerMove(ComputerPlayer computerPlayer, Player currentPlayer,
                                     ScheduledExecutorService emergencyTimer) {
        executor.submit(() -> {
            try {
                Move computerMove = computerPlayer.generateMove(game);
                Thread.sleep(1000); // Small delay for better UX

                // Cancel the emergency timeout
                emergencyTimer.shutdownNow();

                Platform.runLater(() -> {
                    try {
                        makeMove(computerMove);
                    } catch (Exception e) {
                        logger.severe("Error executing computer move: " + e.getMessage());
                        Move passMove = Move.createPassMove(currentPlayer);
                        makeMove(passMove);
                    } finally {
                        computerMoveInProgress = false;
                    }
                });
            } catch (Exception e) {
                logger.severe("Error in computer move: " + e.getMessage());
                Platform.runLater(() -> {
                    emergencyTimer.shutdownNow();
                    Move passMove = Move.createPassMove(currentPlayer);
                    makeMove(passMove);
                    computerMoveInProgress = false;
                });
            }
        });
    }

    // Player move handling methods

    public boolean placeTileTemporarily(int rackIndex, int row, int col) {
        boolean success = moveHandler.placeTileTemporarily(rackIndex, row, col);
        if (success) {
            updateBoard();
            updateRack();
        }
        return success;
    }

    public boolean commitPlacement() {
        boolean success = moveHandler.commitPlacement();
        if (success) {
            updateBoard();
            updateRack();
            updateCurrentPlayer();
        }
        return success;
    }

    public void cancelPlacements() {
        moveHandler.cancelPlacements();
        updateBoard();
    }

    public boolean exchangeTiles() {
        List<Tile> selectedTiles = tilePlacer.getSelectedTiles();
        boolean success = moveHandler.exchangeTiles(selectedTiles);
        if (success) {
            tilePlacer.clearSelectedTiles();
            updateBoard();
            updateRack();
            updateCurrentPlayer();
        }
        return success;
    }

    public boolean passTurn() {
        boolean success = moveHandler.passTurn();
        if (success) {
            updateBoard();
            updateRack();
            updateCurrentPlayer();
        }
        return success;
    }

    public void selectTileFromRack(int index) {
        if (!gameInProgress) {
            return;
        }

        Player currentPlayer = game.getCurrentPlayer();
        tilePlacer.selectTileFromRack(currentPlayer, index);
        updateRack();
    }

    public void setBlankTileLetter(int rackIndex, char letter) {
        Player currentPlayer = game.getCurrentPlayer();
        tilePlacer.setBlankTileLetter(currentPlayer, rackIndex, letter);
        updateRack();
    }

    // Game state getters

    public Board getBoard() {
        return game.getBoard();
    }

    public Player getCurrentPlayer() {
        return game.getCurrentPlayer();
    }

    public List<Player> getPlayers() {
        return game.getPlayers();
    }

    public List<Move> getMoveHistory() {
        return game.getMoveHistory();
    }

    public int getRemainingTileCount() {
        return game.getTileBag().getTileCount();
    }

    // Status query methods

    public boolean hasTemporaryTileAt(int row, int col) {
        return moveHandler.hasTemporaryTileAt(row, col);
    }

    public Tile getTemporaryTileAt(int row, int col) {
        return moveHandler.getTemporaryTileAt(row, col);
    }

    public boolean isValidTemporaryPlacement(int row, int col) {
        return moveHandler.isValidTemporaryPlacement(row, col);
    }

    public Move.Direction determineDirection() {
        return moveHandler.determineDirection();
    }

    public boolean isTileSelected(int index) {
        return tilePlacer.isTileSelected(index);
    }

    // Getters for collections

    public Map<Point, Tile> getTemporaryPlacements() {
        return moveHandler.getTemporaryPlacements();
    }

    public List<Tile> getSelectedTiles() {
        return tilePlacer.getSelectedTiles();
    }

    // View update methods

    private void updateBoard() {
        if (boardUpdateListener != null) {
            Platform.runLater(boardUpdateListener);
        }
    }

    private void updateRack() {
        if (rackUpdateListener != null) {
            Platform.runLater(rackUpdateListener);
        }
    }

    private void updateCurrentPlayer() {
        if (playerUpdateListener != null) {
            Platform.runLater(playerUpdateListener);
        }
    }

    // Listener setters

    public void setBoardUpdateListener(Runnable listener) {
        this.boardUpdateListener = listener;
    }

    public void setRackUpdateListener(Runnable listener) {
        this.rackUpdateListener = listener;
    }

    public void setPlayerUpdateListener(Runnable listener) {
        this.playerUpdateListener = listener;
    }

    public void setGameOverListener(Runnable listener) {
        this.gameOverListener = listener;
    }

    // Resource cleanup

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}