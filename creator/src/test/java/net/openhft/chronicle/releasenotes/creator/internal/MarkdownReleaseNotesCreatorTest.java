package net.openhft.chronicle.releasenotes.creator.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.openhft.chronicle.releasenotes.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

final class MarkdownReleaseNotesCreatorTest {

    private static final List<Issue> ISSUES = new ArrayList<>();
    private static final String URL_SCHEME = "https://test.com";

    private final MarkdownReleaseNoteCreator releaseCreator = new MarkdownReleaseNoteCreator();

    @BeforeAll
    static void init() throws MalformedURLException {
        ISSUES.add(new SimpleIssue(1, "Sample Issue", Collections.emptyList(), Optional.empty(), new URL(URL_SCHEME)));
        ISSUES.add(new SimpleIssue(2, "Sample Feature", Collections.singletonList("enhancement"), Optional.empty(), new URL(URL_SCHEME)));
        ISSUES.add(new SimpleIssue(3, "Sample Bug", Collections.singletonList("bug"), Optional.of("Explanation"), new URL(URL_SCHEME)));
        ISSUES.add(new SimpleIssue(4, "Sample Wontfix", Collections.singletonList("wontfix"), Optional.empty(), new URL(URL_SCHEME)));
        ISSUES.add(new FullIssue(5, "Sample Complex", Arrays.asList("enhancement", "wontfix"), Collections.emptyList(), new URL(URL_SCHEME)));
    }

    @Test
    void createRelease() {
        final String tag = "1.0.0";

        ReleaseNotes releaseNotes = new ReleaseNotes(tag, tag, ISSUES);
        final String body = releaseCreator.formatReleaseNotes(releaseNotes);

        assertNotNull(body);

        printRelease(tag, body, "Full Release");
    }

    @Test
    void createAggregatedRelease() {
        final String tag = "1.0.0";

        final ReleaseNotes releaseNotes = new ReleaseNotes(tag, tag, ISSUES);

        final String releaseBody = releaseCreator.formatReleaseNotes(releaseNotes);

        assertNotNull(releaseBody);

        final ReleaseNotes noChangeRelease = new ReleaseNotes("1.1.0", "1.1.0", Collections.emptyList());

        final AggregatedReleaseNotes aggregatedRelease = new AggregatedReleaseNotes("1.2.0", "1.2.0",
                Arrays.asList(releaseNotes, noChangeRelease));

        final String body = releaseCreator.formatAggregatedReleaseNotes(aggregatedRelease);

        assertNotNull(aggregatedRelease);

        printRelease(aggregatedRelease.getTag(), body, "Aggregated Release");

        assertTrue(body.contains(releaseBody));
        assertTrue(body.contains("No changelog"));
    }

    private void printRelease(String tag, String body, String header) {
        System.out.println();
        System.out.println(header + " - Tag: " + tag);
        System.out.println("===================================");
        System.out.println(tag);
        System.out.println();
        System.out.println(body);
    }
}
