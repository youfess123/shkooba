package model;

import utilities.GameConstants;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Dictionary {
    private final Gaddag gaddag;
    private final Set<String> wordSet;
    private final String dictionaryName;

    // Constructor and initialization
    public Dictionary(InputStream inputStream, String name) throws IOException {
        this.gaddag = new Gaddag();
        this.wordSet = new HashSet<>();
        this.dictionaryName = name;

        loadWords(inputStream);
        System.out.println("Loaded dictionary '" + dictionaryName + "' with " + wordSet.size() + " words");
    }

    private void loadWords(InputStream inputStream) throws IOException {
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
    }

    // Static loading methods
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
                throw new IOException("File not found: " + path);
            }
        }

        return is;
    }


    // Accessors
    public String getName() {
        return dictionaryName;
    }

    public Gaddag getGaddag() {
        return gaddag;
    }
}