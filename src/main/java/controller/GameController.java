package controller;


import javafx.application.Platform;
import model.*;
import service.DictionaryService;
import utilities.WordFinder;
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
    private final Map<Player, ComputerPlayer> computerPlayers;
    private final ExecutorService executor;

    private DictionaryService dictionaryService;
    private WordDefinitionDialog definitionDialog;
    private boolean showDefinitionsEnabled = true;

    private boolean gameInProgress;
    private volatile boolean computerMoveInProgress;

    private Runnable boardUpdateListener;
    private Runnable rackUpdateListener;
    private Runnable playerUpdateListener;
    private Runnable gameOverListener;
    private Runnable temporaryPlacementListener;

    public GameController(Game game) {
        this.game = game;
        this.moveHandler = new MoveHandler(game);
        this.tilePlacer = new TilePlacer();
        this.computerPlayers = new HashMap<>();
        this.executor = Executors.newSingleThreadExecutor();
        this.gameInProgress = false;
        this.computerMoveInProgress = false;

        this.dictionaryService = new DictionaryService();
        this.definitionDialog = new WordDefinitionDialog(dictionaryService);

        for (Player player : game.getPlayers()) {
            if (player.isComputer()) {
                computerPlayers.put(player, new ComputerPlayer(player, game.getAiDifficulty()));
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
            tilePlacer.clearSelectedTiles();

            updateBoard();
            updateRack();
            updateCurrentPlayer();

            if (move.getType() == Move.Type.PLACE && !move.getPlayer().isComputer() && showDefinitionsEnabled) {
                showDefinitionsForMove(move);
            }

            if (game.isGameOver()) {
                gameInProgress = false;
                if (gameOverListener != null) {
                    gameOverListener.run();
                }
                return true;
            }

            makeComputerMoveIfNeeded();
        }

        return success;
    }

    private void showDefinitionsForMove(Move move) {
        List<String> words = move.getFormedWords();
        if (words != null && !words.isEmpty()) {
            definitionDialog.showDefinitions(words);
        }
    }

    public List<WordFinder.WordPlacement> generateHints() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer.isComputer() || computerMoveInProgress) {
            return new ArrayList<>();
        }

        try {
            // Create a WordFinder for current board state
            WordFinder wordFinder = new WordFinder(game.getDictionary(), game.getBoard());

            List<WordFinder.WordPlacement> placements = wordFinder.findAllPlacements(currentPlayer.getRack());
            logger.info("Found " + placements.size() + " possible placements for hints");

            if (placements.isEmpty()) {
                return new ArrayList<>();
            }

            placements.sort(Comparator.comparing(WordFinder.WordPlacement::getScore).reversed());

            int movesToLog = Math.min(3, placements.size());
            for (int i = 0; i < movesToLog; i++) {
                WordFinder.WordPlacement placement = placements.get(i);
                logger.info(String.format("Potential hint %d: %d points - %s at (%d,%d) %s",
                        i+1, placement.getScore(), placement.getWord(),
                        placement.getRow() + 1, placement.getCol() + 1,
                        placement.getDirection() == Move.Direction.HORIZONTAL ? "horizontal" : "vertical"));
            }

            // Limit to top N moves
            int maxHints = Math.min(15, placements.size());
            return placements.subList(0, maxHints);
        } catch (Exception e) {
            logger.severe("Error generating hints: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void showWordHistory() {
        List<String> playedWords = new ArrayList<>();

        // collect words from move history
        for (Move move : game.getMoveHistory()) {
            if (move.getType() == Move.Type.PLACE) {
                playedWords.addAll(move.getFormedWords());
            }
        }
        //no duplicates
        List<String> uniqueWords = new ArrayList<>(new HashSet<>(playedWords));
        Collections.sort(uniqueWords);
        if (!uniqueWords.isEmpty()) {
            definitionDialog.showDefinitions(uniqueWords);
        }
    }

    public boolean commitPlacement() {
        boolean success = moveHandler.commitPlacement();
        if (success) {
            updateBoard();
            updateRack();
            updateCurrentPlayer();
            makeComputerMoveIfNeeded();
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
            makeComputerMoveIfNeeded();
        }
        return success;
    }

    public boolean passTurn() {
        boolean success = moveHandler.passTurn();
        if (success) {
            updateBoard();
            updateRack();
            updateCurrentPlayer();
            makeComputerMoveIfNeeded();
        }
        return success;
    }

    public void makeComputerMoveIfNeeded() {
        if (!gameInProgress || computerMoveInProgress) {
            return;
        }

        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer.isComputer()) {
            logger.info("Computer's turn - preparing move for " + currentPlayer.getName());
            computerMoveInProgress = true;
            updateCurrentPlayer();

            ComputerPlayer computerPlayer = computerPlayers.get(currentPlayer);
            if (computerPlayer == null) {
                logger.warning("Computer player not found for " + currentPlayer.getName());
                Move passMove = Move.createPassMove(currentPlayer);
                computerMoveInProgress = false; // Reset flag before making move
                makeMove(passMove);
                return;
            }

            ScheduledExecutorService emergencyTimer = setupEmergencyTimer(currentPlayer);

            executeComputerMove(computerPlayer, currentPlayer, emergencyTimer);
        }
    }

    private ScheduledExecutorService setupEmergencyTimer(Player currentPlayer) {
        ScheduledExecutorService emergencyTimer = Executors.newSingleThreadScheduledExecutor();
        emergencyTimer.schedule(() -> {
            if (computerMoveInProgress) {
                logger.warning("Computer move taking too long - forcing PASS for " + currentPlayer.getName());
                Platform.runLater(() -> {
                    Move passMove = Move.createPassMove(currentPlayer);
                    computerMoveInProgress = false; // Reset flag before making move
                    makeMove(passMove);
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
                        computerMoveInProgress = false;

                        boolean success = makeMove(computerMove);

                        if (!success) {
                            logger.warning("Computer move failed for " + currentPlayer.getName() + ", passing turn");
                            Move passMove = Move.createPassMove(currentPlayer);
                            makeMove(passMove);
                        }
                    } catch (Exception e) {
                        logger.severe("Error executing computer move for " + currentPlayer.getName() + ": " + e.getMessage());
                        computerMoveInProgress = false;
                        Move passMove = Move.createPassMove(currentPlayer);
                        makeMove(passMove);
                    }
                });
            } catch (Exception e) {
                logger.severe("Error in computer move for " + currentPlayer.getName() + ": " + e.getMessage());
                Platform.runLater(() -> {
                    emergencyTimer.shutdownNow();
                    computerMoveInProgress = false;
                    Move passMove = Move.createPassMove(currentPlayer);
                    makeMove(passMove);
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

            if (temporaryPlacementListener != null) {
                Platform.runLater(temporaryPlacementListener);
            }
        }
        return success;
    }

    public void cancelPlacements() {
        moveHandler.cancelPlacements();
        updateBoard();

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

    public Map<Point, Tile> getTemporaryPlacements() {
        return moveHandler.getTemporaryPlacements();
    }

    public List<Tile> getSelectedTiles() {
        return tilePlacer.getSelectedTiles();
    }

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

    public void setShowDefinitionsEnabled(boolean enabled) {
        this.showDefinitionsEnabled = enabled;
    }

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