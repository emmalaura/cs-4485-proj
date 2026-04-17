package com.cs4485.model;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ModelPredictor.completeSentence()
 *
 * getNextWords() requires a live database connection and is not tested here
 * Those calls will be covered in integration tests once the database is available
 *
 * Tests use a small hardcoded corpus to keep setup simple and results predictable
 */
class ModelPredictorTest {

    private static final List<WordRow> WORDS = List.of(
        new WordRow(1, "the",       5, 2, 0),
        new WordRow(2, "algorithm", 3, 0, 0),
        new WordRow(3, "is",        2, 0, 0),
        new WordRow(4, "efficient", 2, 0, 2),
        new WordRow(5, "data",      2, 1, 0),
        new WordRow(6, "structure", 2, 0, 2)
    );

    // the -> algorithm (3), the -> data (2), algorithm -> is (2), is -> efficient (2), data -> structure (2)
    private static final List<TransitionRow> TRANSITIONS = List.of(
        new TransitionRow(1, 1, 2, 3),
        new TransitionRow(2, 1, 5, 2),
        new TransitionRow(3, 2, 3, 2),
        new TransitionRow(4, 3, 4, 2),
        new TransitionRow(5, 5, 6, 2)
    );

    private BigramModel model;

    @BeforeEach
    void setUp() {
        model = new BigramModel(SmoothingMethod.KNESER_NEY);
        model.train(WORDS, TRANSITIONS);
    }

    @Test
    void completeSentenceWithSingleWordInputStartsWithThatWord() {
        List<String> result = ModelPredictor.completeSentence(model, "the", 4, GenerationStrategy.GREEDY);
        assertFalse(result.isEmpty());
        assertEquals("the", result.get(0));
    }

    @Test
    void completeSentenceNormalizesMixedCaseInput() {
        List<String> result = ModelPredictor.completeSentence(model, "The Algorithm", 6, GenerationStrategy.GREEDY);
        assertTrue(result.size() >= 2);
        assertEquals("the", result.get(0));
        assertEquals("algorithm", result.get(1));
    }

    @Test
    void completeSentencePreservesLeadingWordsBeforeSeed() {
        // "the algorithm" -> "the" is preserved, "algorithm" is the seed
        List<String> result = ModelPredictor.completeSentence(model, "the algorithm", 6, GenerationStrategy.GREEDY);
        assertTrue(result.size() >= 2, "Result should have at least 'the' and the continuation");
        assertEquals("the", result.get(0),
            "The word before the seed should be preserved in the output");
        assertEquals("algorithm", result.get(1),
            "The seed word should appear as the second element after the preserved prefix");
    }

    @Test
    void completeSentenceDoesNotExceedMaxLength() {
        List<String> result = ModelPredictor.completeSentence(model, "the", 3, GenerationStrategy.BEAM);
        assertTrue(result.size() <= 3);
    }

    @Test
    void completeSentenceWithNullInputReturnsEmpty() {
        List<String> result = ModelPredictor.completeSentence(model, null, 5, GenerationStrategy.GREEDY);
        assertTrue(result.isEmpty());
    }

    @Test
    void completeSentenceWithBlankInputReturnsEmpty() {
        List<String> result = ModelPredictor.completeSentence(model, "   ", 5, GenerationStrategy.GREEDY);
        assertTrue(result.isEmpty());
    }

    @Test
    void completeSentenceWorksWithEveryGenerationStrategy() {
        for (GenerationStrategy strategy : GenerationStrategy.values()) {
            List<String> result = ModelPredictor.completeSentence(model, "the", 4, strategy);
            assertFalse(result.isEmpty(),
                "completeSentence should return a non-empty result for strategy: " + strategy);
        }
    }

    @Test
    void completeSentenceWithUnknownSeedWordStillReturnsSeedWord() {
        // "unknownword" has no followers, so generation stops immediately
        // The output should still contain the seed word
        List<String> result = ModelPredictor.completeSentence(model, "the unknownword", 5, GenerationStrategy.GREEDY);
        assertTrue(result.contains("unknownword"),
            "Seed word must appear in output even when no followers are known");
    }

    @Test
    void completeSentenceMaxLengthOfOneReturnsOnlyInputWords() {
        // maxLength = 1 means no room to add words beyond what is already in the input
        List<String> result = ModelPredictor.completeSentence(model, "the", 1, GenerationStrategy.GREEDY);
        assertEquals(1, result.size());
        assertEquals("the", result.get(0));
    }
}
