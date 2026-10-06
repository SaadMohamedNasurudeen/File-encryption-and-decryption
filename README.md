# File Encryption & Decryption System

A secure, high-performance desktop application for encrypting and decrypting files using authenticated **AES-256-GCM** encryption, **PBKDF2** key derivation, and a modern **Swing/AWT** dashboard interface. Built strictly using the Java Standard Library (`javax.crypto`, `java.security`), JDK 17+, and Maven.

---

## Tech Stack Used

| Layer / Component | Technology | Description |
| :--- | :--- | :--- |
| **Language & Platform** | **Java (JDK 17+)** | Built on Java 17+ standards; compatible with OpenJDK and Oracle JDK 18. |
| **Build & Dependency Tool** | **Apache Maven (3.8+)** | Dependency management, compiler configuration, packaging, and test runner. |
| **Cryptographic Engine** | **Java Cryptography (JCA / JCE)** | Standard library only (`javax.crypto`, `java.security`) — zero third-party crypto dependencies. |
| **Cipher & Mode** | **AES-256 in GCM Mode** | `AES/GCM/NoPadding` with a 128-bit authentication tag (NIST SP 800-38D). |
| **Key Derivation (KDF)** | **PBKDF2-HMAC-SHA256** | 600,000 iterations with random 16-byte salt (OWASP & NIST compliant). |
| **Random Number Generator** | **SecureRandom** | CSPRNG generating unique 16-byte salts and 12-byte IVs for each file. |
| **Streaming Pipeline** | **Cipher Streams & NIO** | `CipherInputStream` / `CipherOutputStream` with 64 KB buffers and atomic file moves via `java.nio.file.Files`. |
| **GUI Framework** | **Java Swing & AWT** | Pure desktop UI (`javax.swing`, `java.awt`); no JavaFX or external UI libraries. |
| **GUI Concurrency** | **SwingWorker** | Background thread execution keeping the Event Dispatch Thread (EDT) 100% responsive. |
| **Drag & Drop** | **AWT DnD API** | `java.awt.dnd.DropTarget` supporting file drops on both drop zones and text fields. |
| **Unit Testing** | **JUnit 5 (Jupiter)** | Unit and integration testing (`org.junit.jupiter:junit-jupiter:5.10.2`). |
| **Launchers** | **Batch & VBScript** | Windows zero-click desktop launchers (`run.bat`, `Launch-App.vbs`). |

---

## Flow of Working

### 1. High-Level Architecture Flow

```
                      +---------------------------------------+
                      |         DashboardFrame (GUI)          |
                      |  - Encrypt Tab   - Decrypt Tab        |
                      |  - History Tab   - Stat Badges        |
                      +-------------------+-------------------+
                                          |
                        Spawns Background | SwingWorker (EDT Free)
                                          v
                      +---------------------------------------+
                      |          CryptoService (Core)         |
                      |  - AES-256 GCM    - PBKDF2 (600,000)  |
                      |  - Streaming I/O  - Memory Sanitizer  |
                      +-------------------+-------------------+
                                          |
                      +-------------------+-------------------+
                      |                                       |
                      v                                       v
         [ ENCRYPTION PIPELINE ]                 [ DECRYPTION PIPELINE ]
         1. Validate Inputs                      1. Read Salt (16B) & IV (12B)
         2. Generate Salt & IV                   2. Derive Key (PBKDF2 600K)
         3. Derive 256-bit Key                   3. Stream to .part Temp File
         4. Write [Salt][IV] Header              4. Verify GCM Tag on Stream EOF
         5. Stream Ciphertext + Tag              5. Success -> Move to Destination
         6. Save as *.enc File                      Failure -> Delete Temp File
```

---

### 2. Encryption Workflow (Step-by-Step)

```
[User Selects File] ──► [Drag & Drop or JFileChooser]
                              │
                              ▼
[Input Validation]  ──► File exists? Password >= 8 chars? Passwords match?
                              │ (If invalid: show JOptionPane error)
                              ▼
[Lock UI & Start]   ──► Disable action buttons, set cursor to wait, launch SwingWorker
                              │
                              ▼
[Key Derivation]    ──► SecureRandom generates 16-byte Salt + 12-byte IV
                        PBKDF2WithHmacSHA256 (600,000 iterations) -> 256-bit AES Key
                              │
                              ▼
[File Header Write] ──► Write [Salt (16 bytes)][IV (12 bytes)] directly to output stream
                              │
                              ▼
[Streaming Cipher]  ──► Read source file in 64 KB chunks through CipherOutputStream
                        Update ProgressListener -> GUI ProgressBar (0% to 100%)
                        Append 16-byte GCM Authentication Tag at stream closure
                              │
                              ▼
[Sanitization]      ──► Wipe char[] password and raw key bytes with null characters ('\0')
                              │
                              ▼
[Completion]        ──► Re-enable UI, increment Encrypted stat card, append row to History JTable
```

