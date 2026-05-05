package com.cs4485.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main bigram model used for autocomplete and sentence generation.
 *
 * The model is trained from the word and transition counts stored in the database,
 * then used to rank next-word predictions, generate continuations, and measure
 * perplexity on held-out transitions.
 */
public class BigramModel {

    // Count of how many times wordId2 followed wordId1 in the training data.
    private final Map<Integer, Map<Integer, Integer>> bigramCounts;

    // Total number of times each word appeared in the corpus.
    private final Map<Integer, Integer> unigramCounts;

    // Lookup maps for moving between the DB IDs and the actual word strings.
    private final Map<Integer, String> idToWord;
    private final Map<String, Integer> wordToId;

    // For Kneser-Ney, this tracks how many different words can lead into each word.
    private final Map<Integer, Integer> continuationCounts;

    private int vocabularySize;
    private int totalBigrams;

    private final SmoothingMethod smoothingMethod;
    private final double k;        // add-k smoothing parameter; 1.0 gives standard Laplace smoothing
    private final double discount; // Kneser-Ney discount D; 0.75 is a standard default

    // Keeps the last prediction latency around so the UI can display it if needed.
    private long lastPredictionLatencyMs;

    /**
     * Creates a model with the given smoothing method and the default parameters
     * used for add-k and Kneser-Ney.
     *
     * @param smoothingMethod the algorithm used to estimate probabilities for unseen bigrams
     */
    public BigramModel(SmoothingMethod smoothingMethod) {
        this(smoothingMethod, 1.0, 0.75);
    }

    /**
     * Creates a model with explicit smoothing parameters.
     *
     * @param smoothingMethod the algorithm used to estimate probabilities for unseen bigrams
     * @param k               add-k smoothing parameter (only used with ADD_K smoothing)
     * @param discount        Kneser-Ney discount value D, typically between 0 and 1
     *                        (only used with KNESER_NEY smoothing)
     */
    public BigramModel(SmoothingMethod smoothingMethod, double k, double discount) {
        this.smoothingMethod = smoothingMethod;
        this.k = k;
        this.discount = discount;
        this.bigramCounts = new HashMap<>();
        this.unigramCounts = new HashMap<>();
        this.idToWord = new HashMap<>();
        this.wordToId = new HashMap<>();
        this.continuationCounts = new HashMap<>();
    }

    /**
     * Trains the model from the rows loaded out of the database.
     *
     * Any previous state is cleared first, then all of the count maps are rebuilt
     * from the latest word and transition rows.
     *
     * @param wordRows       all rows from the words table
     * @param transitionRows all rows from the word_transitions table
     */
    public void train(List<WordRow> wordRows, List<TransitionRow> transitionRows) {
        bigramCounts.clear();
        unigramCounts.clear();
        idToWord.clear();
        wordToId.clear();
        continuationCounts.clear();
        totalBigrams = 0;

        // Build the lookup maps and unigram counts from the words table.
        for (WordRow row : wordRows) {
            idToWord.put(row.wordId(), row.word());
            wordToId.put(row.word(), row.wordId());
            unigramCounts.put(row.wordId(), row.totalOccurrence());
        }
        vocabularySize = wordRows.size();

        // Build the bigram counts and continuation counts from the transitions table.
        for (TransitionRow row : transitionRows) {
            bigramCounts
                .computeIfAbsent(row.firstWordId(), ignored -> new HashMap<>())
                .put(row.secondWordId(), row.count());

            // Track how many different words can lead into secondWordId.
            continuationCounts.merge(row.secondWordId(), 1, Integer::sum);

            totalBigrams += row.count();
        }
    }

    /**
     * Computes P(wordId2 | wordId1) using the smoothing method configured for this model.
     *
     * @param wordId1 the database ID of the preceding word
     * @param wordId2 the database ID of the following word
     * @return estimated probability, guaranteed to be non-negative;
     *         may be 0.0 only when smoothing is NONE and the bigram was not observed
     */
    public double computeProbability(int wordId1, int wordId2) {
        return switch (smoothingMethod) {
            case NONE      -> computeMleProbability(wordId1, wordId2);
            case LAPLACE   -> computeAddKProbability(wordId1, wordId2, 1.0);
            case ADD_K     -> computeAddKProbability(wordId1, wordId2, k);
            case KNESER_NEY -> computeKneserNeyProbability(wordId1, wordId2);
        };
    }

