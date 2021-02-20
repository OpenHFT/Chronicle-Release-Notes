package net.openhft.chronicle.releasenotes.creator.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.openhft.chronicle.releasenotes.model.Issue;
import net.openhft.chronicle.releasenotes.model.Label;
import net.openhft.chronicle.releasenotes.model.ReleaseNote;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class MarkdownReleaseNoteCreatorTest {

    private static final List<Issue> ISSUES = new ArrayList<>();

    private final MarkdownReleaseNoteCreator releaseCreator = new MarkdownReleaseNoteCreator();

    @BeforeAll
    static void init() {
        ISSUES.add(new Issue(1, "Sample Issue", Collections.emptyList()));
        ISSUES.add(new Issue(2, "Sample Feature", Collections.singletonList(new Label("enhancement"))));
        ISSUES.add(new Issue(3, "Sample Bug", Collections.singletonList(new Label("bug"))));
        ISSUES.add(new Issue(4, "Sample Wontfix", Collections.singletonList(new Label("wontfix"))));
        ISSUES.add(new Issue(5, "Sample Complex", Arrays.asList(new Label("enhancement"), new Label("wontfix"))));
    }

    @Test
    void createRelease() {
        final String tag = "1.0.0";

        final ReleaseNote releaseNote = releaseCreator.createReleaseNote(tag, ISSUES);

        assertNotNull(releaseNote);

        printRelease(releaseNote, "Full Release");
    }

    @Test
    void createAggregatedRelease() {
        final String tag = "1.0.0";

        final ReleaseNote releaseNote = releaseCreator.createReleaseNote(tag, ISSUES);

        assertNotNull(releaseNote);

        final ReleaseNote noChangeRelease = new ReleaseNote("1.1.0", "1.1.0", "");

        final ReleaseNote aggregatedRelease = releaseCreator
            .createAggregatedReleaseNote("1.2.0", Arrays.asList(releaseNote, noChangeRelease));

        assertNotNull(aggregatedRelease);

        printRelease(aggregatedRelease, "Aggregated Release");

        assertTrue(aggregatedRelease.getBody().contains(releaseNote.getBody()));
        assertTrue(aggregatedRelease.getBody().contains("No changelog"));
    }

    private void printRelease(ReleaseNote release, String header) {
        System.out.println();
        System.out.println(header + " - Tag: " + release.getTag());
        System.out.println("===================================");
        System.out.println(release.getTitle());
        System.out.println();
        System.out.println(release.getBody());
    }
}
