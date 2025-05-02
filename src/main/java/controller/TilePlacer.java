package controller;

import model.Player;
import model.Rack;
import model.Tile;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TilePlacer {
    private static final Logger logger = Logger.getLogger(TilePlacer.class.getName());

    private final List<Tile> selectedTiles;
    private final List<Integer> selectedPositions;

    public TilePlacer() {
        this.selectedTiles = new ArrayList<>();
        this.selectedPositions = new ArrayList<>();
    }

    // Tile selection methods
    public void selectTileFromRack(Player player, int index) {
        Rack rack = player.getRack();

        if (index < 0 || index >= rack.size()) {
            return;
        }

        Tile tile = rack.getTile(index);

        if (selectedPositions.contains(index)) {
            selectedTiles.remove(tile);
            selectedPositions.remove(Integer.valueOf(index));
            logger.fine("Deselected tile at index " + index + ": " + tile.getLetter());
        } else {
            selectedTiles.add(tile);
            selectedPositions.add(index);
            logger.fine("Selected tile at index " + index + ": " + tile.getLetter());
        }
    }

    public void clearSelectedTiles() {
        selectedTiles.clear();
        selectedPositions.clear();
    }

    public boolean isTileSelected(int index) {
        return selectedPositions.contains(index);
    }

    // Blank tile handling
    public void setBlankTileLetter(Player player, int rackIndex, char letter) {
        Rack rack = player.getRack();

        if (rackIndex < 0 || rackIndex >= rack.size()) {
            return;
        }

        Tile currentTile = rack.getTile(rackIndex);
        if (!currentTile.isBlank()) {
            return;
        }

        rack.removeTile(currentTile);
        Tile blankTile = Tile.createBlankTile(letter);
        rack.addTile(blankTile);

        logger.fine("Set blank tile letter to " + letter + " at index " + rackIndex);

        if (selectedPositions.contains(rackIndex)) {
            int selectionIndex = selectedPositions.indexOf(rackIndex);
            selectedTiles.set(selectionIndex, blankTile);
        }
    }

    // Accessors
    public List<Tile> getSelectedTiles() {
        return new ArrayList<>(selectedTiles);
    }
}