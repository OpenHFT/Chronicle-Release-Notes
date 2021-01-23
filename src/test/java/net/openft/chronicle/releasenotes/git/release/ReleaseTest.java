package net.openft.chronicle.releasenotes.git.release;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class ReleaseTest {

    @Test
    void throwOnConstruct() {
        assertThrows(NullPointerException.class, () -> new Release(null, null, null));
        assertThrows(NullPointerException.class, () -> new Release(null, null, "body"));
        assertThrows(NullPointerException.class, () -> new Release(null, "title", "body"));

        assertDoesNotThrow(() -> new Release("tag", "title", "body"));
    }

    @ParameterizedTest
    @ValueSource(strings = "tag")
    void getTag(String tag) {
        final var release = new Release(tag, "", "");

        assertEquals(tag, release.getTag());
    }

    @ParameterizedTest
    @ValueSource(strings = "title")
    void getTitle(String title) {
        final var release = new Release("", title, "");

        assertEquals(title, release.getTitle());
    }

    @ParameterizedTest
    @ValueSource(strings = "body")
    void getBody(String body) {
        final var release = new Release("", "", body);

        assertEquals(body, release.getBody());
    }

    @Test
    void testEquals() {
        final var release = new Release("tag", "title", "body");
        final var equalRelease = new Release("tag", "title", "body");

        assertEquals(release, equalRelease);

        final var diffRelease = new Release("diffTag", "diffTitle", "diffBody");

        assertNotEquals(release, diffRelease);
    }
}
