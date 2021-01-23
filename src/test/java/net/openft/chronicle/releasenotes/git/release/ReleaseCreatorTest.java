package net.openft.chronicle.releasenotes.git.release;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

final class ReleaseCreatorTest {

    private static final List<Issue> ISSUES = new ArrayList<>();

    private final ReleaseCreator releaseCreator = ReleaseCreator.getInstance();

    @BeforeAll
    static void init() {
        ISSUES.add(Issue.of(1, "Sample Issue"));
        ISSUES.add(Issue.of(2, "Sample Feature", "enhancement"));
        ISSUES.add(Issue.of(3, "Sample Bug", "bug"));
        ISSUES.add(Issue.of(4, "Sample Wontfix", "wontfix"));
        ISSUES.add(Issue.of(5, "Sample Complex", "enhancement", "wontfix"));
    }

    @Test
    void createRelease() {
        final var tag = "1.0.0";
        final var issues = ISSUES.stream().map(this::mockIssue).collect(Collectors.toList());
        final var ignoredLabels = Collections.singletonList("wontfix");

        final var release = releaseCreator.createRelease(tag, issues);

        assertNotNull(release);

        printRelease(release, "Full Release");

        final var filteredRelease = releaseCreator.createRelease(tag, issues, ignoredLabels);

        assertNotNull(filteredRelease);

        printRelease(filteredRelease, "Filtered Release");

        assertNotEquals(release, filteredRelease);
        assertNotEquals(release.getBody(), filteredRelease.getBody());
        assertEquals(release.getBody().substring(0, filteredRelease.getBody().length()), filteredRelease.getBody());
    }

    @Test
    void createAggregatedRelease() {
        final var tag = "1.0.0";
        final var issues = ISSUES.stream().map(this::mockIssue).collect(Collectors.toList());

        final var release = releaseCreator.createRelease(tag, issues);

        assertNotNull(release);

        final var noChangeRelease = new Release("1.1.0", "1.1.0", "");

        final var aggregatedRelease = releaseCreator
                .createAggregatedRelease("1.2.0", List.of(release, noChangeRelease));

        printRelease(aggregatedRelease, "Aggregated Release");

        assertTrue(aggregatedRelease.getBody().contains(release.getBody()));
        assertTrue(aggregatedRelease.getBody().contains("No changelog"));
    }

    private GHIssue mockIssue(Issue reference) {
        final var issue = mock(GHIssue.class);

        when(issue.getNumber()).thenReturn(reference.getNumber());
        when(issue.getTitle()).thenReturn(reference.getTitle());

        final var collect = reference.getLabels().stream().map(this::mockLabel)
                .collect(Collectors.toList());

        when(issue.getLabels()).thenReturn(collect);

        return issue;
    }

    private GHLabel mockLabel(String name) {
        final var label = mock(GHLabel.class);

        when(label.getName()).thenReturn(name);

        return label;
    }

    private void printRelease(Release release, String header) {
        System.out.println();
        System.out.println(header + " - Tag: " + release.getTag());
        System.out.println("=".repeat(35));
        System.out.println(release.getTitle());
        System.out.println();
        System.out.println(release.getBody());
    }

    private static final class Issue {
        private final int number;
        private final String title;
        private final List<String> labels;

        private Issue(int number, String title, List<String> labels) {
            this.number = number;
            this.title = requireNonNull(title);
            this.labels = requireNonNull(labels);
        }

        public int getNumber() {
            return number;
        }

        public String getTitle() {
            return title;
        }

        public List<String> getLabels() {
            return labels;
        }

        public static Issue of(int number, String title, String... labels) {
            return new Issue(number, title, List.of(labels));
        }
    }
}
