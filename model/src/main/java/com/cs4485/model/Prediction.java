package com.cs4485.model;

/**
 * Immutable representation of a single next-word prediction.
 *
 * Returned by BigramModel.predictNext() and DBInterface.getPredictions().
 * The UI team can use a list of these to display autocomplete suggestions
 * to the user as they type.
 *
 * @param word        the predicted next word as a string
 * @param wordId      the database ID of the predicted word (useful for follow-up DB queries)
 * @param probability the estimated probability that this word follows the preceding word,
 *                    as computed by the model's selected smoothing method
 */
public record Prediction(String word, int wordId, double probability) {}
