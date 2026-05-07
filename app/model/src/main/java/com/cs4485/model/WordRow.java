package com.cs4485.model;

/**
 * Immutable representation of a row from the words database table.
 *
 * The words table is populated by the Data Processing team. Each row represents
 * one unique word found in the training corpus, along with statistics about how
 * often it appears and in what positions.
 *
 * Column names match the database schema exactly so that DBInterface can map
 * ResultSet columns directly to record fields.
 *
 * @param wordId          the primary key uniquely identifying this word in the database
 * @param word            the word string as it appears in the tokenized corpus
 * @param totalOccurrence the total number of times this word appeared across all documents
 * @param startCount      the number of times this word was the first word of a sentence
 * @param endCount        the number of times this word was the last word of a sentence
 */
public record WordRow(int wordId, String word, int totalOccurrence, int startCount, int endCount) {}
