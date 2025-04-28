package model;

import utilities.GameConstants;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Dictionary for Scrabble word validation and lookup.
 * Uses a GADDAG data structure for efficient word finding.
 */
public class Dictionary {
    private final Gaddag gaddag;
    private final Set<String> wordSet;
    private final String dictionaryName;

    /**
     * Creates a dictionary from an input stream.
     *
     * @param inputStream The input stream
     * @param name The dictionary name
     * @throws IOException If the stream cannot be read
     */
    public Dictionary(InputStream inputStream, String name) throws IOException {
        this.gaddag = new Gaddag();
        this.wordSet = new HashSet<>();
        this.dictionaryName = name;
        loadFromInputStream(inputStream);
        System.out.println("Loaded dictionary '" + dictionaryName + "' with " + wordSet.size() + " words");
    }

    /**
     * Loads the default dictionary.
     *
     * @return The input stream for the default dictionary
     * @throws IOException If the dictionary cannot be loaded
     */
    public static InputStream loadDefaultDictionary() throws IOException {
        return loadFile(GameConstants.DEFAULT_DICTIONARY);
    }

    /**
     * Loads a file from a path.
     *
     * @param path The file path
     * @return The input stream for the file
     * @throws IOException If the file cannot be loaded
     */
    public static InputStream loadFile(String path) throws IOException {
        InputStream is = Dictionary.class.getResourceAsStream(path);

        // If not found as a resource, try as an external file
        if (is == null) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                is = new FileInputStream(file);
            } else {
                throw new IOException("File not found: " + path);
            }
        }

        return is;
    }

    /**
     * Loads words from an input stream.
     *
     * @param inputStream The input stream
     * @throws IOException If the stream cannot be read
     */
    private void loadFromInputStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                addWord(line);
            }
        }
    }

    /**
     * Adds a word to the dictionary.
     *
     * @param word The word to add
     */
    public void addWord(String word) {
        word = word.trim().toUpperCase();
        if (word.isEmpty() || !word.matches("[A-Z]+")) {
            return;
        }
        wordSet.add(word);
        gaddag.insert(word);
    }

    /**
     * Checks if a word is valid.
     *
     * @param word The word to check
     * @return true if the word is valid, false otherwise
     */
    public boolean isValidWord(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        word = word.trim().toUpperCase();
        return wordSet.contains(word);
    }

    /**
     * Gets the dictionary name.
     *
     * @return The dictionary name
     */
    public String getName() {
        return dictionaryName;
    }

    /**
     * Gets the GADDAG data structure for efficient word finding.
     *
     * @return The GADDAG
     */
    public Gaddag getGaddag() {
        return gaddag;
    }

    /**
     * Validates if a word placement is valid using GADDAG.
     *
     * @param board The game board
     * @param move The move to validate
     * @return true if valid, false otherwise
     */
    public boolean validateWordPlacement(Board board, Move move) {
        return gaddag.validateWordPlacement(board, move);
    }

    /**
     * Finds all valid words that can be formed using the given rack
     * with the given constraints.
     *
     * @param rack The rack letters
     * @param boardConstraints The constraints imposed by the board
     * @return A set of valid words
     */
    public Set<String> findValidWords(String rack, String boardConstraints) {
        // This is a placeholder - implementation would depend on the specific needs
        // and would leverage the GADDAG
        return new HashSet<>();
    }
}