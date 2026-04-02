package com.cs4485.model;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * Small command-line smoke test for validating the shared database integration.
 *
 * Usage:
 *   java -cp model.jar com.cs4485.model.ModelIntegrationCheck
 *   java -cp model.jar com.cs4485.model.ModelIntegrationCheck algorithm 5
 *
 * Reads DB_HOST, DB_NAME, DB_USER, and DB_PASSWORD from the environment.
 */
public class ModelIntegrationCheck {

    private ModelIntegrationCheck() {}

    public static void main(String[] args) {
        String seedWord = args.length >= 1 ? normalize(args[0]) : null;
        int topN = args.length >= 2 ? Integer.parseInt(args[1]) : 5;

        try (DBInterface db = DBInterface.fromEnvironment()) {
            List<WordRow> words = db.loadWords();
            List<TransitionRow> transitions = db.loadTransitions();

            System.out.println("=== Database Integration Check ===");
            System.out.printf("Loaded %,d words%n", words.size());
            System.out.printf("Loaded %,d transitions%n", transitions.size());

            if (seedWord != null) {
                List<Prediction> predictions = ModelPredictor.getNextWords(db, seedWord, topN);
                System.out.printf("%nTop %d predictions for \"%s\":%n", topN, seedWord);
                if (predictions.isEmpty()) {
                    System.out.println("  No predictions found.");
                } else {
                    for (Prediction prediction : predictions) {
                        System.out.printf("  %s (id=%d, p=%.6f)%n",
                            prediction.word(), prediction.wordId(), prediction.probability());
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database integration check failed: " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid arguments: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Seed word must not be blank.");
        }
        return normalized;
    }
}
