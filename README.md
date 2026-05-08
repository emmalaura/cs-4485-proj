# CS-4485 Project — Auto Glossary

A JavaFX-based sentence builder with autocomplete powered by a Bigram Language Model trained on technical textbooks.

---

## Prerequisites

- Java JDK 25 (Temurin recommended)
- Apache Maven 3.9.6
- JavaFX SDK 26
- MySQL 8.x
- IntelliJ IDEA

---

## Database Setup — macOS

### 1. Install MySQL
If you have Homebrew:
```bash
brew install mysql
brew services start mysql
```

### 2. Secure Your Installation
```bash
mysql_secure_installation
```
Answer the prompts as follows:
- Validate password? → **N**
- Set root password → **cs4485project** (or your own)
- Remove anonymous users? → **Y**
- Disallow root login remotely? → **Y**
- Remove test database? → **Y**
- Reload privilege tables? → **Y**

### 3. Log In and Create the Database
```bash
mysql -u root -p
```
Then run these commands:
```sql
CREATE DATABASE CS4485DB;
CREATE USER 'javauser'@'localhost' IDENTIFIED BY 'cs4485';
GRANT ALL PRIVILEGES ON CS4485DB.* TO 'javauser'@'localhost';
FLUSH PRIVILEGES;
exit
```

### 4. Load the Schema
Exit MySQL and run this in your terminal:
```bash
mysql -u root -p < sql/schema.sql
```
Enter password: `cs4485`

### 5. Verify Tables
```bash
mysql -u javauser -p CS4485DB
```
```sql
SHOW TABLES;
```
You should see all project tables listed.

---

## Database Setup — Windows

### 1. Install MySQL
Download and run the MySQL installer from [mysql.com](https://dev.mysql.com/downloads/installer/).
Select **Developer Default** and set your root password when prompted.

### 2. Add MySQL to PATH
- Open **System Properties** → **Environment Variables**
- Under **System Variables**, find **Path** and click **Edit**
- Click **New** and add your MySQL bin path, for example:
```
C:\Program Files\MySQL\MySQL Server 8.0\bin
```

### 3. Create the Database
Open **MySQL Command Line Client**, enter your root password, then run:
```sql
CREATE DATABASE CS4485DB;
CREATE USER 'javauser'@'localhost' IDENTIFIED BY 'cs4485';
GRANT ALL PRIVILEGES ON CS4485DB.* TO 'javauser'@'localhost';
FLUSH PRIVILEGES;
exit
```

### 4. Load the Schema
Open a regular Command Prompt and run:
```bash
mysql -u root -p < sql/schema.sql
```
Enter password: `cs4485`

### 5. Verify Tables
```bash
mysql -u javauser -p CS4485DB
```
```sql
SHOW TABLES;
```
You should see all project tables listed.

---

## JavaFX Setup — IntelliJ

### 1. Download JavaFX SDK
Download JavaFX SDK 26 from [gluonhq.com](https://gluonhq.com/products/javafx/).
- macOS Apple Silicon → choose **aarch64**
- macOS Intel / Windows → choose **x64**

Extract it somewhere permanent, for example:
- macOS: `/Users/yourname/javafx-sdk-26`
- Windows: `C:\javafx-sdk-26`

### 2. Add JavaFX as a Library in IntelliJ
- Go to **File → Project Structure → Libraries**
- Click **+** → **Java**
- Navigate to your JavaFX SDK `lib` folder and select it
- Click **OK** and apply to your module

### 3. Configure VM Options
- Go to **Run → Edit Configurations**
- Click **Modify options → Add VM options**
- Paste the following (update the path to match your SDK location):

macOS:
```
--module-path "/Users/yourname/javafx-sdk-26/lib" --add-modules javafx.controls,javafx.fxml
```
Windows:
```
--module-path "C:\javafx-sdk-26\lib" --add-modules javafx.controls,javafx.fxml
```

---

## Build & Run

### 1. Build the Project
```bash
mvn clean install -DskipTests
```

### 2. Verify Database Connection
Run `dbConnection.java` — you should see:
```
DB CONNECTED!
```

### 3. Run the App
```bash
cd app
mvn javafx:run
```
Or run `MainApp.java` directly in IntelliJ.

---

## Database Sync

If your local database differs from the team's latest, import the shared dump:
```bash
mysql -u javauser -p CS4485DB < db_dump.sql
```

If you have processed and added new data, export your database:
```bash
mysqldump -u javauser -p --no-tablespaces CS4485DB > db_dump.sql
```
Then commit `db_dump.sql` to the repo so teammates can sync.

---

## Project Structure

```
cs-4485-proj/
├── app/                        # JavaFX frontend + database connection classes
│   └── src/main/java/com/sentencebuilder/
│       ├── MainApp.java
│       ├── HomeController.java
│       ├── SentenceGenController.java
│       ├── AutoCompleteController.java
│       ├── ReportsController.java
│       ├── ImportController.java
│       ├── ChatHistoryManager.java
│       ├── ThemeManager.java
│       ├── UIUtils.java
│       └── BaseController.java
├── model/                      # Bigram model, DBInterface, ModelPredictor
├── sql/                        # MySQL schema and dumps
└── pom.xml
```

---

## Team Branches

| Branch | Purpose |
|---|---|
| `javafx` | Frontend UI |
| `integration` | Main integration branch |
| `data-processing` | Text file processing |
| `bigram-model` | Language model |
| `mysql` | Database connections |
