package controller;

import model.*;
import utilities.BoardUtils;
import utilities.GameConstants;

import java.awt.Point;
import java.util.*;
import java.util.logging.Logger;

public class MoveHandler {
    private static final Logger logger = Logger.getLogger(MoveHandler.class.getName());

    private final Game game;
    private final Map<Point, Tile> temporaryPlacements;
    private final List<Integer> temporaryIndices;

    public MoveHandler(Game game) {
        this.game = game;
        this.temporaryPlacements = new HashMap<>();
        this.temporaryIndices = new ArrayList<>();
    }

    public boolean placeTileTemporarily(int rackIndex, int row, int col) {
        try {
            // Basic validation
            if (row < 0 || row >= Board.SIZE || col < 0 || col >= Board.SIZE) {
                return false;
            }

            Player currentPlayer = game.getCurrentPlayer();
            Rack rack = currentPlayer.getRack();

            if (rackIndex < 0 || rackIndex >= rack.size()) {
                return false;
            }

            if (game.getBoard().getSquare(row, col).hasTile() || hasTemporaryTileAt(row, col)) {
                return false;
            }

            Tile tile = rack.getTile(rackIndex);

            if (temporaryIndices.contains(rackIndex)) {
                return false;
            }

            if (!isValidTemporaryPlacement(row, col)) {
                return false;
            }

            // Place the tile temporarily
            temporaryPlacements.put(new Point(row, col), tile);
            temporaryIndices.add(rackIndex);

            logger.fine("Tile " + tile.getLetter() + " temporarily placed at (" + row + "," + col + ")");
            return true;
        } catch (Exception e) {
            logger.warning("Error in placeTileTemporarily: " + e.getMessage());
            return false;
        }
    }

    public boolean hasTemporaryTileAt(int row, int col) {
        return temporaryPlacements.containsKey(new Point(row, col));
    }

    public Tile getTemporaryTileAt(int row, int col) {
        return temporaryPlacements.get(new Point(row, col));
    }

    public boolean isValidTemporaryPlacement(int row, int col) {
        Board board = game.getBoard();

        // Check for existing tile
        if (board.getSquare(row, col).hasTile() || hasTemporaryTileAt(row, col)) {
            return false;
        }

        // For the first move, require the center square
        if (board.isEmpty() && temporaryPlacements.isEmpty()) {
            return row == GameConstants.CENTER_SQUARE && col == GameConstants.CENTER_SQUARE;
        }

        // Check for placement direction constraints
        if (!temporaryPlacements.isEmpty()) {
            return isValidDirectionalPlacement(row, col);
        }

        // For subsequent moves, must connect to existing tiles
        return BoardUtils.hasAdjacentTile(board, row, col);
    }

    private boolean isValidDirectionalPlacement(int row, int col) {
        List<Point> placementPoints = new ArrayList<>(temporaryPlacements.keySet());

        // For the first temporary tile, any adjacent square is valid
        if (placementPoints.size() == 1) {
            Point existingPoint = placementPoints.get(0);
            return (row == existingPoint.x || col == existingPoint.y);
        }

        // For subsequent tiles, must follow the established direction
        Move.Direction direction = determineDirection();
        if (direction == null) {
            return false;
        }

        return isValidPlacementInDirection(row, col, direction, placementPoints);
    }

    private boolean isValidPlacementInDirection(int row, int col, Move.Direction direction,
                                                List<Point> placementPoints) {
        Board board = game.getBoard();

        if (direction == Move.Direction.HORIZONTAL) {
            // Check if all placements are in the same row
            for (Point p : placementPoints) {
                if (p.x != row) {
                    return false;
                }
            }

            // Check if the new column is adjacent to existing placements
            int minCol = Integer.MAX_VALUE;
            int maxCol = Integer.MIN_VALUE;
            for (Point p : placementPoints) {
                minCol = Math.min(minCol, p.y);
                maxCol = Math.max(maxCol, p.y);
            }

            // Allow placement adjacent to existing temporary tiles
            if (col >= minCol - 1 && col <= maxCol + 1) {
                return true;
            }

            // Or adjacent to a board tile
            return (col > 0 && board.getSquare(row, col - 1).hasTile()) ||
                    (col < Board.SIZE - 1 && board.getSquare(row, col + 1).hasTile());
        } else {
            // Check if all placements are in the same column
            for (Point p : placementPoints) {
                if (p.y != col) {
                    return false;
                }
            }

            // Check if the new row is adjacent to existing placements
            int minRow = Integer.MAX_VALUE;
            int maxRow = Integer.MIN_VALUE;
            for (Point p : placementPoints) {
                minRow = Math.min(minRow, p.x);
                maxRow = Math.max(maxRow, p.x);
            }

            // Allow placement adjacent to existing temporary tiles
            if (row >= minRow - 1 && row <= maxRow + 1) {
                return true;
            }

            // Or adjacent to a board tile
            return (row > 0 && board.getSquare(row - 1, col).hasTile()) ||
                    (row < Board.SIZE - 1 && board.getSquare(row + 1, col).hasTile());
        }
    }

