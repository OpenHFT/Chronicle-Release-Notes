package net.openft.chronicle.releasenotes.git.release;

import static java.util.Objects.requireNonNull;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class ReleaseCreator {

    private static final ReleaseCreator INSTANCE = new ReleaseCreator();

    private static final String H3 = "### ";
    private static final String BOLD = "**";
    private static final String ENTRY = "- ";
    private static final String LABEL_OPEN = "[";
    private static final String LABEL_CLOSE = "]";

    private static final String DEFAULT_LABEL = "closed";
    private static final String MISSING_CHANGELOG = ENTRY + "No changelog";

    private static final String NEW_LINE = System.lineSeparator();

    private ReleaseCreator() {
    }

    public static ReleaseCreator getInstance() {
        return INSTANCE;
    }

    public Release createRelease(String tag, Set<GHIssue> issues) {
        return createRelease(tag, issues, null);
    }

    public Release createRelease(String tag, Set<GHIssue> issues, Set<String> ignoredLabels) {
        requireNonNull(tag);
        requireNonNull(issues);

        if (ignoredLabels != null) {
            issues = issues.stream()
                .filter(issue -> issue.getLabels().stream()
                    .noneMatch(ghLabel -> ignoredLabels.contains(ghLabel.getName())))
                .collect(Collectors.toSet());
        }

        issues = issues.stream().sorted((o1, o2) -> {
            final var l1 = o1.getLabels().stream().map(GHLabel::getName).findFirst().orElse(DEFAULT_LABEL);
            final var l2 = o2.getLabels().stream().map(GHLabel::getName).findFirst().orElse(DEFAULT_LABEL);

            return l1.compareTo(l2);
        }).collect(Collectors.toCollection(LinkedHashSet::new));

        final var body = new StringBuilder();

        issues.forEach(issue ->
            body.append(ENTRY)
                .append(LABEL_OPEN)
                .append(BOLD)
                .append(issue.getLabels().stream().map(GHLabel::getName).findFirst().orElse(DEFAULT_LABEL))
                .append(BOLD)
                .append(LABEL_CLOSE)
                .append(String.format(" %s #%d", issue.getTitle(), issue.getNumber()))
                .append(NEW_LINE)
        );

        return new Release(tag, tag, body.toString());
    }

    public Release createAggregatedRelease(String tag, Set<Release> releases) {
        requireNonNull(tag);
        requireNonNull(releases);

        final var body = new StringBuilder();

        releases.forEach(release ->
            body.append(H3).append(BOLD).append(release.getTitle()).append(BOLD)
                .append(NEW_LINE)
                .append(release.getBody().isEmpty() ? MISSING_CHANGELOG + NEW_LINE : release.getBody())
                .append(NEW_LINE));

        return new Release(tag, tag, body.toString());
    }
}
