package model;

public class Tile {
    private final char letter;
    private  int value;
    private final boolean isBlank;

    public Tile(char letter, int value) {
        this.letter = Character.toUpperCase(letter);
        this.value = value;
        this.isBlank = (letter == '*');
    }

    public static Tile createBlankTile(char letter) {
        return new Tile(Character.toUpperCase(letter), 0) {
            @Override
            public boolean isBlank() {
                return true;
            }
        };
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }


    public char getLetter() {
        return letter;
    }

    public boolean isBlank() {
        return isBlank;
    }

    @Override
    public String toString() {
        return isBlank ? (letter + "(*)") : String.valueOf(letter);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tile other = (Tile) obj;
        if (isBlank && other.isBlank) {
            return true;
        }
        return letter == other.letter && value == other.value && isBlank == other.isBlank;
    }

    @Override
    public int hashCode() {
        int result = letter;
        result = 31 * result + value;
        result = 31 * result + (isBlank ? 1 : 0);
        return result;
    }
}