    public Move.Direction determineDirection() {
        if (temporaryPlacements.size() <= 1) {
            if (temporaryPlacements.size() == 1) {
                return determineDirectionForSingleTile();
            }
            return null;
        }

        // Check if all placements are in the same row or column
        List<Point> points = new ArrayList<>(temporaryPlacements.keySet());
        boolean sameRow = true;
        int firstRow = points.get(0).x;
        boolean sameColumn = true;
        int firstCol = points.get(0).y;

        for (Point p : points) {
            if (p.x != firstRow) {
                sameRow = false;
            }
            if (p.y != firstCol) {
                sameColumn = false;
            }
        }

        if (sameRow) {
            return Move.Direction.HORIZONTAL;
        }
        if (sameColumn) {
            return Move.Direction.VERTICAL;
        }

        return null;
    }

    private Move.Direction determineDirectionForSingleTile() {
        Point p = temporaryPlacements.keySet().iterator().next();
        int row = p.x;
        int col = p.y;
        Board board = game.getBoard();

        // Check for adjacent tiles in each direction
        boolean hasHorizontalAdjacent = (col > 0 && board.getSquare(row, col - 1).hasTile()) ||
                (col < Board.SIZE - 1 && board.getSquare(row, col + 1).hasTile());
        boolean hasVerticalAdjacent = (row > 0 && board.getSquare(row - 1, col).hasTile()) ||
                (row < Board.SIZE - 1 && board.getSquare(row + 1, col).hasTile());

        // If only one direction has adjacents, use that
        if (hasHorizontalAdjacent && !hasVerticalAdjacent) {
            return Move.Direction.HORIZONTAL;
        }
        if (hasVerticalAdjacent && !hasHorizontalAdjacent) {
            return Move.Direction.VERTICAL;
        }

        // If both have adjacents, determine based on which forms longer words
        List<Square> horizontalWord = board.getHorizontalWord(row, col);
        List<Square> verticalWord = board.getVerticalWord(row, col);

        if (horizontalWord.size() > verticalWord.size()) {
            return Move.Direction.HORIZONTAL;
        } else if (verticalWord.size() > horizontalWord.size()) {
            return Move.Direction.VERTICAL;
        }

        // Default to null if can't determine
        return null;
    }

    public boolean commitPlacement() {
        if (temporaryPlacements.isEmpty()) {
            return false;
        }

        // Determine direction
        Move.Direction direction = determineDirection();
        if (direction == null) {
            direction = Move.Direction.HORIZONTAL; // Default to horizontal if can't determine
        }

        // Find start position
        int startRow = Integer.MAX_VALUE;
        int startCol = Integer.MAX_VALUE;
        for (Point p : temporaryPlacements.keySet()) {
            startRow = Math.min(startRow, p.x);
            startCol = Math.min(startCol, p.y);
        }

        // For first move, must include center
        if (game.getBoard().isEmpty()) {
            boolean includesCenter = false;
            for (Point p : temporaryPlacements.keySet()) {
                if (p.x == GameConstants.CENTER_SQUARE && p.y == GameConstants.CENTER_SQUARE) {
                    includesCenter = true;
                    break;
                }
            }
            if (!includesCenter) {
                return false;
            }
        }

        // Create the move
        Move placeMove = Move.createPlaceMove(game.getCurrentPlayer(), startRow, startCol, direction);
        List<Tile> tilesToPlace = getTilesInOrder(direction, startRow, startCol);
        placeMove.addTiles(tilesToPlace);

        // Execute the move
        boolean success = game.executeMove(placeMove);

        if (success) {
            clearTemporaryPlacements();
        }

        return success;
    }

    private List<Tile> getTilesInOrder(Move.Direction direction, int startRow, int startCol) {
        List<Tile> orderedTiles = new ArrayList<>();

        if (direction == Move.Direction.HORIZONTAL) {
            Map<Integer, Tile> colToTile = new TreeMap<>();
            for (Map.Entry<Point, Tile> entry : temporaryPlacements.entrySet()) {
                Point p = entry.getKey();
                if (p.x == startRow) {
                    colToTile.put(p.y, entry.getValue());
                }
            }
            orderedTiles.addAll(colToTile.values());
        } else {
            Map<Integer, Tile> rowToTile = new TreeMap<>();
            for (Map.Entry<Point, Tile> entry : temporaryPlacements.entrySet()) {
                Point p = entry.getKey();
                if (p.y == startCol) {
                    rowToTile.put(p.x, entry.getValue());
                }
            }
            orderedTiles.addAll(rowToTile.values());
        }

        return orderedTiles;
    }

    public void cancelPlacements() {
        clearTemporaryPlacements();
    }

    private void clearTemporaryPlacements() {
        temporaryPlacements.clear();
        temporaryIndices.clear();
    }

    public boolean exchangeTiles(List<Tile> selectedTiles) {
        if (selectedTiles.isEmpty()) {
            return false;
        }

        if (game.getTileBag().getTileCount() < 1) {
            return false;
        }

        if (!temporaryPlacements.isEmpty()) {
            return false;
        }

        Move exchangeMove = Move.createExchangeMove(game.getCurrentPlayer(), selectedTiles);
        return game.executeMove(exchangeMove);
    }

    public boolean passTurn() {
        if (!temporaryPlacements.isEmpty()) {
            return false;
        }

        Move passMove = Move.createPassMove(game.getCurrentPlayer());
        return game.executeMove(passMove);
    }

    public Map<Point, Tile> getTemporaryPlacements() {
        return new HashMap<>(temporaryPlacements);
    }

    /**
     * Gets the indices of tiles in the rack that are currently placed temporarily on the board.
     * This method is needed for the hint feature to avoid selecting tiles that are already in use.
     *
     * @return A list of rack indices
     */
    public List<Integer> getTemporaryIndices() {
        return new ArrayList<>(temporaryIndices);
    }
}