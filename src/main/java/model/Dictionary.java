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
    private final Map<String, Integer> wordScoreCache;

    private static final String DEFAULT_DICTIONARY = "/dictionaries/Dictionary.txt";

    /**
     * Creates a dictionary from a file.
     *
     * @param filePath Path to the dictionary file
     * @throws IOException If the file cannot be read
     */
    public Dictionary(String filePath) throws IOException {
        this.gaddag = new Gaddag();
        this.wordSet = new HashSet<>();
        this.wordScoreCache = new HashMap<>();
        this.dictionaryName = extractDictionaryName(filePath);
        loadFromFile(filePath);
        System.out.println("Loaded dictionary '" + dictionaryName + "' with " + wordSet.size() + " words");
    }

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
        this.wordScoreCache = new HashMap<>();
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
     * Extracts the dictionary name from a file path.
     *
     * @param filePath The file path
     * @return The dictionary name
     */
    private String extractDictionaryName(String filePath) {
        String filename = filePath.substring(filePath.lastIndexOf('/') + 1);
        if (filename.contains(".")) {
            filename = filename.substring(0, filename.lastIndexOf('.'));
        }
        return filename;
    }

    /**
     * Loads words from a file.
     *
     * @param filePath The file path
     * @throws IOException If the file cannot be read
     */
    private void loadFromFile(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                addWord(line);
            }
        }
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
     * Caches a word score for future reference.
     *
     * @param word The word
     * @param score The score
     */
    public void cacheWordScore(String word, int score) {
        wordScoreCache.put(word.toUpperCase(), score);
    }

    /**
     * Gets a cached word score.
     *
     * @param word The word
     * @return The score, or null if not cached
     */
    public Integer getCachedWordScore(String word) {
        return wordScoreCache.get(word.toUpperCase());
    }

    /**
     * Gets the number of words in the dictionary.
     *
     * @return The word count
     */
    public int getWordCount() {
        return wordSet.size();
    }
}