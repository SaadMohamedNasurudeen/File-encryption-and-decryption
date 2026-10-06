package crypto;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Objects;

/**
 * High-security cryptographic service implementing authenticated AES-256-GCM encryption
 * and PBKDF2 key derivation. Standard library only (javax.crypto, java.security).
 *
 * File structure: [16-byte Salt] [12-byte IV] [Ciphertext + 16-byte GCM Tag]
 */
public class CryptoService {

    /** AES transformation: AES in Galois/Counter Mode with no padding. */
    public static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /** Key derivation function: PBKDF2 with HMAC-SHA256. */
    public static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";

    /** Iteration count for PBKDF2 (at least 600,000 as required). */
    public static final int PBKDF2_ITERATIONS = 600_000;

    /** Key length: 256 bits (32 bytes). */
    public static final int KEY_LENGTH_BITS = 256;

    /** Random salt length: 16 bytes (128 bits). */
    public static final int SALT_LENGTH_BYTES = 16;

    /** GCM initialization vector length: 12 bytes (96 bits) per NIST SP 800-38D. */
    public static final int IV_LENGTH_BYTES = 12;

    /** GCM authentication tag length: 128 bits (16 bytes). */
    public static final int TAG_LENGTH_BITS = 128;

    /** Minimum file header size: 16 (salt) + 12 (IV) = 28 bytes. */
    public static final int HEADER_LENGTH_BYTES = SALT_LENGTH_BYTES + IV_LENGTH_BYTES;

    /** Minimum encrypted file size: header (28 bytes) + GCM tag (16 bytes) = 44 bytes. */
    public static final int MIN_ENCRYPTED_FILE_SIZE = HEADER_LENGTH_BYTES + (TAG_LENGTH_BITS / 8);

    /** Standard buffer size for streaming (64 KB). */
    public static final int BUFFER_SIZE = 64 * 1024;

    /** Standard encrypted file extension. */
    public static final String ENCRYPTED_EXTENSION = ".enc";

    private final SecureRandom secureRandom;

    public CryptoService() {
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypts the input file using the provided password and saves to [inputFile].enc.
     * The password char array is zeroed out after use.
     *
     * @param inputFile File to encrypt
     * @param password  User password (zeroed after execution)
     * @return The newly created encrypted file
     * @throws Exception on encryption error or I/O failure
     */
    public File encrypt(File inputFile, char[] password) throws Exception {
        Objects.requireNonNull(inputFile, "Input file cannot be null");
        File outputFile = new File(inputFile.getParentFile(), inputFile.getName() + ENCRYPTED_EXTENSION);
        return encrypt(inputFile, outputFile, password, null);
    }

    /**
     * Decrypts the input file using the provided password and restores it.
     * The password char array is zeroed out after use.
     *
     * @param inputFile Encrypted file to decrypt
     * @param password  User password (zeroed after execution)
     * @return The newly created decrypted file
     * @throws Exception on wrong password, tampering, or I/O failure
     */
    public File decrypt(File inputFile, char[] password) throws Exception {
        Objects.requireNonNull(inputFile, "Input file cannot be null");
        String name = inputFile.getName();
        File outputFile;
        if (name.toLowerCase().endsWith(ENCRYPTED_EXTENSION)) {
            String originalName = name.substring(0, name.length() - ENCRYPTED_EXTENSION.length());
            outputFile = new File(inputFile.getParentFile(), originalName);
        } else {
            outputFile = new File(inputFile.getParentFile(), name + ".decrypted");
        }
        return decrypt(inputFile, outputFile, password, null);
    }

    /**
     * Full-featured encrypt method supporting explicit output file and progress reporting.
     * Streams large files using CipherOutputStream without loading full content into memory.
     *
     * @param inputFile  Source file to encrypt
     * @param outputFile Destination encrypted file
     * @param password   User password (zeroed after use)
     * @param listener   Optional progress callback
     * @return The encrypted output file
     * @throws Exception on error
     */
    public File encrypt(File inputFile, File outputFile, char[] password, ProgressListener listener) throws Exception {
        validateInputFile(inputFile);
        if (outputFile == null) {
            outputFile = new File(inputFile.getParentFile(), inputFile.getName() + ENCRYPTED_EXTENSION);
        }
        validatePassword(password);

        try {
            // Generate a fresh random 16-byte salt and 12-byte IV for every encryption (never reuse an IV)
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(salt);
            secureRandom.nextBytes(iv);

            // Derive 256-bit AES key using PBKDF2WithHmacSHA256
            SecretKey secretKey = deriveKey(password, salt);

            // Initialize AES-256 GCM cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            long inputFileSize = inputFile.length();

            // Stream file data: write header [salt][IV] first, followed by ciphertext + GCM tag
            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                bos.write(salt);
                bos.write(iv);
                bos.flush();

                try (CipherOutputStream cos = new CipherOutputStream(bos, cipher);
                     FileInputStream fis = new FileInputStream(inputFile);
                     BufferedInputStream bis = new BufferedInputStream(fis)) {

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long totalBytesProcessed = 0;

                    while ((bytesRead = bis.read(buffer)) != -1) {
                        cos.write(buffer, 0, bytesRead);
                        totalBytesProcessed += bytesRead;
                        if (listener != null) {
                            listener.onProgress(totalBytesProcessed, inputFileSize);
                        }
                    }
                    cos.flush();
                }
            }

            if (listener != null) {
                listener.onProgress(inputFileSize, inputFileSize);
            }

            return outputFile;
        } finally {
            zeroArray(password);
        }
    }