    /**
     * Plain maximum-likelihood probability: count(w1, w2) / count(w1).
     *
     * I only use this when no smoothing is requested, so unseen bigrams come back as 0.0.
     */
    private double computeMleProbability(int wordId1, int wordId2) {
        int countW1 = unigramCounts.getOrDefault(wordId1, 0);
        if (countW1 == 0) return 0.0;
        int countW1W2 = bigramCounts
            .getOrDefault(wordId1, Collections.emptyMap())
            .getOrDefault(wordId2, 0);
        return (double) countW1W2 / countW1;
    }

    /**
     * Add-k smoothing: (count(w1, w2) + k) / (count(w1) + k * V).
     *
     * This gives unseen pairs some probability mass instead of dropping them to zero.
     * When k = 1, this becomes standard Laplace smoothing.
     */
    private double computeAddKProbability(int wordId1, int wordId2, double kValue) {
        int countW1 = unigramCounts.getOrDefault(wordId1, 0);
        int countW1W2 = bigramCounts
            .getOrDefault(wordId1, Collections.emptyMap())
            .getOrDefault(wordId2, 0);
        return (countW1W2 + kValue) / (countW1 + kValue * vocabularySize);
    }

    /**
     * Kneser-Ney smoothing: discount the observed count, then back off to a continuation term.
     *
     * Formula:
     *   P(w2 | w1) = max(C(w1,w2) - D, 0) / C(w1)
     *              + lambda(w1) * P_continuation(w2)
     *
     * where:
     *   D           = discount constant (typically 0.75)
     *   lambda(w1)  = D * |{w : C(w1,w) > 0}| / C(w1)    [backoff weight]
     *   P_cont(w2)  = |{w : C(w,w2) > 0}| / total_bigrams  [continuation probability]
     *
     * The continuation term helps me reward words that show up in many different
     * contexts, not just words that are globally frequent.
     */
    private double computeKneserNeyProbability(int wordId1, int wordId2) {
        int countW1 = unigramCounts.getOrDefault(wordId1, 0);
        int countW1W2 = bigramCounts
            .getOrDefault(wordId1, Collections.emptyMap())
            .getOrDefault(wordId2, 0);

        // First keep the discounted part of the observed bigram count.
        double discountedBigram = Math.max(countW1W2 - discount, 0.0) / (countW1 + 1e-10);

        // Then figure out how much probability mass to redistribute.
        int uniqueCompletions = bigramCounts.getOrDefault(wordId1, Collections.emptyMap()).size();
        double lambda = (discount * uniqueCompletions) / (countW1 + 1e-10);

        // Finally use the continuation term for the backed-off probability.
        double pContinuation = continuationCounts.getOrDefault(wordId2, 0) / (double)(totalBigrams + 1);

        return discountedBigram + lambda * pContinuation;
    }

    /**
     * Computes probabilities for every observed bigram transition.
     *
     * I use this when training is done and I want to write the probability values
     * back into the word_transitions table in one pass.
     *
     * @return map from int[]{firstWordId, secondWordId} to computed probability
     */
    public Map<int[], Double> computeAllProbabilities() {
        Map<int[], Double> result = new HashMap<>();
        for (var outer : bigramCounts.entrySet()) {
            int wordId1 = outer.getKey();
            for (int wordId2 : outer.getValue().keySet()) {
                result.put(new int[]{wordId1, wordId2}, computeProbability(wordId1, wordId2));
            }
        }
        return result;
    }

    /**
     * Returns the top N most likely words to follow the given word ID.
     *
     * I only rank words that were actually seen after wordId1 during training.
     * I also store the elapsed time so the UI can show prediction latency.
     *
     * @param wordId1 the database ID of the preceding word
     * @param topN    the maximum number of predictions to return
     * @return predictions sorted by probability descending; may be fewer than topN
     *         if wordId1 has fewer observed followers
     */
    public List<Prediction> predictNext(int wordId1, int topN) {
        long start = System.currentTimeMillis();

        List<Prediction> predictions = bigramCounts
            .getOrDefault(wordId1, Collections.emptyMap())
            .keySet()
            .stream()
            .map(wordId2 -> new Prediction(
                idToWord.getOrDefault(wordId2, "[unknown]"),
                wordId2,
                computeProbability(wordId1, wordId2)
            ))
            .sorted(Comparator.comparingDouble(Prediction::probability).reversed())
            .limit(topN)
            .collect(Collectors.toList());

        lastPredictionLatencyMs = System.currentTimeMillis() - start;
        return predictions;
    }

