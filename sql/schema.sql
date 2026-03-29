CREATE TABLE words (
    wordId INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(100) NOT NULL UNIQUE,
    totalOccurrence INT UNSIGNED NOT NULL DEFAULT 0,
    startCount INT UNSIGNED NOT NULL DEFAULT 0,
    endCount INT UNSIGNED NOT NULL DEFAULT 0,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idxTotalOccurrence (totalOccurrence)
);

CREATE TABLE word_transitions (
    transitionId INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    firstWordId INT UNSIGNED NOT NULL,
    secondWordId INT UNSIGNED NOT NULL,
    count INT UNSIGNED NOT NULL DEFAULT 1,
    probability DECIMAL(10, 8) NOT NULL DEFAULT 0.00000000,
    UNIQUE KEY uqTransition (firstWordId, secondWordId),
    FOREIGN KEY (firstWordId) REFERENCES words(wordId),
    FOREIGN KEY (secondWordId) REFERENCES words(wordId),
    INDEX idxFirstWordId (firstWordId),
    INDEX idxSecondWordId (secondWordId)
);

CREATE TABLE imported_files (
    fileId INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    fileName VARCHAR(255) NOT NULL,
    filePath VARCHAR(1024),
    fileSizeBytes BIGINT UNSIGNED,
    wordCount INT UNSIGNED NOT NULL DEFAULT 0,
    sentenceCount INT UNSIGNED NOT NULL DEFAULT 0,
    uniqueWords INT UNSIGNED NOT NULL DEFAULT 0,
    checksum CHAR(64),
    importedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uqChecksum (checksum)
);