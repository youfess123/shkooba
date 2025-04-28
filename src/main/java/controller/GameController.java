package controller;

import javafx.application.Platform;
import model.*;
import service.DictionaryService;
import utilities.GameConstants;
import view.WordDefinitionDialog;

import java.awt.Point;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Controller for the Scrabble game.
 * Manages game state, processes player actions, and updates the view.
 */
public class GameController {
    private static final Logger logger = Logger.getLogger(GameController.class.getName());

    private final Game game;
    private final MoveHandler moveHandler;
    private final TilePlacer tilePlacer;
    private final List<ComputerPlayer> computerPlayers;
    private final ExecutorService executor;
    private final DictionaryService dictionaryService;
    private final WordDefinitionDialog definitionDialog;
    private boolean showDefinitionsEnabled = true; // Allow users to toggle this feature

    private boolean gameInProgress;
    private volatile boolean computerMoveInProgress;

    // View update listeners
    private Runnable boardUpdateListener;
    private Runnable rackUpdateListener;
    private Runnable playerUpdateListener;
    private Runnable gameOverListener;
    private Runnable temporaryPlacementListener;

    /**
     * Creates a new game controller for the specified game.
     *
     * @param game The game to control
     */
    public GameController(Game game) {
        this.game = game;
        this.moveHandler = new MoveHandler(game);
        this.tilePlacer = new TilePlacer();
        this.computerPlayers = new ArrayList<>();
        this.executor = Executors.newSingleThreadExecutor();
        this.gameInProgress = false;
        this.computerMoveInProgress = false;

        // Initialize the dictionary service
        this.dictionaryService = new DictionaryService();
        this.definitionDialog = new WordDefinitionDialog(dictionaryService);

        // Initialize computer players
        for (Player player : game.getPlayers()) {
            if (player.isComputer()) {
                computerPlayers.add(new ComputerPlayer(player, GameConstants.AI_MEDIUM));
            }
        }
    }

    /**
     * Starts the game.
     */
    public void startGame() {
        game.start();
        gameInProgress = true;
        updateBoard();
        updateRack();
        updateCurrentPlayer();
        makeComputerMoveIfNeeded();
    }

    /**
     * Executes a move in the game.
     *
     * @param move The move to execute
     * @return true if the move was executed successfully, false otherwise
     */
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

            // Show word definitions if it's a PLACE move and not a computer player
            if (move.getType() == Move.Type.PLACE && !move.getPlayer().isComputer() && showDefinitionsEnabled) {
                showDefinitionsForMove(move);
            }

            // Check game over
            if (game.isGameOver()) {
                gameInProgress = false;
                if (gameOverListener != null) {
                    gameOverListener.run();
                }
                return true;
            }

