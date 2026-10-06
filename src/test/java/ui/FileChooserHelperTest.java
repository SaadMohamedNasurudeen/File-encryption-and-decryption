package ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.awt.Dimension;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileChooserHelperTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("FileChooserHelper configures Encrypt chooser with 850x550 size, accessory, and filters out .enc")
    void testEncryptFileChooserConfiguration() throws Exception {
        JFileChooser chooser = FileChooserHelper.createFileChooser("Select File to Encrypt", true);

        // Check preferred size
        assertEquals(new Dimension(850, 550), chooser.getPreferredSize(), "Chooser should have 850x550 preferred size");

        // Check accessory panel
        assertNotNull(chooser.getAccessory(), "Accessory preview panel must be present");
        assertTrue(chooser.getAccessory() instanceof FileChooserHelper.FilePreviewAccessory,
                "Accessory must be instance of FilePreviewAccessory");

        // Check default filter: rejects .enc, accepts others
        FileFilter defaultFilter = chooser.getFileFilter();
        assertNotNull(defaultFilter);

        File plainDoc = tempDir.resolve("document.pdf").toFile();
        Files.writeString(plainDoc.toPath(), "test plain content");

        File encDoc = tempDir.resolve("secret.enc").toFile();
        Files.writeString(encDoc.toPath(), "test enc content");

        File subDir = tempDir.resolve("subdir").toFile();
        subDir.mkdir();

        assertTrue(defaultFilter.accept(plainDoc), "Encrypt filter should accept non-.enc file");
        assertFalse(defaultFilter.accept(encDoc), "Encrypt filter should reject .enc file by default");
        assertTrue(defaultFilter.accept(subDir), "Encrypt filter should allow directory navigation");
    }

    @Test
    @DisplayName("FileChooserHelper configures Decrypt chooser defaulting to .enc filter")
    void testDecryptFileChooserConfiguration() throws Exception {
        JFileChooser chooser = FileChooserHelper.createFileChooser("Select Encrypted File", false);

        assertEquals(new Dimension(850, 550), chooser.getPreferredSize(), "Chooser should have 850x550 preferred size");
        assertNotNull(chooser.getAccessory(), "Accessory preview panel must be present");

        FileFilter defaultFilter = chooser.getFileFilter();
        assertNotNull(defaultFilter);

        File plainDoc = tempDir.resolve("photo.png").toFile();
        Files.writeString(plainDoc.toPath(), "image data");

        File encDoc = tempDir.resolve("backup.enc").toFile();
        Files.writeString(encDoc.toPath(), "enc data");

        File subDir = tempDir.resolve("my_folder").toFile();
        subDir.mkdir();

        assertTrue(defaultFilter.accept(encDoc), "Decrypt filter should accept .enc file");
        assertFalse(defaultFilter.accept(plainDoc), "Decrypt filter should reject non-.enc file by default");
        assertTrue(defaultFilter.accept(subDir), "Decrypt filter should allow directory navigation");
    }
}
