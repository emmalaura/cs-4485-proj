package com.cs4485.model;

/**
 * Immutable representation of a row from the word_transitions database table.
 *
 * The word_transitions table is populated by the Data Processing team. Each row
 * records how many times a specific word (secondWordId) followed another specific
 * word (firstWordId) in the training corpus.
 *
 * The probability column in the database is NOT included here because it is an
 * output of this model, not an input. DBInterface.updateProbabilities() writes
 * the computed values back to that column after training.
 *
 * Column names match the database schema exactly so that DBInterface can map
 * ResultSet columns directly to record fields.
 *
 * @param transitionId the primary key for this transition record
 * @param firstWordId  the database ID of the word that comes first in the pair
 * @param secondWordId the database ID of the word that follows firstWordId
 * @param count        the number of times secondWordId was observed following firstWordId
 */
public record TransitionRow(int transitionId, int firstWordId, int secondWordId, int count) {}
