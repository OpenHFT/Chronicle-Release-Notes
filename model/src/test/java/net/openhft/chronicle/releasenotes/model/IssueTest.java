package net.openhft.chronicle.releasenotes.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

final class IssueTest {

    private static final String URL_SCHEME = "https://test.com";

    @Test
    void throwOnConstruct() {
        assertThrows(NullPointerException.class, () -> new Issue(0, null, null, null, null));
        assertThrows(NullPointerException.class, () -> new Issue(0, null, null, null, new URL(URL_SCHEME)));
        assertThrows(NullPointerException.class, () -> new Issue(0, null, null, new ArrayList<>(), new URL(URL_SCHEME)));
        assertThrows(NullPointerException.class, () -> new Issue(0, null, new ArrayList<>(), new ArrayList<>(), new URL(URL_SCHEME)));

        assertDoesNotThrow(() -> new Issue(0, "", new ArrayList<>(), new ArrayList<>(), new URL(URL_SCHEME)));
    }

    @ParameterizedTest
    @ValueSource(ints = 0)
    void getNumber(int number) throws MalformedURLException {
        final Issue issue = new Issue(number, "", new ArrayList<>(), new ArrayList<>(), new URL(URL_SCHEME));

        assertEquals(number, issue.getNumber());
    }

    @ParameterizedTest
    @ValueSource(strings = "title")
    void getTitle(String title) throws MalformedURLException {
        final Issue issue = new Issue(0, title, new ArrayList<>(), new ArrayList<>(), new URL(URL_SCHEME));

        assertEquals(title, issue.getTitle());
    }

    @ParameterizedTest
    @ValueSource(strings = "label")
    void getLabels(String label) throws MalformedURLException {
        final List<Label> labels = Collections.singletonList(new Label(label));

        final Issue issue = new Issue(0, "", labels, new ArrayList<>(), new URL(URL_SCHEME));

        assertEquals(labels, issue.getLabels());
    }

    @ParameterizedTest
    @ValueSource(strings = "comment")
    void getComments(String comment) throws MalformedURLException {
        final List<IssueComment> comments = Collections.singletonList(new IssueComment(comment, new Date()));

        final Issue issue = new Issue(0, "", new ArrayList<>(), comments, new URL(URL_SCHEME));

        assertEquals(comments, issue.getComments());
    }
}
