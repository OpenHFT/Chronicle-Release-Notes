package net.openhft.chronicle.releasenotes.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class ReleaseNoteTest {

    @Test
    void throwOnConstruct() {
        assertThrows(NullPointerException.class, () -> new ReleaseNote(null, null, null));
        assertThrows(NullPointerException.class, () -> new ReleaseNote(null, null, "body"));
        assertThrows(NullPointerException.class, () -> new ReleaseNote(null, "title", "body"));

        assertDoesNotThrow(() -> new ReleaseNote("tag", "title", "body"));
    }

    @ParameterizedTest
    @ValueSource(strings = "tag")
    void getTag(String tag) {
        final ReleaseNote releaseNote = new ReleaseNote(tag, "", "");

        assertEquals(tag, releaseNote.getTag());
    }

    @ParameterizedTest
    @ValueSource(strings = "title")
    void getTitle(String title) {
        final ReleaseNote releaseNote = new ReleaseNote("", title, "");

        assertEquals(title, releaseNote.getTitle());
    }

    @ParameterizedTest
    @ValueSource(strings = "body")
    void getBody(String body) {
        final ReleaseNote releaseNote = new ReleaseNote("", "", body);

        assertEquals(body, releaseNote.getBody());
    }
}
