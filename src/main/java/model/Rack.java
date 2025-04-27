package model;


import utilities.GameConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class Rack {
    private static final Logger logger = Logger.getLogger(Rack.class.getName());
    private final List<Tile> tiles;

    public Rack() {
        this.tiles = new ArrayList<>(GameConstants.RACK_CAPACITY);
    }

    public boolean addTile(Tile tile) {
        if (tile == null) {
            logger.warning("Attempted to add null tile to rack");
            return false;
        }

        if (tiles.size() >= GameConstants.RACK_CAPACITY) {
            logger.warning("Cannot add tile, rack is full");
            return false;
        }

        return tiles.add(tile);
    }

    public int addTiles(List<Tile> tilesToAdd) {
        if (tilesToAdd == null || tilesToAdd.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Tile tile : tilesToAdd) {
            if (tile == null) continue;
            if (addTile(tile)) count++;
            else break;
        }

        return count;
    }

    public boolean removeTile(Tile tile) {
        if (tile == null) {
            return false;
        }
        return tiles.remove(tile);
    }

    public boolean removeTiles(List<Tile> tilesToRemove) {
        if (tilesToRemove == null || tilesToRemove.isEmpty()) {
            return false;
        }

        List<Tile> rackCopy = new ArrayList<>(tiles);
        int removedCount = 0;

        for (Tile tileToRemove : tilesToRemove) {
            boolean found = false;
            for (Tile rackTile : rackCopy) {
                if (tilesMatch(rackTile, tileToRemove)) {
                    rackCopy.remove(rackTile);
                    removedCount++;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        if (removedCount == tilesToRemove.size()) {
            tiles.clear();
            tiles.addAll(rackCopy);
            return true;
        }

        return false;
    }

    private boolean tilesMatch(Tile a, Tile b) {
        return a.getLetter() == b.getLetter() &&
                a.getValue() == b.getValue() &&
                a.isBlank() == b.isBlank();
    }

    public Tile getTile(int index) {
        if (index < 0 || index >= tiles.size()) {
            return null;
        }
        return tiles.get(index);
    }

    public List<Tile> getTiles() {
        return Collections.unmodifiableList(tiles);
    }

    public int size() {
        return tiles.size();
    }

    public boolean isEmpty() {
        return tiles.isEmpty();
    }

    public boolean isFull() {
        return tiles.size() >= GameConstants.RACK_CAPACITY;
    }

    public int getEmptySlots() {
        return GameConstants.RACK_CAPACITY - tiles.size();
    }

    public void shuffle() {
        Collections.shuffle(tiles);
    }

    public int getTotalValue() {
        int total = 0;
        for (Tile tile : tiles) {
            total += tile.getValue();
        }
        return total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Rack: ");
        for (Tile tile : tiles) {
            sb.append(tile.getLetter());
            if (tile.isBlank()) {
                sb.append("(*)");
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }
}