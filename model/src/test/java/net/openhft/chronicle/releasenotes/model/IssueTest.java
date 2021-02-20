package net.openhft.chronicle.releasenotes.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class IssueTest {

    @Test
    void throwOnConstruct() {
        assertThrows(NullPointerException.class, () -> new Issue(0, null, null));
        assertThrows(NullPointerException.class, () -> new Issue(0, null, new ArrayList<>()));

        assertDoesNotThrow(() -> new Issue(0, "", new ArrayList<>()));
    }

    @ParameterizedTest
    @ValueSource(ints = 0)
    void getNumber(int number) {
        final Issue issue = new Issue(number, "", new ArrayList<>());

        assertEquals(number, issue.getNumber());
    }

    @ParameterizedTest
    @ValueSource(strings = "title")
    void getTitle(String title) {
        final Issue issue = new Issue(0, title, new ArrayList<>());

        assertEquals(title, issue.getTitle());
    }

    @ParameterizedTest
    @ValueSource(strings = "label")
    void getLabels(String label) {
        final List<Label> labels = Collections.singletonList(new Label(label));

        final Issue issue = new Issue(0, "", labels);

        assertEquals(labels, issue.getLabels());
    }
}