            // Trigger computer move if needed - ensure this happens after player moves
            makeComputerMoveIfNeeded();
        }

        return success;
    }

    /**
     * Commits the current temporary tile placement as a move.
     *
     * @return true if the move was successful, false otherwise
     */
    public boolean commitPlacement() {
        boolean success = moveHandler.commitPlacement();
        if (success) {
            updateBoard();
            updateRack();
            updateCurrentPlayer();
            makeComputerMoveIfNeeded(); // Add this call to ensure AI plays after player
        }
        return success;
    }

    /**
     * Exchanges selected tiles for new ones from the bag.
     *
     * @return true if the exchange was successful, false otherwise
     */
    public boolean exchangeTiles() {
        List<Tile> selectedTiles = tilePlacer.getSelectedTiles();
        boolean success = moveHandler.exchangeTiles(selectedTiles);
        if (success) {
            tilePlacer.clearSelectedTiles();
            updateBoard();
            updateRack();
            updateCurrentPlayer();
            makeComputerMoveIfNeeded(); // Add this call
        }
        return success;
    }

    /**
     * Passes the current player's turn.
     *
     * @return true if the pass was successful, false otherwise
     */
    public boolean passTurn() {
        boolean success = moveHandler.passTurn();
        if (success) {
            updateBoard();
            updateRack();
            updateCurrentPlayer();
            makeComputerMoveIfNeeded(); // Add this call
        }
        return success;
    }

    /**
     * Makes a computer move if the current player is a computer.
     */
    public void makeComputerMoveIfNeeded() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer.isComputer() && !computerMoveInProgress && gameInProgress) {
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

    /**
     * Gets the computer player object for a player.
     *
     * @param player The player
     * @return The computer player, or null if not found
     */
    private ComputerPlayer getComputerPlayerFor(Player player) {
        for (ComputerPlayer cp : computerPlayers) {
            if (cp.getPlayer() == player) {
                return cp;
            }
        }
        return null;
    }

    /**
     * Sets up an emergency timer to force a pass move if the computer takes too long.
     *
     * @param currentPlayer The current player
     * @return The scheduled executor service for the timer
     */
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

    /**
     * Executes a computer move on a separate thread.
     *
     * @param computerPlayer The computer player
     * @param currentPlayer The current player
     * @param emergencyTimer The emergency timer
     */
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
                        // Use makeMove instead of directly calling game.executeMove
                        // This ensures that if there are multiple AI players, they will play in sequence
                        boolean success = makeMove(computerMove);

                        if (!success) {
                            logger.warning("Computer move failed, passing turn");
                            Move passMove = Move.createPassMove(currentPlayer);
                            makeMove(passMove);
                        }
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

    /**
     * Sets the temporary placement listener.
     *
     * @param listener The listener
     */
    public void setTemporaryPlacementListener(Runnable listener) {
        this.temporaryPlacementListener = listener;
    }

    /**
     * Places a tile temporarily on the board.
     *
     * @param rackIndex The index of the tile in the rack
     * @param row The row coordinate
     * @param col The column coordinate
     * @return true if the placement was successful, false otherwise
     */
    public boolean placeTileTemporarily(int rackIndex, int row, int col) {
        boolean success = moveHandler.placeTileTemporarily(rackIndex, row, col);
        if (success) {
            updateBoard();
            updateRack();

            // Add this line to update button states when tiles are placed
            if (temporaryPlacementListener != null) {
                Platform.runLater(temporaryPlacementListener);
            }
        }
        return success;
    }

    /**
     * Cancels all temporary tile placements.
     */
    public void cancelPlacements() {
        moveHandler.cancelPlacements();
        updateBoard();

        // Add this line to update button states when placements are canceled
        if (temporaryPlacementListener != null) {
            Platform.runLater(temporaryPlacementListener);
        }
    }

    /**
     * Selects or deselects a tile in the rack.
     *
     * @param index The index of the tile in the rack
     */
    public void selectTileFromRack(int index) {
        if (!gameInProgress) {
            return;
        }

        Player currentPlayer = game.getCurrentPlayer();
        tilePlacer.selectTileFromRack(currentPlayer, index);
        updateRack();

        if (temporaryPlacementListener != null) {
            Platform.runLater(temporaryPlacementListener);
        }
    }

    /**
     * Sets the letter for a blank tile.
     *
     * @param rackIndex The index of the tile in the rack
     * @param letter The letter to assign to the blank tile
     */
    public void setBlankTileLetter(int rackIndex, char letter) {
        Player currentPlayer = game.getCurrentPlayer();
        tilePlacer.setBlankTileLetter(currentPlayer, rackIndex, letter);
        updateRack();
    }

    /**
     * Shows definitions for words formed in a move.
     *
     * @param move The move containing words to show definitions for
     */
    private void showDefinitionsForMove(Move move) {
        List<String> words = move.getFormedWords();
        if (words != null && !words.isEmpty()) {
            // If only one word was formed, show its definition directly
            if (words.size() == 1) {
                definitionDialog.showDefinition(words.get(0));
            } else {
                // If multiple words were formed, show a list to choose from
                definitionDialog.showDefinitions(words);
            }
        }
    }

    /**
     * Shows the definition dialog for a specific word.
     *
     * @param word The word to look up
     */
    public void showDefinitionForWord(String word) {
        if (showDefinitionsEnabled) {
            definitionDialog.showDefinition(word);
        }
    }

    /**
     * Shows a list of words played in the game that the user can select to see definitions.
     */
    public void showWordHistory() {
        List<String> playedWords = new ArrayList<>();

        // Collect all words from move history
        for (Move move : game.getMoveHistory()) {
            if (move.getType() == Move.Type.PLACE) {
                playedWords.addAll(move.getFormedWords());
            }
        }

        // Remove duplicates
        List<String> uniqueWords = new ArrayList<>(new HashSet<>(playedWords));

        // Sort alphabetically
        Collections.sort(uniqueWords);

        if (!uniqueWords.isEmpty()) {
            definitionDialog.showDefinitions(uniqueWords);
        }
    }

    /**
     * Enables or disables the automatic display of word definitions.
     *
     * @param enabled Whether to show definitions automatically after moves
     */
    public void setShowDefinitionsEnabled(boolean enabled) {
        this.showDefinitionsEnabled = enabled;
    }

    /**
     * Checks if word definitions are enabled.
     *
     * @return Whether word definitions are enabled
     */
    public boolean isShowDefinitionsEnabled() {
        return showDefinitionsEnabled;
    }

    // Game state getters

    /**
     * Gets the game board.
     *
     * @return The board
     */
    public Board getBoard() {
        return game.getBoard();
    }

    /**
     * Gets the current player.
     *
     * @return The current player
     */
    public Player getCurrentPlayer() {
        return game.getCurrentPlayer();
    }

    /**
     * Gets all players in the game.
     *
     * @return The list of players
     */
    public List<Player> getPlayers() {
        return game.getPlayers();
    }

    /**
     * Gets the move history.
     *
     * @return The list of moves
     */
    public List<Move> getMoveHistory() {
        return game.getMoveHistory();
    }

    /**
     * Gets the number of tiles remaining in the bag.
     *
     * @return The number of tiles
     */
    public int getRemainingTileCount() {
        return game.getTileBag().getTileCount();
    }

    // Status query methods

    /**
     * Checks if a position has a temporary tile.
     *
     * @param row The row coordinate
     * @param col The column coordinate
     * @return true if there is a temporary tile at the position, false otherwise
     */
    public boolean hasTemporaryTileAt(int row, int col) {
        return moveHandler.hasTemporaryTileAt(row, col);
    }

    /**
     * Gets the temporary tile at a position.
     *
     * @param row The row coordinate
     * @param col The column coordinate
     * @return The tile, or null if none
     */
    public Tile getTemporaryTileAt(int row, int col) {
        return moveHandler.getTemporaryTileAt(row, col);
    }

    /**
     * Checks if a temporary placement at a position would be valid.
     *
     * @param row The row coordinate
     * @param col The column coordinate
     * @return true if the placement would be valid, false otherwise
     */
    public boolean isValidTemporaryPlacement(int row, int col) {
        return moveHandler.isValidTemporaryPlacement(row, col);
    }

    /**
     * Determines the direction of the current temporary placements.
     *
     * @return The direction (horizontal or vertical)
     */
    public Move.Direction determineDirection() {
        return moveHandler.determineDirection();
    }

    /**
     * Checks if a tile in the rack is selected.
     *
     * @param index The index of the tile in the rack
     * @return true if the tile is selected, false otherwise
     */
    public boolean isTileSelected(int index) {
        return tilePlacer.isTileSelected(index);
    }

    // Getters for collections

    /**
     * Gets all temporary tile placements.
     *
     * @return A map of positions to tiles
     */
    public Map<Point, Tile> getTemporaryPlacements() {
        return moveHandler.getTemporaryPlacements();
    }

    /**
     * Gets all selected tiles.
     *
     * @return The list of selected tiles
     */
    public List<Tile> getSelectedTiles() {
        return tilePlacer.getSelectedTiles();
    }

    // View update methods

    /**
     * Updates the board view.
     */
    private void updateBoard() {
        if (boardUpdateListener != null) {
            Platform.runLater(boardUpdateListener);
        }
    }

    /**
     * Updates the rack view.
     */
    private void updateRack() {
        if (rackUpdateListener != null) {
            Platform.runLater(rackUpdateListener);
        }
    }

    /**
     * Updates the current player display.
     */
    private void updateCurrentPlayer() {
        if (playerUpdateListener != null) {
            Platform.runLater(playerUpdateListener);
        }
    }

    // Listener setters

    /**
     * Sets the board update listener.
     *
     * @param listener The listener
     */
    public void setBoardUpdateListener(Runnable listener) {
        this.boardUpdateListener = listener;
    }

    /**
     * Sets the rack update listener.
     *
     * @param listener The listener
     */
    public void setRackUpdateListener(Runnable listener) {
        this.rackUpdateListener = listener;
    }

    /**
     * Sets the player update listener.
     *
     * @param listener The listener
     */
    public void setPlayerUpdateListener(Runnable listener) {
        this.playerUpdateListener = listener;
    }

    /**
     * Sets the game over listener.
     *
     * @param listener The listener
     */
    public void setGameOverListener(Runnable listener) {
        this.gameOverListener = listener;
    }

    // Resource cleanup

    /**
     * Shuts down the controller and releases resources.
     */
    public void shutdown() {
        if (definitionDialog != null) {
            definitionDialog.close();
        }

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