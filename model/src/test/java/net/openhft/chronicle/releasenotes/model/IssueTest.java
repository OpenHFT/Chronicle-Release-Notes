package net.openhft.chronicle.releasenotes.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

final class IssueTest {

    private static final String URL_SCHEME = "https://test.com";

    @Test
    void throwOnConstruct() {
        assertThrows(NullPointerException.class, () -> new FullIssue(0, null, null, null, null));
        assertThrows(NullPointerException.class, () -> new FullIssue(0, null, null, null, new URL(URL_SCHEME)));
        assertThrows(NullPointerException.class, () -> new FullIssue(0, null, null, new ArrayList<>(), new URL(URL_SCHEME)));
        assertThrows(NullPointerException.class, () -> new FullIssue(0, null, new ArrayList<>(), new ArrayList<>(), new URL(URL_SCHEME)));

        assertDoesNotThrow(() -> new FullIssue(0, "", new ArrayList<>(), new ArrayList<>(), new URL(URL_SCHEME)));

        assertThrows(NullPointerException.class, () -> new SimpleIssue(0, null, null, null, null));
        assertThrows(NullPointerException.class, () -> new SimpleIssue(0, null, null, null, new URL(URL_SCHEME)));
        assertThrows(NullPointerException.class, () -> new SimpleIssue(0, null, null, Optional.empty(), new URL(URL_SCHEME)));
        assertThrows(NullPointerException.class, () -> new SimpleIssue(0, null, Optional.empty(), Optional.empty(), new URL(URL_SCHEME)));

        assertDoesNotThrow(() -> new SimpleIssue(0, "", Optional.empty(), Optional.empty(), new URL(URL_SCHEME)));
    }
}