    /**
     * Returns the top N most likely words to follow the given word string.
     *
     * I normalize the input first so it matches the lowercase tokens coming from
     * the data pipeline, then delegate to the ID-based version.
     *
     * @param word the preceding word as typed by the user
     * @param topN the maximum number of predictions to return
     * @return predictions sorted by probability descending
     */
    public List<Prediction> predictNext(String word, int topN) {
        if (word == null || word.isBlank()) {
            return Collections.emptyList();
        }

        Integer wordId = wordToId.get(word.trim().toLowerCase(Locale.ROOT));
        if (wordId == null) return Collections.emptyList();
        return predictNext(wordId, topN);
    }

    /**
     * Generates a word sequence starting from the given word using the specified strategy.
     *
     * The output always starts with startWord. Generation stops when maxLength is reached
     * or when no followers are found for the current word.
     *
     * @param startWord the first word in the generated sequence
     * @param maxLength the maximum number of words in the output (including startWord)
     * @param strategy  how to select words at each step
     * @return list of words forming the generated sequence; always contains at least startWord
     */
    public List<String> generateSentence(String startWord, int maxLength, GenerationStrategy strategy) {
        return switch (strategy) {
            case GREEDY -> generateGreedy(startWord, maxLength);
            case BEAM   -> generateBeam(startWord, maxLength, 5);
            case SAMPLE -> generateSample(startWord, maxLength);
        };
    }

    /**
     * Greedy generation: at each step, picks the single highest-probability next word.
     *
     * Fast but deterministic. Can produce repetitive output on small corpora if the
     * model settles into a high-frequency loop.
     */
    private List<String> generateGreedy(String startWord, int maxLength) {
        List<String> sentence = new ArrayList<>();
        sentence.add(startWord);
        String current = startWord;

        for (int i = 1; i < maxLength; i++) {
            List<Prediction> next = predictNext(current, 1);
            if (next.isEmpty()) break;
            current = next.get(0).word();
            sentence.add(current);
        }
        return sentence;
    }

    /**
     * Beam search: maintains the top beamWidth candidate sequences at each step and
     * returns the sequence with the highest cumulative log-probability.
     *
     * More expensive than greedy but produces better output by considering multiple
     * candidate paths before committing to a word choice.
     *
     * @param beamWidth the number of candidate sequences to keep at each step
     */
    private List<String> generateBeam(String startWord, int maxLength, int beamWidth) {
        // Each element is a pair of (cumulative log-probability, word sequence).
        // Log-probabilities are used instead of raw probabilities to avoid underflow
        // when multiplying many small values together.
        List<double[]> beamScores = new ArrayList<>();
        List<List<String>> beamSequences = new ArrayList<>();
        beamScores.add(new double[]{0.0});
        beamSequences.add(new ArrayList<>(List.of(startWord)));

        for (int step = 1; step < maxLength; step++) {
            List<double[]> candidateScores = new ArrayList<>();
            List<List<String>> candidateSequences = new ArrayList<>();

            for (int b = 0; b < beamSequences.size(); b++) {
                String lastWord = beamSequences.get(b).get(beamSequences.get(b).size() - 1);
                List<Prediction> next = predictNext(lastWord, beamWidth);

                if (next.isEmpty()) {
                    // This beam has no followers; carry it forward unchanged
                    candidateScores.add(beamScores.get(b));
                    candidateSequences.add(beamSequences.get(b));
                    continue;
                }

                for (Prediction p : next) {
                    String nextWord = p.word();

                    // prevent self-loop repetition
                    if (nextWord.equals(lastWord)) continue;

                    // prevent cycles
                    if (newSeq.contains(nextWord)) continue;

                    double newScore = beamScores.get(b)[0] + Math.log(p.probability() + 1e-10);

                    List<String> newSeq = new ArrayList<>(beamSequences.get(b));
                    newSeq.add(nextWord);

                    candidateScores.add(new double[]{newScore});
                    candidateSequences.add(newSeq);
                }
            }

            // Keep only the top beamWidth candidates by log-probability
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < candidateScores.size(); i++) indices.add(i);
            indices.sort((a, b) -> Double.compare(candidateScores.get(b)[0], candidateScores.get(a)[0]));

            beamScores.clear();
            beamSequences.clear();
            for (int i = 0; i < Math.min(beamWidth, indices.size()); i++) {
                int idx = indices.get(i);
                beamScores.add(candidateScores.get(idx));
                beamSequences.add(candidateSequences.get(idx));
            }
        }

