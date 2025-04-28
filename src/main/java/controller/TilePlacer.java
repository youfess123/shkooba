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

    public void selectTileFromRack(Player player, int index) {
        Rack rack = player.getRack();

        if (index < 0 || index >= rack.size()) {
            return;
        }

        Tile tile = rack.getTile(index);

        if (selectedPositions.contains(index)) {
            // Deselect the tile
            selectedTiles.remove(tile);
            selectedPositions.remove(Integer.valueOf(index));
            logger.fine("Deselected tile at index " + index + ": " + tile.getLetter());
        } else {
            // Select the tile
            selectedTiles.add(tile);
            selectedPositions.add(index);
            logger.fine("Selected tile at index " + index + ": " + tile.getLetter());
        }
    }

    public void clearSelectedTiles() {
        selectedTiles.clear();
        selectedPositions.clear();
    }

    public void setBlankTileLetter(Player player, int rackIndex, char letter) {
        Rack rack = player.getRack();

        if (rackIndex < 0 || rackIndex >= rack.size()) {
            return;
        }

        Tile currentTile = rack.getTile(rackIndex);
        if (!currentTile.isBlank()) {
            return; // Only allow changing blank tiles
        }

        // Replace the blank tile in the rack with the assigned letter
        rack.removeTile(currentTile);
        Tile blankTile = Tile.createBlankTile(letter);
        rack.addTile(blankTile);


        logger.fine("Set blank tile letter to " + letter + " at index " + rackIndex);

        // Update selection if this tile was selected
        if (selectedPositions.contains(rackIndex)) {
            int selectionIndex = selectedPositions.indexOf(rackIndex);
            selectedTiles.set(selectionIndex, blankTile);
        }
    }

    public boolean isTileSelected(int index) {
        return selectedPositions.contains(index);
    }



    public List<Tile> getSelectedTiles() {
        return new ArrayList<>(selectedTiles);
    }


}