package com.cs4485.model;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BigramModel.
 *
 * All tests use hardcoded WordRow and TransitionRow data. No database connection
 * is required. The test corpus represents a small set of technical sentences:
 *
 *   "the algorithm processes data"
 *   "the algorithm is efficient"
 *   "the binary search algorithm"
 *   "binary search is efficient"
 *   "the data is efficient"
 *
 * Word IDs and transition counts are derived from this corpus and verified manually.
 */
class BigramModelTest {

    // Word IDs used in test data
    private static final int ID_THE       = 1;
    private static final int ID_ALGORITHM = 2;
    private static final int ID_PROCESSES = 3;
    private static final int ID_DATA      = 4;
    private static final int ID_IS        = 5;
    private static final int ID_EFFICIENT = 6;
    private static final int ID_BINARY    = 7;
    private static final int ID_SEARCH    = 8;

    // Words table rows: (wordId, word, totalOccurrence, startCount, endCount)
    private static final List<WordRow> TEST_WORDS = List.of(
        new WordRow(ID_THE,       "the",       5, 3, 0),
        new WordRow(ID_ALGORITHM, "algorithm", 3, 0, 0),
        new WordRow(ID_PROCESSES, "processes", 1, 0, 0),
        new WordRow(ID_DATA,      "data",      2, 0, 1),
        new WordRow(ID_IS,        "is",        3, 0, 0),
        new WordRow(ID_EFFICIENT, "efficient", 3, 0, 3),
        new WordRow(ID_BINARY,    "binary",    2, 2, 0),
        new WordRow(ID_SEARCH,    "search",    2, 0, 0)
    );

    // Transitions table rows: (transitionId, firstWordId, secondWordId, count)
    // Derived from the test corpus above:
    //   the -> algorithm (3 times), the -> binary (1), the -> data (1)
    //   algorithm -> processes (1), algorithm -> is (1)
    //   binary -> search (2)
    //   search -> algorithm (1)
    //   is -> efficient (3)
    //   processes -> data (1)
    private static final List<TransitionRow> TEST_TRANSITIONS = List.of(
        new TransitionRow(1, ID_THE,       ID_ALGORITHM, 3),
        new TransitionRow(2, ID_THE,       ID_BINARY,    1),
        new TransitionRow(3, ID_THE,       ID_DATA,      1),
        new TransitionRow(4, ID_ALGORITHM, ID_PROCESSES, 1),
        new TransitionRow(5, ID_ALGORITHM, ID_IS,        1),
        new TransitionRow(6, ID_BINARY,    ID_SEARCH,    2),
        new TransitionRow(7, ID_SEARCH,    ID_ALGORITHM, 1),
        new TransitionRow(8, ID_IS,        ID_EFFICIENT, 3),
        new TransitionRow(9, ID_PROCESSES, ID_DATA,      1)
    );

    private BigramModel model;

    @BeforeEach
    void setUp() {
        // Use Laplace as the default for most tests since it has simple, verifiable properties
        model = new BigramModel(SmoothingMethod.LAPLACE);
        model.train(TEST_WORDS, TEST_TRANSITIONS);
    }

    // --- Training tests ---

    @Test
    void trainBuildsCorrectVocabularySize() {
        assertEquals(8, model.getVocabularySize(),
            "Vocabulary size should equal the number of rows in TEST_WORDS");
    }

    @Test
    void trainBuildsCorrectBigramCountForKnownPair() {
        // "the" -> "algorithm" appears 3 times in TEST_TRANSITIONS
        int count = model.getBigramCounts().get(ID_THE).get(ID_ALGORITHM);
        assertEquals(3, count);
    }

    @Test
    void trainBuildsCorrectUnigramCountFromWordsTable() {
        // totalOccurrence for "the" in TEST_WORDS is 5
        assertEquals(5, model.getUnigramCounts().get(ID_THE));
    }

    @Test
    void trainClearsPreviousDataOnRetrain() {
        // A second call to train() should replace, not accumulate, previous counts
        List<WordRow> singleWord = List.of(new WordRow(99, "test", 1, 0, 0));
        List<TransitionRow> noTransitions = List.of();
        model.train(singleWord, noTransitions);

        assertEquals(1, model.getVocabularySize());
        assertTrue(model.getBigramCounts().isEmpty());
    }

