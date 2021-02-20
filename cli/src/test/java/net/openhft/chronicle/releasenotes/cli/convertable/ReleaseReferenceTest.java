package net.openhft.chronicle.releasenotes.cli.convertable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class ReleaseReferenceTest {

    @Test
    void throwOnConstruct() {
        assertThrows(NullPointerException.class, () -> new ReleaseReference(null, null));
        assertThrows(NullPointerException.class, () -> new ReleaseReference(null, "release"));

        assertDoesNotThrow(() -> new ReleaseReference("repository", "release"));
    }

    @ParameterizedTest
    @ValueSource(strings = "repository")
    void getRepository(String repository) {
        final ReleaseReference releaseReference = new ReleaseReference(repository, "");

        assertEquals(repository, releaseReference.getRepository());
    }

    @ParameterizedTest
    @ValueSource(strings = "release")
    void getRelease(String release) {
        final ReleaseReference releaseReference = new ReleaseReference("", release);

        assertEquals(release, releaseReference.getRelease());
    }

    @Test
    void testEquals() {
        final ReleaseReference releaseReference = new ReleaseReference("repository", "release");
        final ReleaseReference equalReleaseReference = new ReleaseReference("repository", "release");

        assertEquals(releaseReference, equalReleaseReference);

        final ReleaseReference diffReleaseReference = new ReleaseReference("diffRepository", "diffRelease");

        assertNotEquals(releaseReference, diffReleaseReference);
    }
}
