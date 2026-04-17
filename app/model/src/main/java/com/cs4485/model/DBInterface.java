package com.cs4485.model;

import java.sql.*;
import java.util.*;

/**
 * JDBC layer for the model side of the project.
 *
 * Used to connect to MySQL, load the word and transition counts the team stores
 * in the database, and write the trained probabilities back to word_transitions.
 *
 * Expected column names:
 *   words:            wordId, word, totalOccurrence, startCount, endCount
 *   word_transitions: transitionId, firstWordId, secondWordId, count, probability
 */
public class DBInterface implements AutoCloseable {

    private final Connection connection;

    /**
     * Opens a JDBC connection using the provided database credentials.
     *
     * @param host     the hostname or IP address of the MySQL server
     * @param database the name of the database schema
     * @param user     the MySQL username
     * @param password the MySQL password
     * @throws SQLException if the driver cannot connect with the given parameters
     */
    public DBInterface(String host, String database, String user, String password) throws SQLException {
        String url = "jdbc:mysql://" + host + "/" + database
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        connection = DriverManager.getConnection(url, user, password);
    }

    /**
     * Opens a JDBC connection using the standard DB environment variables.
     *
     * This keeps the connection setup out of the source code when we run against
     * a shared database.
     *
     * @return a connected DBInterface instance
     * @throws SQLException          if the connection cannot be established
     * @throws IllegalStateException if any required environment variable is not set
     */
    public static DBInterface fromEnvironment() throws SQLException {
        return new DBInterface(
            requireEnv("DB_HOST"),
            requireEnv("DB_NAME"),
            requireEnv("DB_USER"),
            requireEnv("DB_PASSWORD")
        );
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Required environment variable is not set: " + name);
        }
        return value;
    }

    /**
     * Loads every row from the words table.
     *
     * Used to build the vocabulary and unigram counts before training.
     *
     * @return list of WordRow records; one per unique word in the vocabulary
     * @throws SQLException if the query fails
     */
    public List<WordRow> loadWords() throws SQLException {
        List<WordRow> words = new ArrayList<>();
        String sql = "SELECT wordId, word, totalOccurrence, startCount, endCount FROM words";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                words.add(new WordRow(
                    rs.getInt("wordId"),
                    rs.getString("word"),
                    rs.getInt("totalOccurrence"),
                    rs.getInt("startCount"),
                    rs.getInt("endCount")
                ));
            }
        }
        return words;
    }

    /**
     * Loads all rows from word_transitions, excluding the probability column.
     *
     * Only the transition counts are needed here because probability is written
     * by the model after training.
     *
     * @return list of TransitionRow records; one per observed word pair
     * @throws SQLException if the query fails
     */
    public List<TransitionRow> loadTransitions() throws SQLException {
        List<TransitionRow> transitions = new ArrayList<>();
        String sql = "SELECT transitionId, firstWordId, secondWordId, count "
                   + "FROM word_transitions";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                transitions.add(new TransitionRow(
                    rs.getInt("transitionId"),
                    rs.getInt("firstWordId"),
                    rs.getInt("secondWordId"),
                    rs.getInt("count")
                ));
            }
        }
        return transitions;
    }

    /**
     * Writes the computed probabilities back into word_transitions.
     *
     * I batch these updates so training can write everything back in one transaction.
     *
     * @param probabilities map from int[]{firstWordId, secondWordId} to probability value,
     *                      as returned by BigramModel.computeAllProbabilities()
     * @throws SQLException if any batch update fails; the transaction is rolled back
     */
    public void updateProbabilities(Map<int[], Double> probabilities) throws SQLException {
        String sql = "UPDATE word_transitions SET probability = ? "
                   + "WHERE firstWordId = ? AND secondWordId = ?";

        connection.setAutoCommit(false);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (var entry : probabilities.entrySet()) {
                int[] ids = entry.getKey();
                stmt.setDouble(1, entry.getValue());
                stmt.setInt(2, ids[0]); // firstWordId
                stmt.setInt(3, ids[1]); // secondWordId
                stmt.addBatch();
            }
            stmt.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            // Always restore auto-commit so subsequent calls behave predictably
            connection.setAutoCommit(true);
        }
    }

    /**
     * Returns the top N next-word predictions for a given word.
     *
     * This is the lightweight path the UI can use without loading the whole model
     * into memory.
     *
     * For best performance, the DB team should add the index:
     *   INDEX idx_prob (firstWordId, probability DESC)
     *
     * @param word the preceding word string as typed by the user
     * @param topN the number of suggestions to return
     * @return list of Prediction records sorted by probability descending;
     *         empty list if the word is not in the vocabulary
     * @throws SQLException if the query fails
     */
    public List<Prediction> getPredictions(String word, int topN) throws SQLException {
        String sql = """
            SELECT w2.wordId, w2.word, wt.probability
            FROM word_transitions wt
            JOIN words w1 ON wt.firstWordId  = w1.wordId
            JOIN words w2 ON wt.secondWordId = w2.wordId
            WHERE w1.word = ?
            ORDER BY wt.probability DESC
            LIMIT ?
            """;

        List<Prediction> predictions = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, word);
            stmt.setInt(2, topN);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    predictions.add(new Prediction(
                        rs.getString("word"),
                        rs.getInt("wordId"),
                        rs.getDouble("probability")
                    ));
                }
            }
        }
        return predictions;
    }

    /**
     * Looks up the database ID for a given word string.
     *
     * @param word the word string to look up
     * @return an Optional containing the wordId, or empty if the word is not in the vocabulary
     * @throws SQLException if the query fails
     */
    public Optional<Integer> getWordId(String word) throws SQLException {
        String sql = "SELECT wordId FROM words WHERE word = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, word);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getInt("wordId"));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns whether the given word string exists in the vocabulary.
     *
     * @param word the word string to check
     * @return true if the word is present in the words table, false otherwise
     * @throws SQLException if the query fails
     */
    public boolean isKnownWord(String word) throws SQLException {
        return getWordId(word).isPresent();
    }

    /**
     * Inserts a new word into the words table and returns its generated ID.
     *
     * If the word already exists, the existing ID is returned and no insert is performed.
     *
     * @param word the word string to add
     * @return the wordId of the newly inserted or already-existing word
     * @throws SQLException if the insert fails or no generated key is returned
     */
    public int addNewWord(String word) throws SQLException {
        Optional<Integer> existing = getWordId(word);
        if (existing.isPresent()) return existing.get();
        String sql = "INSERT INTO words (word, totalOccurrence, startCount, endCount) VALUES (?, 1, 0, 0)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, word);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Insert of new word succeeded but no generated key was returned: " + word);
    }

    /**
     * Inserts a transition between two words, or increments its count if it already exists.
     *
     * Uses MySQL's ON DUPLICATE KEY UPDATE to atomically upsert the transition count.
     * This requires a UNIQUE constraint on (firstWordId, secondWordId) in word_transitions.
     *
     * @param firstWordId  the database ID of the preceding word
     * @param secondWordId the database ID of the following word
     * @throws SQLException if the upsert fails
     */
    public void addOrIncrementTransition(int firstWordId, int secondWordId) throws SQLException {
        String sql = """
            INSERT INTO word_transitions (firstWordId, secondWordId, count, probability)
            VALUES (?, ?, 1, 0.0)
            ON DUPLICATE KEY UPDATE count = count + 1
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, firstWordId);
            stmt.setInt(2, secondWordId);
            stmt.executeUpdate();
        }
    }

    /**
     * Returns all words from the words table, ordered by total occurrence descending.
     *
     * Useful for inspecting the most frequent vocabulary items without loading the full model.
     *
     * @return list of WordRow records sorted by totalOccurrence DESC
     * @throws SQLException if the query fails
     */
    public List<WordRow> getWordsSortedByFrequency() throws SQLException {
        List<WordRow> words = new ArrayList<>();
        String sql = "SELECT wordId, word, totalOccurrence, startCount, endCount "
                   + "FROM words ORDER BY totalOccurrence DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                words.add(new WordRow(
                    rs.getInt("wordId"),
                    rs.getString("word"),
                    rs.getInt("totalOccurrence"),
                    rs.getInt("startCount"),
                    rs.getInt("endCount")
                ));
            }
        }
        return words;
    }

    /**
     * Closes the JDBC connection if it is still open.
     *
     * Always call this when done, or use DBInterface in a try-with-resources block.
     * Calling close() on an already-closed connection is a no-op.
     */
    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
