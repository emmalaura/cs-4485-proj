Model Developer Notes
CS 4485 - Bigram Sentence Builder
=====================================

WHAT HAS BEEN BUILT
--------------------

model/pom.xml
  Maven build file. Declares Java 17 as the language version, adds the MySQL JDBC
  driver (mysql-connector-j 8.3.0) for database access, and JUnit 5.10.2 for testing.
  The maven-shade-plugin is configured to bundle all dependencies into a single JAR
  so ModelTrainer and ModelPredictor can be run on the command line without a classpath.

src/main/java/com/cs4485/model/SmoothingMethod.java
  Enum listing the four supported smoothing algorithms: NONE (MLE), LAPLACE, ADD_K,
  and KNESER_NEY. Each value has a string label (e.g., "kneser_ney") used on the
  command line. Includes a fromLabel() factory method for command-line parsing.

src/main/java/com/cs4485/model/GenerationStrategy.java
  Enum for the three sentence generation strategies: GREEDY, BEAM, and SAMPLE.
  Used as a parameter to BigramModel.generateSentence() and ModelPredictor.completeSentence().

src/main/java/com/cs4485/model/WordRow.java
  Java record mapping one row from the words database table. Fields match the column
  names exactly: wordId, word, totalOccurrence, startCount, endCount.

src/main/java/com/cs4485/model/TransitionRow.java
  Java record mapping one row from the word_transitions database table. Fields:
  transitionId, firstWordId, secondWordId, count. The probability
  column is intentionally excluded because it is an output of training, not an input.

src/main/java/com/cs4485/model/Prediction.java
  Java record representing one autocomplete suggestion: word (string), wordId (int),
  and probability (double). Returned by BigramModel.predictNext() and
  DBInterface.getPredictions(). Passed to the UI team to display suggestions.

src/main/java/com/cs4485/model/BigramModel.java
  The core model class. Trains on word and transition data loaded from the database,
  computes conditional probabilities P(word2 | word1) using the selected smoothing
  method, and supports sentence generation and perplexity evaluation.
  See the full method list in the class Javadoc.

src/main/java/com/cs4485/model/DBInterface.java
  JDBC layer that connects to the MySQL database. Provides loadWords(), loadTransitions(),
  updateProbabilities() (batch UPDATE on word_transitions.probability), getPredictions()
  for direct UI queries, getWordId(), isKnownWord(), addNewWord() (inserts a new word and
  returns its generated ID), addOrIncrementTransition() (MySQL upsert on transition count),
  and getWordsSortedByFrequency(). Implements AutoCloseable for safe use in
  try-with-resources blocks.

src/main/java/com/cs4485/model/ModelPredictor.java
  Public API for the UI (JavaFX) team. Two static methods:
    - getNextWords(db, word, topN): queries the DB directly; no model in memory needed.
    - completeSentence(model, partialSentence, maxLength, strategy): uses an in-memory model.

src/main/java/com/cs4485/model/ModelTrainer.java
  Command-line entry point for training. Loads data from the DB, trains the model,
  optionally compares all four smoothing methods by perplexity, writes computed
  probabilities back to word_transitions.probability, and prints a metrics summary.

src/main/java/com/cs4485/model/ModelIntegrationCheck.java
  Small command-line smoke test for the shared MySQL database. Verifies that the
  schema is reachable, loads row counts through DBInterface, and optionally prints
  next-word predictions for a supplied seed word.

src/test/java/com/cs4485/model/BigramModelTest.java
  JUnit 5 unit tests for BigramModel. Uses hardcoded test data; no DB connection needed.
  Covers training, all four smoothing methods, predictNext(), generateSentence() with
  all three strategies, perplexity, computeAllProbabilities(), and latency tracking.

src/test/java/com/cs4485/model/ModelPredictorTest.java
  JUnit 5 unit tests for ModelPredictor.completeSentence(). Uses hardcoded test data.
  Tests null/blank input, output length, seed word preservation, and all strategies.


DESIGN DECISIONS AND WHY
--------------------------

Java records for data classes (WordRow, TransitionRow, Prediction)
  Records give immutable data carriers with auto-generated equals(), hashCode(), toString(),
  and accessor methods without boilerplate. They enforce that the DB row data is never
  accidentally mutated after loading.

Kneser-Ney as the default smoothing method
  Laplace (add-1) smoothing is simple but over-smooths: it gives too much probability to
  rare words just because the vocabulary is large. Kneser-Ney uses a "continuation
  probability" that rewards words appearing in many different contexts rather than simply
  frequent words. For a technical corpus with lots of domain-specific words, this produces
  better predictions. The --evaluate flag in ModelTrainer lets you verify this by comparing
  perplexity scores across all four methods on your actual data.

