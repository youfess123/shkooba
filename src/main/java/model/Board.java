package model;

import utilities.GameConstants;
import java.util.ArrayList;
import java.util.List;

public class Board {
    public static final int SIZE = GameConstants.BOARD_SIZE;
    private final Square[][] squares;

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

    public static Board copyBoard(Board originalBoard) {
        return originalBoard.copy();
    }


    public static boolean hasAdjacentTile(Board board, int row, int col) {
        return board.hasAdjacentTile(row, col);
    }

    public List<Square> getAdjacentOccupiedSquares(int row, int col) {
        List<Square> adjacent = new ArrayList<>();

        // Check above
        if (row > 0 && getSquare(row - 1, col).hasTile()) {
            adjacent.add(getSquare(row - 1, col));
        }

        // Check below
        if (row < SIZE - 1 && getSquare(row + 1, col).hasTile()) {
            adjacent.add(getSquare(row + 1, col));
        }

        // Check left
        if (col > 0 && getSquare(row, col - 1).hasTile()) {
            adjacent.add(getSquare(row, col - 1));
        }

        // Check right
        if (col < SIZE - 1 && getSquare(row, col + 1).hasTile()) {
            adjacent.add(getSquare(row, col + 1));
        }

        return adjacent;
    }

    public boolean hasAdjacentTile(int row, int col) {
        return !getAdjacentOccupiedSquares(row, col).isEmpty();
    }

    public List<Square> getHorizontalWord(int row, int col) {
        List<Square> word = new ArrayList<>();

        if (!getSquare(row, col).hasTile()) {
            return word;
        }

        // Find the starting column of the word
        int startCol = col;
        while (startCol > 0 && getSquare(row, startCol - 1).hasTile()) {
            startCol--;
        }

        // Collect all tiles in the word
        int currentCol = startCol;
        while (currentCol < SIZE && getSquare(row, currentCol).hasTile()) {
            word.add(getSquare(row, currentCol));
            currentCol++;
        }

        // Only return words with at least 2 letters
        return word.size() >= 2 ? word : new ArrayList<>();
    }

    public List<Square> getVerticalWord(int row, int col) {
        List<Square> word = new ArrayList<>();

        if (!getSquare(row, col).hasTile()) {
            return word;
        }

        // Find the starting row of the word
        int startRow = row;
        while (startRow > 0 && getSquare(startRow - 1, col).hasTile()) {
            startRow--;
        }

        // Collect all tiles in the word
        int currentRow = startRow;
        while (currentRow < SIZE && getSquare(currentRow, col).hasTile()) {
            word.add(getSquare(currentRow, col));
            currentRow++;
        }

        // Only return words with at least 2 letters
        return word.size() >= 2 ? word : new ArrayList<>();
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Column headers
        sb.append("   ");
        for (int col = 0; col < SIZE; col++) {
            sb.append(String.format("%2d ", col + 1));
        }
        sb.append("\n");

        // Rows
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