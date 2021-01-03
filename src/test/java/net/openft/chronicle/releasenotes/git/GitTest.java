package net.openft.chronicle.releasenotes.git;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GitTest {

    @Test
    void setConfiguredRepository() {
        assertDoesNotThrow(() -> Git.setConfiguredRepository("owner/repository"));
        assertDoesNotThrow(() -> Git.setConfiguredRepository("owner/valid-repository"));
        assertDoesNotThrow(() -> Git.setConfiguredRepository("owner/valid_repository"));

        assertThrows(RuntimeException.class, () -> Git.setConfiguredRepository(""));
        assertThrows(RuntimeException.class, () -> Git.setConfiguredRepository("owner_repository"));
        assertThrows(RuntimeException.class, () -> Git.setConfiguredRepository("/owner/repository"));
        assertThrows(RuntimeException.class, () -> Git.setConfiguredRepository("owner/repository/"));
        assertThrows(RuntimeException.class, () -> Git.setConfiguredRepository("/owner/repository/"));
    }
}
