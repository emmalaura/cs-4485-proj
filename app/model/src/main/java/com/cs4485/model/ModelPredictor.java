package com.cs4485.model;

import java.sql.SQLException;
import java.util.*;

/**
 * Main integration layer the UI can call for predictions.
 *
 * The API exposes two paths:
 *
 *   1. getNextWords() -- queries the word_transitions table directly using the
 *      pre-computed probability column. This is the fast path for autocomplete.
 *
 *   2. completeSentence() -- uses a BigramModel already loaded into memory to
 *      extend a partial sentence. This is the better fit for longer generation.
 *
 * Example usage for the UI team:
 *
 *   // Option 1: single next-word suggestions (no model in memory needed)
 *   try (DBInterface db = DBInterface.fromEnvironment()) {
 *       List<Prediction> suggestions = ModelPredictor.getNextWords(db, "algorithm", 5);
 *       suggestions.forEach(p -> System.out.println(p.word() + " (" + p.probability() + ")"));
 *   }
 *
 *   // Option 2: multi-word sentence completion
 *   try (DBInterface db = DBInterface.fromEnvironment()) {
 *       BigramModel model = new BigramModel(SmoothingMethod.KNESER_NEY);
 *       model.train(db.loadWords(), db.loadTransitions());
 *       List<String> result = ModelPredictor.completeSentence(model, "the algorithm", 10, GenerationStrategy.BEAM);
 *       System.out.println(String.join(" ", result));
 *   }
 */
public class ModelPredictor {

    // Utility class only; no instances needed.
    private ModelPredictor() {}

    /**
     * Returns the top N next-word suggestions for the given word.
     *
     * The input is normalized first so it lines up with the lowercase tokens
     * coming from the data-processing pipeline.
     *
     * @param db   an open DBInterface connection
     * @param word the word that the user just typed
     * @param topN the number of suggestions to return
     * @return predictions sorted by probability descending; empty if the word is unknown
     * @throws SQLException if the database query fails
     */
    public static List<Prediction> getNextWords(DBInterface db, String word, int topN)
            throws SQLException {
        String normalizedWord = normalizeToken(word);
        if (normalizedWord == null) {
            return Collections.emptyList();
        }
        return db.getPredictions(normalizedWord, topN);
    }

    /**
     * Completes a partial sentence using an already-trained in-memory model.
     *
     * The last input word is treated as the seed, generation starts from there,
     * and the original prefix is stitched back onto the generated continuation.
     *
     * @param model           a trained BigramModel
     * @param partialSentence the words already typed, as a space-separated string
     *                        (e.g., "the algorithm")
     * @param maxLength       the maximum total number of words in the returned sentence,
     *                        including the words from partialSentence
     * @param strategy        how to pick words at each generation step
     * @return the partial sentence extended with generated words;
     *         empty list if partialSentence is null or blank
     */
    public static List<String> completeSentence(BigramModel model,
                                                 String partialSentence,
                                                 int maxLength,
                                                 GenerationStrategy strategy) {
        String normalizedSentence = normalizeSentence(partialSentence);
        if (normalizedSentence == null) {
            return Collections.emptyList();
        }

        String[] inputWords = normalizedSentence.split("\\s+");
        String seedWord = inputWords[inputWords.length - 1];

        // The +1 keeps the seed word from getting counted twice in the total length.
        int targetLength = Math.max(1, maxLength - inputWords.length + 1);
        List<String> continuation = model.generateSentence(seedWord, targetLength, strategy);

        // Keep the original prefix, then append the generated continuation.
        List<String> result = new ArrayList<>();
        for (int i = 0; i < inputWords.length - 1; i++) {
            result.add(inputWords[i]);
        }
        result.addAll(continuation);
        return result;
    }

    private static String normalizeToken(String word) {
        if (word == null) {
            return null;
        }

        String normalized = word.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeSentence(String sentence) {
        if (sentence == null) {
            return null;
        }

        String normalized = sentence.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}

List<Prediction> preds = db.getPredictions("bigram", 10);
preds.forEach(p ->
        System.out.println(p.word() + " -> " + p.probability())
        );