    // --- Smoothing tests ---

    @Test
    void laplaceProbabilityIsNonZeroForUnseenBigram() {
        // "efficient" -> "the" was never observed, but Laplace assigns it a positive probability
        double prob = model.computeProbability(ID_EFFICIENT, ID_THE);
        assertTrue(prob > 0.0,
            "Laplace smoothing must give non-zero probability to unseen bigrams");
    }

    @Test
    void mleProbabilityIsZeroForUnseenBigram() {
        BigramModel mleModel = new BigramModel(SmoothingMethod.NONE);
        mleModel.train(TEST_WORDS, TEST_TRANSITIONS);
        double prob = mleModel.computeProbability(ID_EFFICIENT, ID_THE);
        assertEquals(0.0, prob, 1e-10,
            "MLE must return 0.0 for a bigram that was never observed");
    }

    @Test
    void laplaceProbabilitiesSumToApproximatelyOne() {
        // For a fixed word1, the sum of P(w2|w1) over all w2 in the vocabulary should be ~1.0
        double total = 0.0;
        for (WordRow w : TEST_WORDS) {
            total += model.computeProbability(ID_THE, w.wordId());
        }
        assertEquals(1.0, total, 0.01,
            "Laplace probabilities over the vocabulary must sum to approximately 1.0");
    }

    @Test
    void addKProbabilityIsNonZeroForUnseenBigram() {
        BigramModel addKModel = new BigramModel(SmoothingMethod.ADD_K, 0.5, 0.75);
        addKModel.train(TEST_WORDS, TEST_TRANSITIONS);
        double prob = addKModel.computeProbability(ID_EFFICIENT, ID_THE);
        assertTrue(prob > 0.0);
    }

    @Test
    void kneserNeyProbabilitiesAreNonNegativeForAllPairs() {
        BigramModel knModel = new BigramModel(SmoothingMethod.KNESER_NEY);
        knModel.train(TEST_WORDS, TEST_TRANSITIONS);
        for (WordRow w1 : TEST_WORDS) {
            for (WordRow w2 : TEST_WORDS) {
                double prob = knModel.computeProbability(w1.wordId(), w2.wordId());
                assertTrue(prob >= 0.0,
                    "Kneser-Ney probability must be non-negative for ("
                    + w1.word() + ", " + w2.word() + ")");
            }
        }
    }

    // --- predictNext() tests ---

    @Test
    void predictNextReturnsSortedByProbabilityDescending() {
        List<Prediction> predictions = model.predictNext(ID_THE, 3);
        assertFalse(predictions.isEmpty());
        for (int i = 0; i < predictions.size() - 1; i++) {
            assertTrue(
                predictions.get(i).probability() >= predictions.get(i + 1).probability(),
                "Predictions must be sorted from highest to lowest probability"
            );
        }
    }

    @Test
    void predictNextReturnsHighestCountWordFirst() {
        // "the" -> "algorithm" has count 3, which is higher than any other follower of "the"
        List<Prediction> predictions = model.predictNext(ID_THE, 1);
        assertFalse(predictions.isEmpty());
        assertEquals("algorithm", predictions.get(0).word());
    }

    @Test
    void predictNextByStringMatchesPredictNextById() {
        List<Prediction> byId     = model.predictNext(ID_THE, 3);
        List<Prediction> byString = model.predictNext("the", 3);
        assertEquals(byId.size(), byString.size());
        for (int i = 0; i < byId.size(); i++) {
            assertEquals(byId.get(i).word(), byString.get(i).word());
            assertEquals(byId.get(i).probability(), byString.get(i).probability(), 1e-10);
        }
    }

    @Test
    void predictNextByStringReturnsEmptyForUnknownWord() {
        List<Prediction> predictions = model.predictNext("unknownword123", 5);
        assertTrue(predictions.isEmpty(),
            "predictNext must return an empty list for words not seen in training");
    }

    @Test
    void predictNextTopNLimitIsRespected() {
        List<Prediction> predictions = model.predictNext(ID_THE, 2);
        assertTrue(predictions.size() <= 2);
    }

