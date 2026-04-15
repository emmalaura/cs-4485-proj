package com.cs4485.model;

/**
 * Smoothing methods available for the bigram model.
 *
 * Smoothing prevents zero probabilities for word pairs not seen during training.
 * When a bigram was never observed, an unsmoothed model assigns it probability 0,
 * which causes problems during sentence generation and perplexity evaluation.
 * Each smoothing strategy handles this differently.
 *
 * The appropriate method depends on the size and sparseness of the training corpus.
 * For this project's technical text corpus, KNESER_NEY is the recommended default.
 */
public enum SmoothingMethod {

    /**
     * No smoothing (Maximum Likelihood Estimate).
     *
     * Probability = count(w1, w2) / count(w1).
     * Returns 0.0 for any word pair not observed in training.
     * Suitable only for very large corpora where nearly all word pairs are observed.
     */
    NONE("none"),

    /**
     * Laplace (add-one) smoothing.
     *
     * Probability = (count(w1, w2) + 1) / (count(w1) + V)
     * where V is the vocabulary size.
     * Adds 1 to every bigram count, including unseen ones, guaranteeing no zero probabilities.
     * Simple and effective for small corpora but can over-smooth when the vocabulary is large.
     */
    LAPLACE("laplace"),

    /**
     * Add-k smoothing.
     *
     * Probability = (count(w1, w2) + k) / (count(w1) + k * V)
     * A generalization of Laplace where k can be tuned to any positive value.
     * Values of k less than 1 assign less probability mass to unseen bigrams than Laplace,
     * which is often better for medium-sized corpora.
     */
    ADD_K("add_k"),

    /**
     * Kneser-Ney smoothing.
     *
     * Discounts observed bigram counts by a fixed amount D and backs off to a
     * continuation probability that rewards words appearing in many different contexts
     * rather than simply words that are frequent.
     * Performs best on sparse technical text and is the default for this project.
     *
     * See BigramModel.computeKneserNeyProbability() for the full formula.
     */
    KNESER_NEY("kneser_ney");

    private final String label;

    SmoothingMethod(String label) {
        this.label = label;
    }

    /**
     * Returns the command-line label for this smoothing method.
     * Used by ModelTrainer to parse and display the selected method.
     *
     * @return the string label (e.g., "kneser_ney")
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the SmoothingMethod corresponding to the given label string.
     * Comparison is case-insensitive.
     *
     * @param label the label to look up (e.g., "laplace", "kneser_ney")
     * @return the matching SmoothingMethod
     * @throws IllegalArgumentException if no method matches the given label
     */
    public static SmoothingMethod fromLabel(String label) {
        for (SmoothingMethod method : values()) {
            if (method.label.equalsIgnoreCase(label)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown smoothing method: " + label
            + ". Valid options: none, laplace, add_k, kneser_ney");
    }
}