In-memory model + DB update, not storing counts back to DB
  The model trains entirely in memory and writes only the final probability values back to
  the database. This avoids race conditions with the Data Processing team's inserts and
  keeps the training pipeline simple: read counts, compute probabilities, write probabilities.

Two prediction paths for the UI team
  ModelPredictor.getNextWords() queries the DB directly using the pre-computed probability
  column, which is fast and requires no Java model object in memory. This is best for
  real-time keystroke-level autocomplete. ModelPredictor.completeSentence() loads the
  full model into memory and uses beam search, which is better when the user has typed
  several words and wants a longer multi-word suggestion. Both prediction paths now
  normalize user input to lowercase to match the data-processing team's tokenization.

Beam search as the recommended generation strategy
  Greedy is fast but deterministic and can produce repetitive output. Sampling is varied
  but unpredictable. Beam search (width 5) keeps the five most likely candidate sentences
  at each step and returns the best one, producing coherent output while still exploring
  alternatives. This is the strategy recommended for the UI autocomplete feature.

Plain JDBC over an ORM (Hibernate)
  Hibernate would add significant complexity and a large dependency for what amounts to
  a few simple SQL queries. Plain JDBC with PreparedStatements gives full control over
  the SQL and makes the queries transparent to the DB team.


WHAT STILL NEEDS TO BE DONE
-----------------------------

1. Integration testing against the real database
   BigramModelTest and ModelPredictorTest use hardcoded data. Once the database is live,
   run ModelIntegrationCheck and ModelTrainer against it and manually verify predictions
   make sense for the corpus.

2. Confirm the data-processing import path into MySQL
   The Data-Processing branch is now aligned with the current schema in that it no longer
   depends on the removed word_file_occurrences table. It still produces CSV outputs, so
   the team should confirm the exact import step that loads those CSVs into the shared
   MySQL tables (words, word_transitions, imported_files).

3. Recommend this index to the DB team for better UI query performance
   Without an index on (firstWordId, probability), getPredictions() will do a full table
   scan of word_transitions for every autocomplete suggestion. Ask them to add:
     CREATE INDEX idx_prob ON word_transitions (firstWordId, probability DESC);

4. Verify the probability values produced by the data-processing pipeline
   The model code now treats the database schema as the source of truth, but the team
   should still validate that any precomputed probabilities or counts coming from the
   data-processing pipeline match the intended interpretation of P(nextWord | currentWord).

5. ModelTrainer needs a scheduled re-training mechanism
   Right now training is a one-shot CLI run. If new documents are imported into the corpus
   after the initial training, ModelTrainer must be re-run to update the probabilities.
   The team should decide: manual re-run, or a scheduled job, or trigger on import.


ALTERNATIVES CONSIDERED BUT NOT CHOSEN
----------------------------------------

Trigram model (instead of bigram)
  A trigram model conditions on the previous two words: P(w3 | w1, w2). This generally
  produces more coherent sentences than a bigram model but requires significantly more
  data because three-word sequences are much sparser than two-word sequences. Given the
  corpus size constraints for a student project, a bigram model is more appropriate.
  The architecture of this code could be extended to trigrams if the corpus grows.

Storing the full model as a JSON blob in a separate table
  An alternative design would serialize the entire BigramModel (all counts and probabilities)
  as a single JSON record and store it in a new model_versions table. This would allow
  easy versioning and rollback. The design was not chosen because: (1) the DB team's schema
  already stores individual bigram probabilities in word_transitions, which is what the UI
  needs for direct queries; (2) a JSON blob would require loading the entire model to answer
  even a single prediction query; (3) it would require a separate table not in the agreed schema.

Hibernate ORM
  Hibernate would generate SQL from annotated Java entities and handle connection pooling.
  For the number of queries in this model (two reads and one bulk update), plain JDBC is
  simpler, more transparent to the DB team, and avoids a large framework dependency.

Good-Turing smoothing
  Good-Turing estimates probabilities for unseen events by counting how often counts
  occur (the "frequency of frequencies"). It is theoretically well-motivated but
  computationally fiddly and less standard than Kneser-Ney. Kneser-Ney is preferred
  because it is widely supported in NLP literature and performs well in practice.

Temperature-based sampling
  The SAMPLE strategy currently samples directly from the probability distribution.
  A temperature parameter (dividing log-probabilities by T before normalizing) would
  allow controlling the randomness: T < 1 makes the model more deterministic, T > 1
  makes it more random. This was omitted to keep the API simple; it can be added later
  if the UI team wants more control over suggestion variety.