---

### 3. Decryption Workflow (Step-by-Step)

```
[User Selects .enc] ──► [Drag & Drop or JFileChooser with .enc Filter]
                              │
                              ▼
[Input Validation]  ──► File length >= 44 bytes (28B header + 16B tag)? Password entered?
                              │
                              ▼
[Lock UI & Start]   ──► Launch SwingWorker, start Progress Bar
                              │
                              ▼
[Header Extraction] ──► Read Salt (bytes 0–15) and IV (bytes 16–27)
                        PBKDF2WithHmacSHA256 (600,000 iterations) -> 256-bit AES Key
                              │
                              ▼
[Staged Decryption] ──► Create staging temporary file: ".dec_tmp_*.part" in target directory
                        Stream ciphertext through CipherInputStream in 64 KB chunks
                        Write unverified plaintext ONLY into the temporary file
                              │
                              ▼
[Authentication Tag Verification]
        │
        ├─► [TAG MISMATCH / BAD PASSWORD / TAMPERED]
        │       1. Catch AEADBadTagException (or wrapped IOException)
        │       2. Delete staging ".dec_tmp_*.part" immediately (no partial files remain)
        │       3. Wipe password memory ('\0')
        │       4. Increment Failures stat card, update History as "Failed"
        │       5. Alert user: "Wrong password or file was tampered with"
        │
        └─► [AUTHENTICATION SUCCEEDED]
                1. GCM integrity check passes
                2. Files.move(tempFile, destinationFile, REPLACE_EXISTING)
                3. Wipe password memory ('\0')
                4. Increment Decrypted stat card, update History as "Success"
                5. Show success dialog with restored file path
```

---

## File Format Specification

Encrypted files are saved with the `.enc` extension using the binary layout:

```
+---------------------------------------------------------------------------------------+
|  Salt (16 bytes)  |  IV (12 bytes)  |  Ciphertext (N bytes)  |  GCM Tag (16 bytes)    |
+---------------------------------------------------------------------------------------+
|<---------- 28-byte Header --------->|<---------------- Encrypted Payload ------------>|
```

1. **Salt (bytes 0–15)**: 16 bytes of cryptographically secure random data used in PBKDF2 key derivation.
2. **IV (bytes 16–27)**: 12 bytes (96 bits) of random data ensuring ciphertext uniqueness across identical plaintexts.
3. **Ciphertext & GCM Tag (bytes 28+)**: The encrypted content followed by the 128-bit integrity authentication tag appended automatically by GCM mode.

---

## Key Features

- **Modern Dashboard GUI (Swing & AWT)**:
  - Tabbed interface: **Encrypt File**, **Decrypt File**, and **Activity History**.
  - Interactive file selection with file metadata panel showing filename, size, and destination format.
  - Password fields with confirmation validation and show/hide password toggles.
  - Real-time `JProgressBar` with percentage indicators and dynamic status messages.
  - Audit trail `JTable` recording file names, actions (Encrypt/Decrypt), timestamps, and status badges (Success/Failed).
  - Background asynchronous execution using `SwingWorker` to keep the UI responsive.
  - Input validation with `JOptionPane` error dialogs (file existence, password length >= 8 characters, password matching).
  - Live **Password Strength Meter** (Weak / Medium / Strong colored indicator).
  - Live **Header Stat Badges** tracking Encrypted, Decrypted, and Failure counts.
  - Advanced **Drag-and-Drop** support for both designated drop zones and file path text fields.
  - Custom 850x550 **Details View File Chooser** with dark theme and metadata preview accessory.

- **Enterprise Cryptography Standards**:
  - **Cipher**: AES-256 in Galois/Counter Mode (`AES/GCM/NoPadding`) with a 128-bit authentication tag.
  - **Key Derivation Function (KDF)**: `PBKDF2WithHmacSHA256` with 600,000 iterations and a cryptographically secure random 16-byte salt.
  - **Initialization Vector (IV)**: Fresh 12-byte (96-bit) IV generated per operation via `SecureRandom` (never reused).
  - **Tamper Resistance**: Catches `AEADBadTagException` on invalid passwords or modified bytes and reports `"Wrong password or file was tampered with"`.
  - **Staged Decryption**: Decrypts directly into a temporary file first, and moves to the final target location **only after** GCM authentication verification succeeds. If authentication fails, temporary files are immediately deleted.
  - **Streaming I/O**: Streams data using `CipherInputStream` and `CipherOutputStream` with 64 KB buffers, enabling processing of arbitrarily large multi-gigabyte files without memory spikes.
  - **Memory Sanitization**: Passwords are handled exclusively as `char[]` arrays and wiped with null characters (`\0`) immediately after key generation.

