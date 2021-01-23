package net.openft.chronicle.releasenotes.git.release.cli;

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
        final var releaseReference = new ReleaseReference(repository, "");

        assertEquals(repository, releaseReference.getRepository());
    }

    @ParameterizedTest
    @ValueSource(strings = "release")
    void getRelease(String release) {
        final var releaseReference = new ReleaseReference("", release);

        assertEquals(release, releaseReference.getRelease());
    }

    @Test
    void testEquals() {
        final var releaseReference = new ReleaseReference("repository", "release");
        final var equalReleaseReference = new ReleaseReference("repository", "release");

        assertEquals(releaseReference, equalReleaseReference);

        final var diffReleaseReference = new ReleaseReference("diffRepository", "diffRelease");

        assertNotEquals(releaseReference, diffReleaseReference);
    }
}
