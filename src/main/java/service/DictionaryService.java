package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DictionaryService {
    private static final Logger logger = Logger.getLogger(DictionaryService.class.getName());

    private static final String API_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/";

    private final Map<String, List<WordDefinition>> definitionCache;

    private final HttpClient httpClient;

    public static class WordDefinition {
        private final String word;
        private final String partOfSpeech;
        private final String definition;
        private final List<String> examples;

        public WordDefinition(String word, String partOfSpeech, String definition) {
            this.word = word;
            this.partOfSpeech = partOfSpeech;
            this.definition = definition;
            this.examples = new ArrayList<>();
        }

        public WordDefinition(String word, String partOfSpeech, String definition, List<String> examples) {
            this.word = word;
            this.partOfSpeech = partOfSpeech;
            this.definition = definition;
            this.examples = examples;
        }

        public String getWord() {
            return word;
        }

        public String getPartOfSpeech() {
            return partOfSpeech;
        }

        public String getDefinition() {
            return definition;
        }

        public List<String> getExamples() {
            return examples;
        }

        public boolean hasExamples() {
            return !examples.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(word).append(" (").append(partOfSpeech).append("): ");
            sb.append(definition);

            if (!examples.isEmpty()) {
                sb.append("\nExample: ").append(examples.get(0));
            }

            return sb.toString();
        }
    }

    /**
     * Creates a new DictionaryService.
     */
    public DictionaryService() {
        this.definitionCache = new HashMap<>();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Pre-loads definitions for common Scrabble words.
     */


    /**
     * Gets definitions for a word, either from cache or by making an API request.
     *
     * @param word The word to look up
     * @return A list of definitions for the word
     */
    public CompletableFuture<List<WordDefinition>> getDefinitions(String word) {
        final String finalWord = word.toLowerCase();

        // Check cache first
        if (definitionCache.containsKey(finalWord)) {
            return CompletableFuture.completedFuture(definitionCache.get(finalWord));
        }

        // Make API request
        return fetchDefinitionsFromAPI(finalWord)
                .handle((definitions, ex) -> {
                    if (ex != null) {
                        logger.log(Level.WARNING, "Error fetching definition for " + finalWord, ex);
                        return createFallbackDefinition(finalWord);
                    }
                    return definitions;
                });
    }

    /**
     * Fetches definitions from the dictionary API.
     *
     * @param word The word to look up
     * @return A CompletableFuture that will contain the list of definitions
     */
    private CompletableFuture<List<WordDefinition>> fetchDefinitionsFromAPI(String word) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(API_URL + word))
                .header("Accept", "application/json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return parseDefinitionResponse(word, response.body());
                    } else {
                        logger.warning("API returned status code: " + response.statusCode());
                        return createFallbackDefinition(word);
                    }
                })
                .exceptionally(ex -> {
                    logger.log(Level.WARNING, "Exception during API call", ex);
                    return createFallbackDefinition(word);
                });
    }

    /**
     * Parses the JSON response from the dictionary API.
     *
     * @param word The word that was looked up
     * @param jsonResponse The JSON response from the API
     * @return A list of word definitions
     */
    private List<WordDefinition> parseDefinitionResponse(String word, String jsonResponse) {
        List<WordDefinition> definitions = new ArrayList<>();

        try {
            JSONArray responseArray = new JSONArray(jsonResponse);

            if (responseArray.length() > 0) {
                JSONObject entry = responseArray.getJSONObject(0);
                JSONArray meanings = entry.getJSONArray("meanings");

                for (int i = 0; i < meanings.length(); i++) {
                    JSONObject meaning = meanings.getJSONObject(i);
                    String partOfSpeech = meaning.getString("partOfSpeech");

                    JSONArray definitionsArray = meaning.getJSONArray("definitions");
                    for (int j = 0; j < Math.min(2, definitionsArray.length()); j++) {
                        JSONObject definitionObj = definitionsArray.getJSONObject(j);
                        String definition = definitionObj.getString("definition");

                        List<String> examples = new ArrayList<>();
                        if (definitionObj.has("example")) {
                            examples.add(definitionObj.getString("example"));
                        }

                        definitions.add(new WordDefinition(word, partOfSpeech, definition, examples));

                        // Limit to max 2 definitions per part of speech to avoid overwhelming the player
                        if (definitions.size() >= 4) {
                            break;
                        }
                    }

                    if (definitions.size() >= 4) {
                        break;
                    }
                }
            }

            // Cache the results
            if (!definitions.isEmpty()) {
                definitionCache.put(word, definitions);
            }

        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error parsing JSON response for word: " + word, e);
            definitions = createFallbackDefinition(word);
        }

        return definitions;
    }

    /**
     * Creates a fallback definition when the API call fails.
     *
     * @param word The word that was looked up
     * @return A list containing a single generic definition
     */
    private List<WordDefinition> createFallbackDefinition(String word) {
        List<WordDefinition> fallback = new ArrayList<>();
        fallback.add(new WordDefinition(
                word,
                "unknown",
                "Definition not available. This is a valid Scrabble word."
        ));
        return fallback;
    }
}