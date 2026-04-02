package com.cs4485.model;

import java.sql.SQLException;
import java.util.*;

/**
 * Command-line entry point for training the model against the shared database.
 *
 * Loads the latest counts, trains the model, and writes the resulting
 * probabilities back into word_transitions.
 *
 * Typical usage:
 *
 *   # Train with the default smoothing method (Kneser-Ney)
 *   java -jar model.jar com.cs4485.model.ModelTrainer --host localhost --database sentencebuilder \
 *       --user root --password secret
 *
 *   # Compare all smoothing methods and pick the best one
 *   java -jar model.jar com.cs4485.model.ModelTrainer --host localhost --database sentencebuilder \
 *       --user root --password secret --evaluate
 *
 * Command-line arguments (all optional if environment variables are set):
 *   --host      MySQL server hostname (falls back to DB_HOST env var)
 *   --database  Database schema name  (falls back to DB_NAME env var)
 *   --user      MySQL username        (falls back to DB_USER env var)
 *   --password  MySQL password        (falls back to DB_PASSWORD env var)
 *   --smoothing Smoothing method: none, laplace, add_k, kneser_ney (default: kneser_ney)
 *   --k         Add-k smoothing parameter (default: 1.0; only used with --smoothing add_k)
 *   --evaluate  If present, compares all four smoothing methods by perplexity before training
 */
public class ModelTrainer {

    public static void main(String[] args) {
        Map<String, String> params = parseArgs(args);

        // Let command-line args override the environment if both are present.
        String host     = resolve(params, "host",     "DB_HOST");
        String database = resolve(params, "database", "DB_NAME");
        String user     = resolve(params, "user",     "DB_USER");
        String password = resolve(params, "password", "DB_PASSWORD");

        if (host == null || database == null || user == null || password == null) {
            System.err.println("Error: database connection details are missing.");
            System.err.println("Provide --host, --database, --user, --password on the command line,");
            System.err.println("or set DB_HOST, DB_NAME, DB_USER, DB_PASSWORD as environment variables.");
            System.exit(1);
        }

        String smoothingLabel = params.getOrDefault("smoothing", "kneser_ney");
        double k              = Double.parseDouble(params.getOrDefault("k", "1.0"));
        boolean evaluate      = params.containsKey("evaluate");

        SmoothingMethod smoothingMethod;
        try {
            smoothingMethod = SmoothingMethod.fromLabel(smoothingLabel);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        try (DBInterface db = new DBInterface(host, database, user, password)) {

            System.out.println("Loading training data from database...");
            long loadStart = System.currentTimeMillis();
            List<WordRow>       words       = db.loadWords();
            List<TransitionRow> transitions = db.loadTransitions();
            long loadTime = System.currentTimeMillis() - loadStart;

            System.out.printf("Loaded %,d words and %,d transitions in %d ms%n",
                words.size(), transitions.size(), loadTime);

            // If --evaluate is set, compare the smoothing methods before training.
            if (evaluate) {
                runEvaluation(words, transitions, k);
            }

            System.out.printf("%nTraining with %s smoothing...%n", smoothingLabel);
            long trainStart = System.currentTimeMillis();

            BigramModel model = new BigramModel(smoothingMethod, k, 0.75);
            model.train(words, transitions);

            long trainTime = System.currentTimeMillis() - trainStart;

            System.out.println("Computing smoothed probabilities...");
            Map<int[], Double> probabilities = model.computeAllProbabilities();

            System.out.println("Writing probabilities to database...");
            db.updateProbabilities(probabilities);

            // Print a quick training summary at the end.
            System.out.println();
            System.out.println("=== Training Complete ===");
            System.out.printf("Vocabulary size:    %,d unique words%n",  model.getVocabularySize());
            System.out.printf("Bigram transitions: %,d unique pairs%n",  probabilities.size());
            System.out.printf("Total bigram tokens:%,d occurrences%n",   model.getTotalBigrams());
            System.out.printf("Training time:      %d ms%n",             trainTime);
            System.out.printf("Smoothing method:   %s%n",                smoothingLabel);

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Runs a simple 90/10 evaluation across all smoothing methods.
     *
     * Lower perplexity means the model is doing a better job on unseen transitions.
     */
    private static void runEvaluation(List<WordRow> words, List<TransitionRow> transitions, double k) {
        int splitIdx = (int)(transitions.size() * 0.9);
        List<TransitionRow> trainSet = transitions.subList(0, splitIdx);
        List<TransitionRow> testSet  = transitions.subList(splitIdx, transitions.size());

        System.out.println();
        System.out.println("=== Smoothing Method Comparison (90/10 split) ===");
        System.out.printf("%-15s  %12s%n", "Method", "Perplexity");
        System.out.println("-".repeat(30));

        for (SmoothingMethod method : SmoothingMethod.values()) {
            // Only ADD_K uses the supplied k value.
            double kValue = (method == SmoothingMethod.ADD_K) ? k : 1.0;
            BigramModel model = new BigramModel(method, kValue, 0.75);
            model.train(words, trainSet);
            double ppl = model.computePerplexity(testSet);
            System.out.printf("%-15s  %12.2f%n", method.getLabel(), ppl);
        }
    }

    /**
     * Returns the value for a parameter by checking command-line args first,
     * then falling back to the named environment variable.
     * Returns null if neither is set.
     */
    private static String resolve(Map<String, String> params, String argName, String envName) {
        if (params.containsKey(argName)) return params.get(argName);
        return System.getenv(envName);
    }

    /**
     * Parses command-line arguments of the form --key value or --flag into a map.
     * Flag arguments (no following value) are stored with an empty string as the value.
     */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i].substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    params.put(key, args[++i]);
                } else {
                    params.put(key, "");
                }
            }
        }
        return params;
    }
}
