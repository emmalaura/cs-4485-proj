package com.cs4485.model;

/**
 * Generation strategies for sentence completion in BigramModel.generateSentence().
 *
 * Controls how the model selects the next word at each step when extending
 * a partial sentence. Each strategy makes a different tradeoff between
 * output quality, variety, and computation time.
 */
public enum GenerationStrategy {

    /**
     * At each step, pick the single most probable next word.
     *
     * Fastest strategy. Can produce repetitive or short output on small corpora
     * because the model may loop back to the same high-frequency words repeatedly.
     * Good for a quick first test of whether the model is working correctly.
     */
    GREEDY,

    /**
     * Maintain the top K candidate word sequences at each step, then return the
     * sequence with the highest total log-probability.
     *
     * Beam width is fixed at 5 in BigramModel. Produces more varied and contextually
     * coherent output than GREEDY because it considers multiple paths before committing.
     */
    BEAM,

    /**
     * At each step, randomly sample the next word from the probability distribution.
     *
     * Produces the most varied output but the least deterministic results. Useful
     * for exploring the model's learned distribution and testing probability behavior.
     */
    SAMPLE
}
