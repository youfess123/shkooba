package model;

import utilities.GameConstants;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class Dictionary {
    private final Gaddag gaddag;
    private final Set<String> wordSet;
    private final String dictionaryName;

    public Dictionary(InputStream inputStream, String name) throws IOException {
        this.gaddag = new Gaddag();
        this.wordSet = new HashSet<>();
        this.dictionaryName = name;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim().toUpperCase();
                if (!line.isEmpty()) {
                    wordSet.add(line);
                    gaddag.insert(line);
                }
            }
        }

        System.out.println("Loaded dictionary '" + dictionaryName + "' with " + wordSet.size() + " words");
    }

    public static InputStream loadDefaultDictionary() throws IOException {
        return loadFile(GameConstants.DEFAULT_DICTIONARY);
    }

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

    public boolean isValidWord(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        word = word.trim().toUpperCase();
        return wordSet.contains(word);
    }

    public String getName() {
        return dictionaryName;
    }

    public Gaddag getGaddag() {
        return gaddag;
    }

}