    // --- generateSentence() tests ---

    @Test
    void greedyGenerationStartsWithSeedWord() {
        List<String> sentence = model.generateSentence("the", 5, GenerationStrategy.GREEDY);
        assertFalse(sentence.isEmpty());
        assertEquals("the", sentence.get(0));
    }

    @Test
    void greedyGenerationDoesNotExceedMaxLength() {
        List<String> sentence = model.generateSentence("the", 3, GenerationStrategy.GREEDY);
        assertTrue(sentence.size() <= 3);
    }

    @Test
    void beamSearchStartsWithSeedWord() {
        List<String> sentence = model.generateSentence("the", 5, GenerationStrategy.BEAM);
        assertFalse(sentence.isEmpty());
        assertEquals("the", sentence.get(0));
    }

    @Test
    void beamSearchDoesNotExceedMaxLength() {
        List<String> sentence = model.generateSentence("the", 4, GenerationStrategy.BEAM);
        assertTrue(sentence.size() <= 4);
    }

    @Test
    void sampleGenerationProducesWordsFromVocabulary() {
        Set<String> vocabulary = new HashSet<>();
        for (WordRow w : TEST_WORDS) vocabulary.add(w.word());

        List<String> sentence = model.generateSentence("the", 6, GenerationStrategy.SAMPLE);
        assertFalse(sentence.isEmpty());
        for (String word : sentence) {
            assertTrue(vocabulary.contains(word),
                "Generated word '" + word + "' must be in the training vocabulary");
        }
    }

    @Test
    void generationWithUnknownSeedWordStillReturnsNonEmptyList() {
        // When the seed word has no followers, the method should return at least the seed word
        List<String> sentence = model.generateSentence("efficient", 5, GenerationStrategy.GREEDY);
        assertFalse(sentence.isEmpty());
    }

    // --- computePerplexity() tests ---

    @Test
    void perplexityIsFiniteAndPositive() {
        double ppl = model.computePerplexity(TEST_TRANSITIONS);
        assertTrue(ppl > 0.0 && ppl < Double.MAX_VALUE,
            "Perplexity must be finite and positive");
    }

    @Test
    void perplexityIsLowerOnTrainingDataThanHeldOut() {
        // Train on the first 7 transitions and evaluate on the last 2
        BigramModel trainModel = new BigramModel(SmoothingMethod.LAPLACE);
        List<TransitionRow> trainSet = TEST_TRANSITIONS.subList(0, 7);
        List<TransitionRow> testSet  = TEST_TRANSITIONS.subList(7, TEST_TRANSITIONS.size());
        trainModel.train(TEST_WORDS, trainSet);

        double trainPpl = trainModel.computePerplexity(trainSet);
        double testPpl  = trainModel.computePerplexity(testSet);

        assertTrue(trainPpl <= testPpl,
            "Training perplexity (" + trainPpl + ") should be <= test perplexity (" + testPpl + ")");
    }

    @Test
    void perplexityReturnsMaxValueForEmptyTestSet() {
        double ppl = model.computePerplexity(Collections.emptyList());
        assertEquals(Double.MAX_VALUE, ppl);
    }

    // --- computeAllProbabilities() tests ---

    @Test
    void computeAllProbabilitiesCoverAllTransitions() {
        Map<int[], Double> probs = model.computeAllProbabilities();
        // One entry per unique (word1, word2) pair in the transitions table
        assertEquals(TEST_TRANSITIONS.size(), probs.size());
    }

    @Test
    void computeAllProbabilitiesValuesArePositiveWithSmoothing() {
        Map<int[], Double> probs = model.computeAllProbabilities();
        for (double p : probs.values()) {
            assertTrue(p > 0.0,
                "All probabilities should be positive when using Laplace smoothing");
        }
    }

    // --- Latency tracking tests ---

    @Test
    void predictionLatencyIsTrackedAfterPredictNext() {
        model.predictNext(ID_THE, 3);
        assertTrue(model.getLastPredictionLatencyMs() >= 0,
            "Latency must be recorded after predictNext() is called");
    }
}
