package crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class CryptoServiceTest {

    private CryptoService cryptoService;

    @TempDir
    Path tempFolder;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService();
    }

    @Test
    @DisplayName("Encrypt then decrypt returns the identical file content for text data")
    void testEncryptDecryptTextFile() throws Exception {
        String originalContent = "This is a secret message that needs AES-256-GCM protection!\nLine 2 with special chars: !@#$%^&*()_+~`";
        File originalFile = tempFolder.resolve("test_plain.txt").toFile();
        Files.writeString(originalFile.toPath(), originalContent, StandardCharsets.UTF_8);

        char[] password = "MasterSecurePassword@2026!".toCharArray();
        char[] passwordCopy = password.clone();

        // Encrypt
        File encryptedFile = cryptoService.encrypt(originalFile, password);
        assertTrue(encryptedFile.exists(), "Encrypted file should exist");
        assertTrue(encryptedFile.getName().endsWith(".enc"), "Encrypted file should end with .enc");
        assertTrue(encryptedFile.length() > originalFile.length(), "Encrypted file should include salt + IV + tag overhead");

        // Decrypt
        File decryptedFile = cryptoService.decrypt(encryptedFile, passwordCopy);
        assertTrue(decryptedFile.exists(), "Decrypted file should exist");

        String decryptedContent = Files.readString(decryptedFile.toPath(), StandardCharsets.UTF_8);
        assertEquals(originalContent, decryptedContent, "Decrypted content must match original text identically");
    }

    @Test
    @DisplayName("Encrypt then decrypt returns identical file content for binary data")
    void testEncryptDecryptBinaryData() throws Exception {
        // Generate random binary data
        byte[] binaryData = new byte[128 * 1024]; // 128 KB
        new Random(42).nextBytes(binaryData);

        File originalFile = tempFolder.resolve("test_image.bin").toFile();
        Files.write(originalFile.toPath(), binaryData);

        char[] password = "BinarySafePassword#999".toCharArray();
        char[] passwordCopy = password.clone();

        File encryptedFile = cryptoService.encrypt(originalFile, password);
        File decryptedFile = cryptoService.decrypt(encryptedFile, passwordCopy);

        byte[] decryptedData = Files.readAllBytes(decryptedFile.toPath());
        assertArrayEquals(binaryData, decryptedData, "Decrypted binary bytes must match original binary bytes");
    }

    @Test
    @DisplayName("Decryption fails when provided with wrong password")
    void testWrongPasswordFails() throws Exception {
        File originalFile = tempFolder.resolve("confidential.doc").toFile();
        Files.writeString(originalFile.toPath(), "Confidential quarterly financial report.", StandardCharsets.UTF_8);

        char[] correctPassword = "CorrectPassword123".toCharArray();
        char[] wrongPassword = "WrongPassword321".toCharArray();

        File encryptedFile = cryptoService.encrypt(originalFile, correctPassword);

        SecurityException ex = assertThrows(SecurityException.class, () -> {
            cryptoService.decrypt(encryptedFile, wrongPassword);
        });

        assertTrue(ex.getMessage().contains("Wrong password or file was tampered with"),
                "Exception message must state 'Wrong password or file was tampered with'");
    }

    @Test
    @DisplayName("Decryption fails when ciphertext body is tampered with")
    void testTamperedCiphertextFails() throws Exception {
        File originalFile = tempFolder.resolve("tamper_test.txt").toFile();
        Files.writeString(originalFile.toPath(), "Immutable payload that must not be altered.", StandardCharsets.UTF_8);

        char[] password = "IntegrityPassword456".toCharArray();
        File encryptedFile = cryptoService.encrypt(originalFile, password);

        // Tamper with the ciphertext byte (past the 28-byte header)
        try (RandomAccessFile raf = new RandomAccessFile(encryptedFile, "rw")) {
            long targetPos = CryptoService.HEADER_LENGTH_BYTES + 2;
            raf.seek(targetPos);
            byte b = raf.readByte();
            raf.seek(targetPos);
            raf.writeByte(b ^ 0xFF); // flip bits
        }

        char[] decryptPassword = "IntegrityPassword456".toCharArray();
        SecurityException ex = assertThrows(SecurityException.class, () -> {
            cryptoService.decrypt(encryptedFile, decryptPassword);
        });

        assertTrue(ex.getMessage().contains("Wrong password or file was tampered with"),
                "Exception message must state 'Wrong password or file was tampered with'");
    }

    @Test
    @DisplayName("Decryption fails when GCM authentication tag is tampered with")
    void testTamperedTagFails() throws Exception {
        File originalFile = tempFolder.resolve("tag_tamper.txt").toFile();
        Files.writeString(originalFile.toPath(), "Critical configuration data.", StandardCharsets.UTF_8);

        char[] password = "TagValidationPassword789".toCharArray();
        File encryptedFile = cryptoService.encrypt(originalFile, password);

        // Tamper with the last byte (the GCM authentication tag)
        try (RandomAccessFile raf = new RandomAccessFile(encryptedFile, "rw")) {
            long lastBytePos = encryptedFile.length() - 1;
            raf.seek(lastBytePos);
            byte b = raf.readByte();
            raf.seek(lastBytePos);
            raf.writeByte(b ^ 0x01);
        }

        char[] decryptPassword = "TagValidationPassword789".toCharArray();
        SecurityException ex = assertThrows(SecurityException.class, () -> {
            cryptoService.decrypt(encryptedFile, decryptPassword);
        });

        assertTrue(ex.getMessage().contains("Wrong password or file was tampered with"),
                "Exception message must state 'Wrong password or file was tampered with'");
    }

    @Test
    @DisplayName("Decryption cleans up temporary file when tampering or bad password occurs")
    void testTemporaryFileCleanedUpOnFailure() throws Exception {
        File originalFile = tempFolder.resolve("cleanup_test.txt").toFile();
        Files.writeString(originalFile.toPath(), "Test cleanup on failure.", StandardCharsets.UTF_8);

        char[] password = "MySecretPassword123".toCharArray();
        File encryptedFile = cryptoService.encrypt(originalFile, password);

        File destinationFile = tempFolder.resolve("cleanup_test_output.txt").toFile();
        char[] wrongPassword = "IncorrectPassword123".toCharArray();

        assertThrows(SecurityException.class, () -> {
            cryptoService.decrypt(encryptedFile, destinationFile, wrongPassword, null);
        });

        assertFalse(destinationFile.exists(), "Destination file should not be created on failure");

        // Verify no leftover .part / .tmp files in destination directory
        File[] leftOverFiles = tempFolder.toFile().listFiles((dir, name) -> name.startsWith(".dec_tmp_"));
        assertNotNull(leftOverFiles);
        assertEquals(0, leftOverFiles.length, "Temporary files must be deleted if decryption fails");
    }

    @Test
    @DisplayName("Password char array is zeroed out after encryption and decryption")
    void testPasswordIsZeroedAfterUse() throws Exception {
        File originalFile = tempFolder.resolve("zero_pw.txt").toFile();
        Files.writeString(originalFile.toPath(), "Data for zero password test.", StandardCharsets.UTF_8);

        char[] encPassword = "PasswordToZeroOut".toCharArray();
        char[] encPasswordRef = encPassword;

        cryptoService.encrypt(originalFile, encPassword);

        // Check if array was wiped with null characters '\0'
        for (char c : encPasswordRef) {
            assertEquals('\0', c, "Password char array must be zeroed out after encryption");
        }
    }

    @Test
    @DisplayName("Truncated file (less than header + tag size) is rejected")
    void testTruncatedFileFails() throws Exception {
        File tinyFile = tempFolder.resolve("truncated.enc").toFile();
        Files.write(tinyFile.toPath(), new byte[20]); // less than 44 bytes

        char[] password = "AnyPassword".toCharArray();
        SecurityException ex = assertThrows(SecurityException.class, () -> {
            cryptoService.decrypt(tinyFile, password);
        });

        assertTrue(ex.getMessage().contains("Wrong password or file was tampered with"));
    }
}