    /**
     * Full-featured decrypt method supporting explicit output file and progress reporting.
     * Writes decrypted output to a temporary file first and moves it to the final location
     * ONLY if authentication tag verification succeeds.
     *
     * @param inputFile  Source encrypted file
     * @param outputFile Destination decrypted file
     * @param password   User password (zeroed after use)
     * @param listener   Optional progress callback
     * @return The decrypted output file
     * @throws Exception on wrong password, tampered file, or I/O failure
     */
    public File decrypt(File inputFile, File outputFile, char[] password, ProgressListener listener) throws Exception {
        validateInputFile(inputFile);
        if (outputFile == null) {
            String name = inputFile.getName();
            if (name.toLowerCase().endsWith(ENCRYPTED_EXTENSION)) {
                String originalName = name.substring(0, name.length() - ENCRYPTED_EXTENSION.length());
                outputFile = new File(inputFile.getParentFile(), originalName);
            } else {
                outputFile = new File(inputFile.getParentFile(), name + ".decrypted");
            }
        }
        validatePassword(password);

        if (inputFile.length() < MIN_ENCRYPTED_FILE_SIZE) {
            zeroArray(password);
            throw new SecurityException("Wrong password or file was tampered with");
        }

        File tempFile = null;
        boolean authenticationSuccess = false;

        try {
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            byte[] iv = new byte[IV_LENGTH_BYTES];

            long totalCiphertextBytes = inputFile.length() - HEADER_LENGTH_BYTES;

            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            if (parentDir == null) {
                parentDir = new File(".");
            }

            // Create temporary file in the destination folder for atomic move capability
            tempFile = File.createTempFile(".dec_tmp_", ".part", parentDir);

            try (FileInputStream fis = new FileInputStream(inputFile);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {

                // Read [salt]
                int saltRead = bis.readNBytes(salt, 0, SALT_LENGTH_BYTES);
                if (saltRead != SALT_LENGTH_BYTES) {
                    throw new SecurityException("Wrong password or file was tampered with");
                }

                // Read [IV]
                int ivRead = bis.readNBytes(iv, 0, IV_LENGTH_BYTES);
                if (ivRead != IV_LENGTH_BYTES) {
                    throw new SecurityException("Wrong password or file was tampered with");
                }

                // Derive key using PBKDF2WithHmacSHA256
                SecretKey secretKey = deriveKey(password, salt);

                // Initialize cipher in DECRYPT_MODE
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

                try (CipherInputStream cis = new CipherInputStream(bis, cipher);
                     FileOutputStream fos = new FileOutputStream(tempFile);
                     BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long totalBytesProcessed = 0;

                    while ((bytesRead = cis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                        totalBytesProcessed += bytesRead;
                        if (listener != null) {
                            listener.onProgress(totalBytesProcessed, totalCiphertextBytes);
                        }
                    }
                    bos.flush();
                }
            } catch (AEADBadTagException e) {
                throw new SecurityException("Wrong password or file was tampered with", e);
            } catch (IOException e) {
                if (isAuthenticationOrTamperException(e)) {
                    throw new SecurityException("Wrong password or file was tampered with", e);
                }
                throw e;
            }

            // GCM authentication passed successfully
            authenticationSuccess = true;

            // Atomically or securely replace destination file with verified decrypted temp file
            Files.move(tempFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            if (listener != null) {
                listener.onProgress(totalCiphertextBytes, totalCiphertextBytes);
            }

            return outputFile;
        } finally {
            zeroArray(password);

            // Clean up temporary file if authentication failed or an error occurred
            if (!authenticationSuccess && tempFile != null && tempFile.exists()) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                } catch (IOException ignored) {
                    tempFile.delete();
                }
            }
        }
    }

    /**
     * Derives a 256-bit AES key from the user's password and salt using PBKDF2WithHmacSHA256.
     * Clears internal password structures and wipes intermediate key buffers.
     */
    private SecretKey deriveKey(char[] password, byte[] salt)
            throws GeneralSecurityException {
        PBEKeySpec spec = null;
        byte[] keyBytes = null;
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
            keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            if (spec != null) {
                spec.clearPassword();
            }
            if (keyBytes != null) {
                Arrays.fill(keyBytes, (byte) 0);
            }
        }
    }

    /**
     * Determines whether an exception was triggered by an authentication failure
     * (bad password or tampered ciphertext/tag).
     */
    private static boolean isAuthenticationOrTamperException(Throwable t) {
        Throwable current = t;
        while (current != null) {
            if (current instanceof AEADBadTagException || current instanceof BadPaddingException) {
                return true;
            }
            String msg = current.getMessage();
            if (msg != null && (msg.toLowerCase().contains("tag mismatch") || msg.toLowerCase().contains("bad tag"))) {
                return true;
            }
            for (Throwable suppressed : current.getSuppressed()) {
                if (isAuthenticationOrTamperException(suppressed)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Validates that the input file exists and is a readable file.
     */
    private void validateInputFile(File file) throws FileNotFoundException {
        if (file == null || !file.exists()) {
            throw new FileNotFoundException("File does not exist: " + (file != null ? file.getPath() : "null"));
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Target is not a standard file: " + file.getPath());
        }
    }

    /**
     * Validates that the password is provided and non-empty.
     */
    private void validatePassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
    }

    /**
     * Overwrites a char array with null characters to eliminate sensitive data from memory.
     */
    public static void zeroArray(char[] array) {
        if (array != null) {
            Arrays.fill(array, '\0');
        }
    }
}
