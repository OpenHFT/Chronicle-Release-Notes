package net.openhft.chronicle.releasenotes.creator.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class LabelTest {

    @Test
    void throwOnConstruct() {
        assertThrows(NullPointerException.class, () -> new Label(null));

        assertDoesNotThrow(() -> new Label(""));
    }

    @ParameterizedTest
    @ValueSource(strings = "name")
    void getName(String name) {
        final Label label = new Label(name);

        assertEquals(name, label.getName());
    }
}
