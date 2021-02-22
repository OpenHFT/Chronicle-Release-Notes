package net.openhft.chronicle.releasenotes.creator.internal;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import net.openhft.chronicle.releasenotes.creator.ReleaseNoteCreator;
import net.openhft.chronicle.releasenotes.model.Issue;
import net.openhft.chronicle.releasenotes.model.Label;
import net.openhft.chronicle.releasenotes.model.ReleaseNote;

import java.util.List;

public final class MarkdownReleaseNoteCreator implements ReleaseNoteCreator {

    private static final String DEFAULT_LABEL = "closed";
    private static final String MISSING_CHANGELOG = "No changelog";
    private static final String NEW_LINE = System.lineSeparator();

    @Override
    public ReleaseNote createReleaseNote(String tag, List<Issue> issues) {
        requireNonNull(tag);
        requireNonNull(issues);

        issues = issues.stream().sorted((o1, o2) -> {
            final String l1 = o1.getLabels().stream().map(Label::getName).findFirst().orElse(DEFAULT_LABEL);
            final String l2 = o2.getLabels().stream().map(Label::getName).findFirst().orElse(DEFAULT_LABEL);

            return l1.compareTo(l2);
        }).collect(toList());

        final StringBuilder body = new StringBuilder();

        issues.forEach(issue ->
            body.append(
                    entry(
                        label(bold(
                            issue.getLabels().stream().map(Label::getName).findFirst().orElse(DEFAULT_LABEL)
                        ))
                    )
                )
                .append(String.format(" %s [#%d](%s)", issue.getTitle(), issue.getNumber(), issue.getUrl()))
                .append(NEW_LINE)
        );

        return new ReleaseNote(tag, tag, body.toString());
    }

    @Override
    public ReleaseNote createAggregatedReleaseNote(String tag, List<ReleaseNote> releaseNotes) {
        requireNonNull(tag);
        requireNonNull(releaseNotes);

        final StringBuilder body = new StringBuilder();

        releaseNotes.forEach(releaseNote ->
            body.append(header(bold(releaseNote.getTitle())))
                .append(NEW_LINE)
                .append(releaseNote.getBody().isEmpty() ? entry(MISSING_CHANGELOG) + NEW_LINE : releaseNote.getBody())
                .append(NEW_LINE));

        return new ReleaseNote(tag, tag, body.toString());
    }

    private String header(String text) {
        return "### " + text;
    }

    private String entry(String text) {
        return "- " + text;
    }

    private String label(String text) {
        return "[" + text + "]";
    }

    private String bold(String text) {
        return "**" + text + "**";
    }
}
