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

public class GameController {
    private static final Logger logger = Logger.getLogger(GameController.class.getName());

    private final Game game;
    private final MoveHandler moveHandler;
    private final TilePlacer tilePlacer;
    private final List<ComputerPlayer> computerPlayers;
    private final ExecutorService executor;

    private DictionaryService dictionaryService;
    private WordDefinitionDialog definitionDialog;
    private boolean showDefinitionsEnabled = true;

    private boolean gameInProgress;
    private volatile boolean computerMoveInProgress;

    // View update listeners
    private Runnable boardUpdateListener;
    private Runnable rackUpdateListener;
    private Runnable playerUpdateListener;
    private Runnable gameOverListener;
    private Runnable temporaryPlacementListener;

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

    public void showDefinitionForWord(String word) {
        if (showDefinitionsEnabled) {
            definitionDialog.showDefinition(word);
        }
    }

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

// Update commitPlacement, exchangeTiles, and passTurn methods to also trigger AI moves

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

    // Improve the makeComputerMoveIfNeeded method to handle multiple AI turns if needed
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

    // In GameController.java

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

    public void setTemporaryPlacementListener(Runnable listener) {
        this.temporaryPlacementListener = listener;
    }

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


    public void cancelPlacements() {
        moveHandler.cancelPlacements();
        updateBoard();

        // Add this line to update button states when placements are canceled
        if (temporaryPlacementListener != null) {
            Platform.runLater(temporaryPlacementListener);
        }
    }


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


    // Modify the shutdown method to close the definition dialog
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