---

## Project Structure

```
├── pom.xml
├── README.md
├── run.bat                           # Double-click launcher for Windows (javaw)
├── Launch-App.vbs                    # Completely silent zero-flash launcher
├── src
│   ├── main
│   │   └── java
│   │       ├── Main.java                 # Entry point, sets Look & Feel and launches GUI on EDT
│   │       ├── crypto
│   │       │   ├── CryptoService.java    # AES-256 GCM & PBKDF2 logic (pure crypto, no UI code)
│   │       │   └── ProgressListener.java # Functional callback interface for progress tracking
│   │       └── ui
│   │           ├── DashboardFrame.java       # Swing/AWT dashboard GUI with tabs, styling, and workers
│   │           ├── FileChooserHelper.java    # Styled 850x550 JFileChooser with Details view & preview accessory
│   │           ├── DropZonePanel.java        # Drag-and-drop file target with dashed border
│   │           ├── PasswordStrengthMeter.java# Live visual password strength indicator
│   │           ├── RoundedButton.java        # Custom antialiased rounded buttons with hover/press states
│   │           ├── FocusAwareBorder.java     # Rounded text/password field borders with focus highlight
│   │           ├── CustomTabbedPaneUI.java   # Custom dark tabbed pane UI with active underline indicator
│   │           └── Theme.java                # Centralized color palette and font typography
│   └── test
│       └── java
│           ├── crypto
│           │   └── CryptoServiceTest.java # Core cryptographic JUnit 5 test suite
│           └── ui
│               ├── EndToEndGuiCryptoTest.java # SwingWorker integration & EDT lifecycle tests
│               └── FileChooserHelperTest.java # JFileChooser configuration & filter unit tests
```

---

## Prerequisites

- **Java Development Kit (JDK)**: JDK 17 or higher (tested with JDK 18).
- **Apache Maven**: Version 3.8+ (tested with Maven 3.9.2).

---

## Build & Run Instructions

### 1. Compile and Run Unit Tests
```bash
mvn clean test
```

### 2. Package the Application into an Executable JAR
```bash
mvn package
```
This builds an executable JAR in the `target/` directory:
```
target/file-crypto-tool-1.0.0.jar
```

### 3. Launch the Application

**Option A — Double-Click (No Command Prompt required):**
- **Double-click `run.bat`** in the project folder, OR
- **Double-click `Launch-App.vbs`** for completely silent launch (no black terminal window flashes).

**Option B — Run the packaged JAR from terminal:**
```bash
java -jar target/file-crypto-tool-1.0.0.jar
```

**Option C — Run with Maven:**
```bash
mvn compile exec:java -Dexec.mainClass="Main"
```

---

## Cryptographic Design Rationale

1. **Why AES-GCM?**
   Galois/Counter Mode (GCM) is an Authenticated Encryption with Associated Data (AEAD) algorithm. Unlike unauthenticated modes (e.g. CBC), GCM provides both confidentiality and data integrity authentication simultaneously. Any tampering with the ciphertext, salt, or IV causes authentication verification to fail.

2. **Why PBKDF2-HMAC-SHA256 with 600,000 iterations?**
   OWASP and NIST password storage guidelines recommend high iteration counts (>= 600,000 for PBKDF2-HMAC-SHA256) to drastically increase the computational cost of brute-force and dictionary attacks on user-selected passwords.

3. **Why Temporary File Staging on Decryption?**
   In streaming decryption, plaintext chunks are produced as the file is read. However, GCM's authentication tag is located at the very end of the stream. Writing to a temporary file ensures that if a file was corrupted, modified, or decrypted with the wrong password, no unauthenticated plaintext is written to the user's destination file.

4. **Why `char[]` instead of `java.lang.String`?**
   `String` objects in Java are immutable and placed in the JVM string pool or heap until garbage collected, leaving sensitive passwords exposed in memory dumps. A `char[]` can be explicitly overwritten with `Arrays.fill(chars, '\0')` immediately after use.
