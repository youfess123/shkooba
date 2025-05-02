package controller;

import model.*;
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

    // Temporary tile placement methods
    public boolean placeTileTemporarily(int rackIndex, int row, int col) {
        try {
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

    public void cancelPlacements() {
        clearTemporaryPlacements();
    }

    private void clearTemporaryPlacements() {
        temporaryPlacements.clear();
        temporaryIndices.clear();
    }

    // Placement validation methods
    public boolean isValidTemporaryPlacement(int row, int col) {
        Board board = game.getBoard();

        if (board.getSquare(row, col).hasTile() || hasTemporaryTileAt(row, col)) {
            return false;
        }

        if (board.isEmpty() && temporaryPlacements.isEmpty()) {
            return row == GameConstants.CENTER_SQUARE && col == GameConstants.CENTER_SQUARE;
        }

        if (!temporaryPlacements.isEmpty()) {
            return isValidDirectionalPlacement(row, col);
        }

        return Board.hasAdjacentTile(board, row, col);
    }

    private boolean isValidDirectionalPlacement(int row, int col) {
        List<Point> placementPoints = new ArrayList<>(temporaryPlacements.keySet());

        if (placementPoints.size() == 1) {
            Point existingPoint = placementPoints.get(0);
            return (row == existingPoint.x || col == existingPoint.y);
        }

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
            for (Point p : placementPoints) {
                if (p.x != row) {
                    return false;
                }
            }

            int minCol = Integer.MAX_VALUE;
            int maxCol = Integer.MIN_VALUE;
            for (Point p : placementPoints) {
                minCol = Math.min(minCol, p.y);
                maxCol = Math.max(maxCol, p.y);
            }

            if (col >= minCol - 1 && col <= maxCol + 1) {
                return true;
            }

            return (col > 0 && board.getSquare(row, col - 1).hasTile()) ||
                    (col < Board.SIZE - 1 && board.getSquare(row, col + 1).hasTile());
        } else {
            for (Point p : placementPoints) {
                if (p.y != col) {
                    return false;
                }
            }

            int minRow = Integer.MAX_VALUE;
            int maxRow = Integer.MIN_VALUE;
            for (Point p : placementPoints) {
                minRow = Math.min(minRow, p.x);
                maxRow = Math.max(maxRow, p.x);
            }

            if (row >= minRow - 1 && row <= maxRow + 1) {
                return true;
            }

            return (row > 0 && board.getSquare(row - 1, col).hasTile()) ||
                    (row < Board.SIZE - 1 && board.getSquare(row + 1, col).hasTile());
        }
    }

    // Direction determination methods
    public Move.Direction determineDirection() {
        if (temporaryPlacements.size() <= 1) {
            if (temporaryPlacements.size() == 1) {
                return determineDirectionForSingleTile();
            }
            return null;
        }

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

        boolean hasHorizontalAdjacent = (col > 0 && board.getSquare(row, col - 1).hasTile()) ||
                (col < Board.SIZE - 1 && board.getSquare(row, col + 1).hasTile());
        boolean hasVerticalAdjacent = (row > 0 && board.getSquare(row - 1, col).hasTile()) ||
                (row < Board.SIZE - 1 && board.getSquare(row + 1, col).hasTile());

        if (hasHorizontalAdjacent && !hasVerticalAdjacent) {
            return Move.Direction.HORIZONTAL;
        }
        if (hasVerticalAdjacent && !hasHorizontalAdjacent) {
            return Move.Direction.VERTICAL;
        }

        List<Square> horizontalWord = board.getHorizontalWord(row, col);
        List<Square> verticalWord = board.getVerticalWord(row, col);

        if (horizontalWord.size() > verticalWord.size()) {
            return Move.Direction.HORIZONTAL;
        } else if (verticalWord.size() > horizontalWord.size()) {
            return Move.Direction.VERTICAL;
        }

        return null;
    }

    // Move execution methods
    public boolean commitPlacement() {
        if (temporaryPlacements.isEmpty()) {
            return false;
        }

        Move.Direction direction = determineDirection();
        if (direction == null) {
            direction = Move.Direction.HORIZONTAL;
        }

        int startRow = Integer.MAX_VALUE;
        int startCol = Integer.MAX_VALUE;
        for (Point p : temporaryPlacements.keySet()) {
            startRow = Math.min(startRow, p.x);
            startCol = Math.min(startCol, p.y);
        }

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

        Move placeMove = Move.createPlaceMove(game.getCurrentPlayer(), startRow, startCol, direction);
        List<Tile> tilesToPlace = getTilesInOrder(direction, startRow, startCol);
        placeMove.addTiles(tilesToPlace);

        boolean success = game.executeMove(placeMove);

        if (success) {
            clearTemporaryPlacements();
        }

        return success;
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

    public Map<Point, Tile> getTemporaryPlacements() {
        return new HashMap<>(temporaryPlacements);
    }

}