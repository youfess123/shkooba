package model;


import utilities.GameConstants;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class Dictionary {
    private static final Logger logger = Logger.getLogger(Dictionary.class.getName());

    private final Set<String> validWords;
    private final String dictionaryName;
    private final Map<String, Integer> wordScoreCache;

    public Dictionary(String filePath) throws IOException {
        this.validWords = new HashSet<>();
        this.wordScoreCache = new HashMap<>();
        this.dictionaryName = extractDictionaryName(filePath);
        loadFromFile(filePath);
        logger.info("Loaded dictionary '" + dictionaryName + "' with " + validWords.size() + " words");
    }

    public Dictionary(InputStream inputStream, String name) throws IOException {
        this.validWords = new HashSet<>();
        this.wordScoreCache = new HashMap<>();
        this.dictionaryName = name;
        loadFromInputStream(inputStream);
        logger.info("Loaded dictionary '" + dictionaryName + "' with " + validWords.size() + " words");
    }

    public static InputStream loadDefaultDictionary() throws IOException {
        return loadFile(GameConstants.DEFAULT_DICTIONARY);
    }

    public static InputStream loadFile(String path) throws IOException {
        InputStream is = Dictionary.class.getResourceAsStream(path);

        if (is == null) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                is = new FileInputStream(file);
            } else {
                throw new IOException("Dictionary file not found: " + path);
            }
        }

        return is;
    }

    private String extractDictionaryName(String filePath) {
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash == -1) {
            lastSlash = filePath.lastIndexOf('\\');
        }

        String filename = lastSlash == -1 ? filePath : filePath.substring(lastSlash + 1);
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? filename : filename.substring(0, lastDot);
    }

    private void loadFromFile(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                addWord(line);
            }
        }
    }

    private void loadFromInputStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                addWord(line);
            }
        }
    }

    public void addWord(String word) {
        word = word.trim().toUpperCase();
        if (word.isEmpty() || !word.matches("[A-Z]+")) {
            return;
        }
        validWords.add(word);
    }

    public boolean isValidWord(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }

        word = word.trim().toUpperCase();
        return validWords.contains(word);
    }

    public String getName() {
        return dictionaryName;
    }

    public int getWordCount() {
        return validWords.size();
    }

    public void cacheWordScore(String word, int score) {
        wordScoreCache.put(word.toUpperCase(), score);
    }

    public Integer getCachedWordScore(String word) {
        return wordScoreCache.get(word.toUpperCase());
    }
}