package ui;

import crypto.CryptoService;
import crypto.ProgressListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class EndToEndGuiCryptoTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("End-to-end test simulating GUI SwingWorker lifecycle and crypto execution")
    void testEndToEndSwingWorkerFlow() throws Exception {
        File plainFile = tempDir.resolve("gui_test_document.txt").toFile();
        String secretData = "CONFIDENTIAL DATA PROCESSED THROUGH SWING WORKER SIMULATION\nLine 2";
        Files.writeString(plainFile.toPath(), secretData, StandardCharsets.UTF_8);

        char[] password = "StrongGuiPassword#2026".toCharArray();
        CryptoService cryptoService = new CryptoService();

        // 1. Simulate Encrypt SwingWorker
        CountDownLatch encLatch = new CountDownLatch(1);
        AtomicReference<File> encResult = new AtomicReference<>();
        AtomicReference<Throwable> encError = new AtomicReference<>();

        SwingUtilities.invokeLater(() -> {
            try {
                ProgressListener progressListener = (processed, total) -> {
                    // Simulates GUI progress reporting
                };
                File encrypted = cryptoService.encrypt(plainFile, null, password, progressListener);
                encResult.set(encrypted);
            } catch (Throwable t) {
                encError.set(t);
            } finally {
                encLatch.countDown();
            }
        });

        assertTrue(encLatch.await(10, TimeUnit.SECONDS), "Encryption SwingWorker should complete in time");
        assertNull(encError.get(), "Encryption should produce no error");
        assertNotNull(encResult.get(), "Encrypted file reference should not be null");
        assertTrue(encResult.get().exists(), "Encrypted file must exist on disk");

        // 2. Simulate Decrypt SwingWorker
        CountDownLatch decLatch = new CountDownLatch(1);
        AtomicReference<File> decResult = new AtomicReference<>();
        AtomicReference<Throwable> decError = new AtomicReference<>();
        char[] decPassword = "StrongGuiPassword#2026".toCharArray();

        SwingUtilities.invokeLater(() -> {
            try {
                ProgressListener progressListener = (processed, total) -> {
                    // Simulates GUI progress reporting
                };
                File decrypted = cryptoService.decrypt(encResult.get(), null, decPassword, progressListener);
                decResult.set(decrypted);
            } catch (Throwable t) {
                decError.set(t);
            } finally {
                decLatch.countDown();
            }
        });

        assertTrue(decLatch.await(10, TimeUnit.SECONDS), "Decryption SwingWorker should complete in time");
        assertNull(decError.get(), "Decryption should produce no error");
        assertNotNull(decResult.get(), "Decrypted file reference should not be null");
        assertTrue(decResult.get().exists(), "Decrypted file must exist on disk");

        String restoredData = Files.readString(decResult.get().toPath(), StandardCharsets.UTF_8);
        assertEquals(secretData, restoredData, "Decrypted data must match original text identically");
    }

    @Test
    @DisplayName("Verify DashboardFrame GUI components initialize cleanly on the EDT")
    void testDashboardFrameInitializationOnEdt() throws Exception {
        AtomicReference<DashboardFrame> frameRef = new AtomicReference<>();
        AtomicReference<Throwable> initError = new AtomicReference<>();

        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    DashboardFrame frame = new DashboardFrame();
                    frameRef.set(frame);
                } catch (Throwable t) {
                    initError.set(t);
                }
            });
        } catch (InvocationTargetException e) {
            initError.set(e.getTargetException());
        }

        assertNull(initError.get(), "DashboardFrame constructor must not throw any exception");
        assertNotNull(frameRef.get(), "DashboardFrame instance must be successfully instantiated");

        // Clean up window resources
        SwingUtilities.invokeAndWait(() -> {
            frameRef.get().dispose();
        });
    }
}
