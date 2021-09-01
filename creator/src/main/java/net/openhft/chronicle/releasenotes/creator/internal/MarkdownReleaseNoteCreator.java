package net.openhft.chronicle.releasenotes.creator.internal;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static net.openhft.chronicle.releasenotes.creator.internal.util.MarkdownUtil.bold;
import static net.openhft.chronicle.releasenotes.creator.internal.util.MarkdownUtil.header;
import static net.openhft.chronicle.releasenotes.creator.internal.util.MarkdownUtil.italic;
import static net.openhft.chronicle.releasenotes.creator.internal.util.MarkdownUtil.entry;

import net.openhft.chronicle.releasenotes.creator.ReleaseNoteCreator;
import net.openhft.chronicle.releasenotes.model.Issue;
import net.openhft.chronicle.releasenotes.model.Label;
import net.openhft.chronicle.releasenotes.model.ReleaseNote;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class MarkdownReleaseNoteCreator implements ReleaseNoteCreator {

    private static final String DEFAULT_LABEL = "closed";
    private static final String MISSING_CHANGELOG = "No changelog for this release.";
    private static final String NEW_LINE = System.lineSeparator();

    @Override
    public ReleaseNote createReleaseNote(String tag, List<Issue> issues) {
        requireNonNull(tag);
        requireNonNull(issues);

        if (issues.size() == 0) {
            return new ReleaseNote(tag, tag, italic(MISSING_CHANGELOG));
        }

        final StringBuilder body = new StringBuilder();

        issues.stream().sorted(Issue.compareByLabel(DEFAULT_LABEL)).collect(toList()).forEach(issue -> {
            body.append(
                entry(
                    label(bold(issue.getLabels().stream().map(Label::getName).findFirst().orElse(DEFAULT_LABEL)))
                )
            )
            .append(String.format(" %s [#%d](%s)", issue.getTitle(), issue.getNumber(), issue.getUrl()));

            getReleaseComment(issue).ifPresent(comment -> body.append(String.format(" - %s", comment)));

            body.append(NEW_LINE);
        });

        return new ReleaseNote(tag, tag, body.toString());
    }

    @Override
    public ReleaseNote createAggregatedReleaseNote(String tag, List<ReleaseNote> releaseNotes) {
        requireNonNull(tag);
        requireNonNull(releaseNotes);

        final StringBuilder body = new StringBuilder();

        releaseNotes.forEach(releaseNote ->
            body.append(header(bold(releaseNote.getTitle()), 3))
                .append(NEW_LINE)
                .append(releaseNote.getBody().isEmpty() ? entry(MISSING_CHANGELOG) + NEW_LINE : releaseNote.getBody())
                .append(NEW_LINE));

        return new ReleaseNote(tag, tag, body.toString());
    }

    private String label(String text) {
        return "[" + text + "]";
    }

    private Optional<String> getReleaseComment(Issue issue) {
        return issue.getComments().stream()
            .filter(issueComment -> issueComment.getBody().startsWith("#comment "))
            .sorted(Comparator.reverseOrder())
            .map(issueComment -> issueComment.getBody().substring(issueComment.getBody().indexOf("#comment ") + "#comment ".length()))
            .filter(issueComment -> !issueComment.trim().isEmpty())
            .findFirst();
    }
}
