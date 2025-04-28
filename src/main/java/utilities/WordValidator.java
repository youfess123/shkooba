package utilities;


import model.Dictionary;
import model.Board;
import model.Move;

import java.awt.Point;
import java.util.*;

public final class WordValidator {

    private WordValidator() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static List<String> validateWords(Board board, Move move, List<Point> newTilePositions, Dictionary dictionary) {
        // Use the GADDAG for word validation
        return dictionary.getGaddag().validateWords(board, move, newTilePositions);
    }

    public static boolean isValidPlaceMove(Move move, Board board, Dictionary dictionary) {
        // Use the GADDAG for move validation
        return dictionary.getGaddag().validateWordPlacement(board, move);
    }


}