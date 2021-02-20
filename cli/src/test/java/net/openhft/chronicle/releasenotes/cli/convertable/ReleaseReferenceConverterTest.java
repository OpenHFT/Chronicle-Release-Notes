package net.openhft.chronicle.releasenotes.cli.convertable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import picocli.CommandLine.TypeConversionException;

final class ReleaseReferenceConverterTest {

    private final ReleaseReferenceConverter releaseReferenceConverter = new ReleaseReferenceConverter();

    @Test
    void convert() {
        assertThrows(NullPointerException.class, () -> releaseReferenceConverter.convert(null));
        assertThrows(TypeConversionException.class, () -> releaseReferenceConverter.convert(""));
        assertThrows(TypeConversionException.class, () -> releaseReferenceConverter.convert("invalid_repository_format:tag"));
        assertThrows(TypeConversionException.class, () -> releaseReferenceConverter.convert("owner/repo$itory:tag"));
        assertThrows(TypeConversionException.class, () -> releaseReferenceConverter.convert("owner/repository:t:ag"));

        final ReleaseReference releaseReference = assertDoesNotThrow(() -> releaseReferenceConverter.convert("owner/repository:tag"));

        assertEquals("owner/repository", releaseReference.getRepository());
        assertEquals("tag", releaseReference.getRelease());
    }
}
