package hotel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void detectsFreshInstallWhenDataDirectoryIsEmpty() throws IOException {
        Path dataDirectory = temporaryFolder.newFolder().toPath();

        assertTrue(Main.isFreshInstall(dataDirectory));
    }

    @Test
    public void doesNotSeedDemoDataWhenExistingDataFileIsPresent() throws IOException {
        Path dataDirectory = temporaryFolder.newFolder().toPath();
        Files.createFile(dataDirectory.resolve("account.txt"));

        assertFalse(Main.isFreshInstall(dataDirectory));
    }
}
