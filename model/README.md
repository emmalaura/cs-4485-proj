# Model — Bigram Language Model

Handles training, probability computation, and next-word prediction for the CS 4485 Sentence Builder.

For design decisions and architecture notes, see [DECISIONS.md](DECISIONS.md).

---

## Prerequisites

- Java 17+
- Maven 3.6+

---

## Environment Variables

Required to connect to the shared MySQL database:

| Variable      | Description                          |
|---------------|--------------------------------------|
| `DB_HOST`     | Hostname or IP of the MySQL server   |
| `DB_NAME`     | Database schema name                 |
| `DB_USER`     | MySQL username                       |
| `DB_PASSWORD` | MySQL password                       |

---

## Build

```bash
cd model
mvn package -DskipTests
```

Produces `target/model-1.0-SNAPSHOT.jar` with all dependencies bundled.

---

## Run Tests (no DB needed)

```bash
mvn test
```

All 35 unit tests use hardcoded data — no database connection required.

---

## Train the Model

Run this after the Data-Processing team has populated the `words` and `word_transitions` tables:

```bash
java -cp target/model-1.0-SNAPSHOT.jar com.cs4485.model.ModelTrainer \
  --host $DB_HOST --database $DB_NAME --user $DB_USER --password $DB_PASSWORD \
  --smoothing kneser_ney
```

**Optional flags:**

| Flag          | Values                                      | Default       |
|---------------|---------------------------------------------|---------------|
| `--smoothing` | `none`, `laplace`, `add_k`, `kneser_ney`    | `kneser_ney`  |
| `--k`         | Any decimal (add-k only)                    | `1.0`         |
| `--evaluate`  | (no value) — compares all 4 methods by perplexity before training | off |

Training writes computed probabilities back to `word_transitions.probability`.

---

## Smoke Test Against the DB

Verifies connectivity and prints row counts. Optionally shows next-word predictions for a seed word:

```bash
java -cp target/model-1.0-SNAPSHOT.jar com.cs4485.model.ModelIntegrationCheck \
  [seed_word] [topN]
```

Example:
```bash
java -cp target/model-1.0-SNAPSHOT.jar com.cs4485.model.ModelIntegrationCheck algorithm 5
```

---

## UI / JavaFX Integration

Add the JAR to your project classpath. Two integration paths are available via `ModelPredictor`:

### Option A — Fast path (real-time autocomplete)

Queries the DB directly using pre-computed probabilities. No model needs to be loaded into memory.

```java
DBInterface db = new DBInterface(host, dbName, user, password);
List<Prediction> suggestions = ModelPredictor.getNextWords(db, "algorithm", 5);

for (Prediction p : suggestions) {
    System.out.println(p.word() + " (" + p.probability() + ")");
}
db.close();
```

Requires training to have been run first. Best for keystroke-level autocomplete.

### Option B — In-memory model (multi-word generation)

Loads the full model into memory and generates a sentence continuation. Better when the user has typed several words.

```java
DBInterface db = new DBInterface(host, dbName, user, password);
BigramModel model = new BigramModel(SmoothingMethod.KNESER_NEY);
model.train(db.loadWords(), db.loadTransitions());

List<String> sentence = ModelPredictor.completeSentence(
    model, "the algorithm", 10, GenerationStrategy.BEAM);

System.out.println(String.join(" ", sentence));
db.close();
```

**Generation strategies:**

| Strategy         | Description                                           |
|------------------|-------------------------------------------------------|
| `GREEDY`         | Always picks the most probable next word. Fast.       |
| `BEAM`           | Explores top-5 candidate paths, returns the best one. Recommended. |
| `SAMPLE`         | Randomly samples from the distribution. Most varied output. |

Input is normalized to lowercase automatically — pass user input as-is.

---

## DB Team Notes

**Columns the model reads and writes:**

| Table              | Columns read                                          | Column written  |
|--------------------|-------------------------------------------------------|-----------------|
| `words`            | `wordId`, `word`, `totalOccurrence`, `startCount`, `endCount` | —         |
| `word_transitions` | `transitionId`, `firstWordId`, `secondWordId`, `count` | `probability`  |

**Recommended index** — add this to avoid full table scans on every autocomplete query:

```sql
CREATE INDEX idx_prob ON word_transitions (firstWordId, probability DESC);
```