        if (beamSequences.isEmpty()) return List.of(startWord);

        // Return the beam with the highest log-probability
        int best = 0;
        for (int i = 1; i < beamScores.size(); i++) {
            if (beamScores.get(i)[0] > beamScores.get(best)[0]) best = i;
        }
        return beamSequences.get(best);
    }

    /**
     * Random sampling: at each step, selects the next word by sampling from the
     * probability distribution over observed followers.
     *
     * Produces varied output; each call with the same input may return a different result.
     * Useful for testing that the model's learned distribution is reasonable.
     */
    private List<String> generateSample(String startWord, int maxLength) {
        List<String> sentence = new ArrayList<>();
        sentence.add(startWord);
        String current = startWord;
        Random random = new Random();

        for (int i = 1; i < maxLength; i++) {
            List<Prediction> candidates = predictNext(current, 20);
            if (candidates.isEmpty()) break;

            // Sample one word proportionally to its probability
            double total = candidates.stream().mapToDouble(Prediction::probability).sum();
            double threshold = random.nextDouble() * total;
            double cumulative = 0.0;
            String chosen = candidates.get(candidates.size() - 1).word();
            for (Prediction p : candidates) {
                cumulative += p.probability();
                if (cumulative >= threshold) {
                    chosen = p.word();
                    break;
                }
            }
            sentence.add(chosen);
            current = chosen;
        }
        return sentence;
    }

    /**
     * Computes the perplexity of the model on a held-out set of transitions.
     *
     * Perplexity = exp(-1/N * sum of log P(w2|w1) over all test bigrams).
     * Lower perplexity means the model predicts the test data better.
     * Use this to compare smoothing methods: train on 90% of the data, evaluate on 10%.
     *
     * @param testTransitions word-pair transitions not used during training
     * @return perplexity score; Double.MAX_VALUE if the test set is empty
     */
    public double computePerplexity(List<TransitionRow> testTransitions) {
        double totalLogProb = 0.0;
        int totalTokens = 0;

        for (TransitionRow row : testTransitions) {
            double prob = computeProbability(row.firstWordId(), row.secondWordId());
            // Use -100 as a floor for log(0) to avoid infinite perplexity with MLE smoothing.
            // This penalizes unseen bigrams heavily but keeps the result finite.
            double logProb = prob > 0.0 ? Math.log(prob) : -100.0;
            // Weight by occurrence count so each observed token contributes equally
            totalLogProb += logProb * row.count();
            totalTokens  += row.count();
        }

        if (totalTokens == 0) return Double.MAX_VALUE;
        return Math.exp(-totalLogProb / totalTokens);
    }

    // --- Getters for metrics and testing ---

    /** Returns the number of unique words in the vocabulary after training. */
    public int getVocabularySize() { return vocabularySize; }

    /** Returns the total number of bigram token occurrences seen during training. */
    public int getTotalBigrams() { return totalBigrams; }

    /** Returns the number of milliseconds taken by the most recent predictNext() call. */
    public long getLastPredictionLatencyMs() { return lastPredictionLatencyMs; }

    /** Returns the smoothing method this model was configured with. */
    public SmoothingMethod getSmoothingMethod() { return smoothingMethod; }

    /**
     * Returns an unmodifiable view of the word-to-ID mapping.
     * Intended for unit tests and inspection; do not modify.
     */
    public Map<String, Integer> getWordToId() {
        return Collections.unmodifiableMap(wordToId);
    }

    /**
     * Returns an unmodifiable view of the bigram counts map.
     * Intended for unit tests and inspection; do not modify.
     */
    public Map<Integer, Map<Integer, Integer>> getBigramCounts() {
        return Collections.unmodifiableMap(bigramCounts);
    }

    /**
     * Returns an unmodifiable view of the unigram counts map.
     * Intended for unit tests and inspection; do not modify.
     */
    public Map<Integer, Integer> getUnigramCounts() {
        return Collections.unmodifiableMap(unigramCounts);
    }
}
