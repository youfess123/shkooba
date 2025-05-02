package model;

import utilities.GameConstants;
import java.util.ArrayList;
import java.util.List;

public class Board {
    public static final int SIZE = GameConstants.BOARD_SIZE;
    private final Square[][] squares;

    // Initialization
    public Board() {
        squares = new Square[SIZE][SIZE];
        initializeBoard();
    }

    private void initializeBoard() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                squares[row][col] = new Square(row, col, getSquareTypeFor(row, col));
            }
        }
    }

    private Square.SquareType getSquareTypeFor(int row, int col) {
        if (row == GameConstants.CENTER_SQUARE && col == GameConstants.CENTER_SQUARE) {
            return Square.SquareType.CENTER;
        }

        if ((row == 0 || row == 14) && (col == 0 || col == 7 || col == 14) ||
                (row == 7 && (col == 0 || col == 14))) {
            return Square.SquareType.TRIPLE_WORD;
        }

        if (row == col || row + col == 14) {
            if (row > 0 && row < 14 && col > 0 && col < 14) {
                return Square.SquareType.DOUBLE_WORD;
            }
        }

        if ((row == 1 || row == 13) && (col == 5 || col == 9) ||
                (row == 5 || row == 9) && (col == 1 || col == 5 || col == 9 || col == 13)) {
            return Square.SquareType.TRIPLE_LETTER;
        }

        if ((row == 0 || row == 14) && (col == 3 || col == 11) ||
                (row == 2 || row == 12) && (col == 6 || col == 8) ||
                (row == 3 || row == 11) && (col == 0 || col == 7 || col == 14) ||
                (row == 6 || row == 8) && (col == 2 || col == 6 || col == 8 || col == 12) ||
                (row == 7) && (col == 3 || col == 11)) {
            return Square.SquareType.DOUBLE_LETTER;
        }

        return Square.SquareType.NORMAL;
    }

    // Board state accessors and modifiers
    public Square getSquare(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            throw new IndexOutOfBoundsException("Invalid position: (" + row + ", " + col + ")");
        }
        return squares[row][col];
    }

    public void placeTile(int row, int col, Tile tile) {
        Square square = getSquare(row, col);
        if (!square.hasTile()) {
            square.setTile(tile);
        }
    }

    public boolean isEmpty() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (squares[row][col].hasTile()) {
                    return false;
                }
            }
        }
        return true;
    }

    // Word finding methods
    public List<Square> getHorizontalWord(int row, int col) {
        List<Square> word = new ArrayList<>();

        if (!getSquare(row, col).hasTile()) {
            return word;
        }

        int startCol = col;
        while (startCol > 0 && getSquare(row, startCol - 1).hasTile()) {
            startCol--;
        }

        int currentCol = startCol;
        while (currentCol < SIZE && getSquare(row, currentCol).hasTile()) {
            word.add(getSquare(row, currentCol));
            currentCol++;
        }

        return word.size() >= 2 ? word : new ArrayList<>();
    }

    public List<Square> getVerticalWord(int row, int col) {
        List<Square> word = new ArrayList<>();

        if (!getSquare(row, col).hasTile()) {
            return word;
        }

        int startRow = row;
        while (startRow > 0 && getSquare(startRow - 1, col).hasTile()) {
            startRow--;
        }

        int currentRow = startRow;
        while (currentRow < SIZE && getSquare(currentRow, col).hasTile()) {
            word.add(getSquare(currentRow, col));
            currentRow++;
        }

        return word.size() >= 2 ? word : new ArrayList<>();
    }

    // Adjacency checking methods
    public List<Square> getAdjacentOccupiedSquares(int row, int col) {
        List<Square> adjacent = new ArrayList<>();

        if (row > 0 && getSquare(row - 1, col).hasTile()) {
            adjacent.add(getSquare(row - 1, col));
        }

        if (row < SIZE - 1 && getSquare(row + 1, col).hasTile()) {
            adjacent.add(getSquare(row + 1, col));
        }

        if (col > 0 && getSquare(row, col - 1).hasTile()) {
            adjacent.add(getSquare(row, col - 1));
        }

        if (col < SIZE - 1 && getSquare(row, col + 1).hasTile()) {
            adjacent.add(getSquare(row, col + 1));
        }

        return adjacent;
    }

    public boolean hasAdjacentTile(int row, int col) {
        return !getAdjacentOccupiedSquares(row, col).isEmpty();
    }

    public static boolean hasAdjacentTile(Board board, int row, int col) {
        return board.hasAdjacentTile(row, col);
    }

    // Utility methods
    public Board copy() {
        Board copy = new Board();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Square square = getSquare(r, c);
                if (square.hasTile()) {
                    copy.placeTile(r, c, square.getTile());
                }
            }
        }
        return copy;
    }

    public static Board copyBoard(Board originalBoard) {
        return originalBoard.copy();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("   ");
        for (int col = 0; col < SIZE; col++) {
            sb.append(String.format("%2d ", col + 1));
        }
        sb.append("\n");

        for (int row = 0; row < SIZE; row++) {
            sb.append(String.format("%2d ", row + 1));
            for (int col = 0; col < SIZE; col++) {
                Square square = squares[row][col];
                if (square.hasTile()) {
                    sb.append(" ").append(square.getTile().getLetter()).append(" ");
                } else {
                    String label = square.getSquareType().getLabel();
                    sb.append(" ").append(label.isEmpty() ? "·" : label).append(" ");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}