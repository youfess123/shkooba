package model;


public class Square {
    public enum SquareType {
        NORMAL(1, 1, ""),
        DOUBLE_LETTER(2, 1, "DL"),
        TRIPLE_LETTER(3, 1, "TL"),
        DOUBLE_WORD(1, 2, "DW"),
        TRIPLE_WORD(1, 3, "TW"),
        CENTER(1, 2, "★");

        private final int letterMultiplier;
        private final int wordMultiplier;
        private final String label;

        SquareType(int letterMultiplier, int wordMultiplier, String label) {
            this.letterMultiplier = letterMultiplier;
            this.wordMultiplier = wordMultiplier;
            this.label = label;
        }

        public int getLetterMultiplier() {
            return letterMultiplier;
        }

        public int getWordMultiplier() {
            return wordMultiplier;
        }

        public String getLabel() {
            return label;
        }
    }

    private final int row;
    private final int col;
    private final SquareType type;
    private Tile tile;
    private boolean premiumUsed;

    public Square(int row, int col, SquareType type) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.tile = null;
        this.premiumUsed = false;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public SquareType getSquareType() {
        return type;
    }

    public boolean hasTile() {
        return tile != null;
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        this.tile = tile;
    }

    public void usePremium() {
        this.premiumUsed = true;
    }

    public boolean isPremiumUsed() {
        return premiumUsed;
    }

    @Override
    public String toString() {
        if (hasTile()) {
            return tile.toString();
        } else {
            return type.getLabel().isEmpty() ? "·" : type.getLabel();
        }
    }
}