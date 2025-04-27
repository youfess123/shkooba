package utilities;


import model.Board;
import model.Move;
import model.Square;
import model.Tile;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BoardUtils {

    private BoardUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static Board copyBoard(Board originalBoard) {
        return originalBoard.copy();
    }

    public static boolean hasAdjacentTile(Board board, int row, int col) {
        return board.hasAdjacentTile(row, col);
    }

    public static boolean hasDiagonalAdjacentTile(Board board, int row, int col) {
        if (row > 0 && col > 0 && board.getSquare(row - 1, col - 1).hasTile()) return true;
        if (row > 0 && col < Board.SIZE - 1 && board.getSquare(row - 1, col + 1).hasTile()) return true;
        if (row < Board.SIZE - 1 && col > 0 && board.getSquare(row + 1, col - 1).hasTile()) return true;
        if (row < Board.SIZE - 1 && col < Board.SIZE - 1 && board.getSquare(row + 1, col + 1).hasTile()) return true;
        return false;
    }

    public static List<Point> findAnchorPoints(Board board) {
        List<Point> anchors = new ArrayList<>();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                if (board.getSquare(row, col).hasTile()) {
                    continue;
                }
                if (hasAdjacentTile(board, row, col)) {
                    anchors.add(new Point(row, col));
                }
            }
        }
        return anchors;
    }

    public static String[] getPartialWordsAt(Board board, int row, int col, Move.Direction direction) {
        StringBuilder prefix = new StringBuilder();
        StringBuilder suffix = new StringBuilder();

        if (direction == Move.Direction.HORIZONTAL) {
            // Get prefix (letters to the left)
            int c = col - 1;
            while (c >= 0 && board.getSquare(row, c).hasTile()) {
                prefix.insert(0, board.getSquare(row, c).getTile().getLetter());
                c--;
            }

            // Get suffix (letters to the right)
            c = col + 1;
            while (c < Board.SIZE && board.getSquare(row, c).hasTile()) {
                suffix.append(board.getSquare(row, c).getTile().getLetter());
                c++;
            }
        } else {
            // Get prefix (letters above)
            int r = row - 1;
            while (r >= 0 && board.getSquare(r, col).hasTile()) {
                prefix.insert(0, board.getSquare(r, col).getTile().getLetter());
                r--;
            }

            // Get suffix (letters below)
            r = row + 1;
            while (r < Board.SIZE && board.getSquare(r, col).hasTile()) {
                suffix.append(board.getSquare(r, col).getTile().getLetter());
                r++;
            }
        }

        return new String[] { prefix.toString(), suffix.toString() };
    }

    public static String getWordAt(Board board, int row, int col, Move.Direction direction) {
        StringBuilder word = new StringBuilder();
        int currentRow = row;
        int currentCol = col;

        while (currentRow < Board.SIZE && currentCol < Board.SIZE) {
            Square square = board.getSquare(currentRow, currentCol);
            if (!square.hasTile()) {
                break;
            }
            word.append(square.getTile().getLetter());
            if (direction == Move.Direction.HORIZONTAL) {
                currentCol++;
            } else {
                currentRow++;
            }
        }
        return word.toString();
    }

    public static int findWordStart(Board board, int row, int col, boolean isHorizontal) {
        int position = isHorizontal ? col : row;
        while (position > 0) {
            int prevPos = position - 1;
            Square square = isHorizontal ?
                    board.getSquare(row, prevPos) :
                    board.getSquare(prevPos, col);
            if (!square.hasTile()) {
                break;
            }
            position = prevPos;
        }
        return position;
    }

    public static List<Square> getWordSquares(Board board, int row, int col, Move.Direction direction) {
        List<Square> squares = new ArrayList<>();
        int currentRow = row;
        int currentCol = col;

        while (currentRow < Board.SIZE && currentCol < Board.SIZE) {
            Square square = board.getSquare(currentRow, currentCol);
            if (!square.hasTile()) {
                break;
            }

            squares.add(square);

            if (direction == Move.Direction.HORIZONTAL) {
                currentCol++;
            } else {
                currentRow++;
            }
        }

        return squares;
    }

    public static boolean touchesCenterSquare(Move move) {
        int startRow = move.getStartRow();
        int startCol = move.getStartCol();
        Move.Direction direction = move.getDirection();
        List<Tile> tiles = move.getTiles();

        if (direction == Move.Direction.HORIZONTAL) {
            if (startRow == GameConstants.CENTER_SQUARE) {
                int endCol = startCol + tiles.size() - 1;
                return startCol <= GameConstants.CENTER_SQUARE &&
                        endCol >= GameConstants.CENTER_SQUARE;
            }
        } else {
            if (startCol == GameConstants.CENTER_SQUARE) {
                int endRow = startRow + tiles.size() - 1;
                return startRow <= GameConstants.CENTER_SQUARE &&
                        endRow >= GameConstants.CENTER_SQUARE;
            }
        }

        return false;
    }

    public static List<Tile> getTilesAtPositions(Board board, Set<Point> positions) {
        List<Tile> tiles = new ArrayList<>();

        for (Point p : positions) {
            if (p.x >= 0 && p.x < Board.SIZE && p.y >= 0 && p.y < Board.SIZE) {
                Square square = board.getSquare(p.x, p.y);
                if (square.hasTile()) {
                    tiles.add(square.getTile());
                }
            }
        }

        return tiles;
    }

    public static Set<Point> getTilePositionsForWord(Board board, int startRow, int startCol,
                                                     String word, Move.Direction direction) {
        Set<Point> positions = new HashSet<>();
        int row = startRow;
        int col = startCol;

        for (int i = 0; i < word.length(); i++) {
            if (row < Board.SIZE && col < Board.SIZE) {
                positions.add(new Point(row, col));

                if (direction == Move.Direction.HORIZONTAL) {
                    col++;
                } else {
                    row++;
                }
            }
        }

        return positions;
    }

    public static String squaresToString(List<Square> squares) {
        StringBuilder word = new StringBuilder();
        for (Square square : squares) {
            if (square.hasTile()) {
                word.append(square.getTile().getLetter());
            }
        }
        return word.toString();
    }
}