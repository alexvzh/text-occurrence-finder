# Text Occurrence Finder

A simple Kotlin command-line tool that searches through all files in a directory for a given string.  
It works concurrently across files and safely skips unreadable (binary) files.

---

## Features

- Scans directories recursively  
- Works concurrently using Kotlin coroutines  
- Handles all file types, skipping unreadable ones automatically  
- Prints file name, line, and offset of each match  
- Includes a complete test suite with JUnit and coroutine tests  

---



## Requirements

- Java 17 or newer  
- Gradle 8 or newer (wrapper included)  
- Kotlin 2.0 or newer  

---

## How to Run

Run the program directly with Gradle:

```bash
./gradlew run --args="/path/to/directory searchString"
```

## Testing

The project uses **JUnit 5** and **kotlinx-coroutines-test** for unit testing.  
All tests are located in `src/test/kotlin/SearchTests.kt`.

To run all tests from the command line:

```bash
./gradlew test
```




