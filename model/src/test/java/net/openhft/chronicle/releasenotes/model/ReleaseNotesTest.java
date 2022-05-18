package net.openhft.chronicle.releasenotes.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;

final class ReleaseNotesTest {

    @Test
    void throwOnConstruct() {
        assertThrows(NullPointerException.class, () -> new ReleaseNotes(null, null, null));
        assertThrows(NullPointerException.class, () -> new ReleaseNotes(null, null, Collections.emptyList()));
        assertThrows(NullPointerException.class, () -> new ReleaseNotes(null, "title", Collections.emptyList()));

        assertDoesNotThrow(() -> new ReleaseNotes("tag", "title", Collections.emptyList()));
    }
}
