package net.openhft.chronicle.releasenotes.creator.internal;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static net.openhft.chronicle.releasenotes.creator.internal.util.MarkdownUtil.bold;
import static net.openhft.chronicle.releasenotes.creator.internal.util.MarkdownUtil.header;
import static net.openhft.chronicle.releasenotes.creator.internal.util.MarkdownUtil.italic;
import static net.openhft.chronicle.releasenotes.creator.internal.util.MarkdownUtil.entry;
import static net.openhft.chronicle.releasenotes.model.Issue.compareByLabel;

import net.openhft.chronicle.releasenotes.creator.ReleaseNoteCreator;
import net.openhft.chronicle.releasenotes.model.AggregatedReleaseNotes;
import net.openhft.chronicle.releasenotes.model.ReleaseNotes;

public final class MarkdownReleaseNoteCreator implements ReleaseNoteCreator {

    private static final String DEFAULT_LABEL = "closed";
    private static final String MISSING_CHANGELOG = "No changelog for this release.";
    private static final String NEW_LINE = System.lineSeparator();

    @Override
    public String formatReleaseNotes(ReleaseNotes releaseNotes) {
        requireNonNull(releaseNotes);

        if (releaseNotes.getIssues().size() == 0) {
            return italic(MISSING_CHANGELOG) + NEW_LINE;
        }

        final StringBuilder body = new StringBuilder();

        releaseNotes.getIssues().stream().sorted(compareByLabel(DEFAULT_LABEL)).collect(toList()).forEach(issue -> {
            body.append(
                entry(
                    label(bold(issue.getLabels().stream().findFirst().orElse(DEFAULT_LABEL)))
                )
            )
            .append(String.format(" %s [#%d](%s)", issue.getTitle(), issue.getNumber(), issue.getUrl()));

            issue.getComment().ifPresent(comment -> body.append(String.format(" - %s", comment)));

            body.append(NEW_LINE);
        });

        return body.toString();
    }

    @Override
    public String formatAggregatedReleaseNotes(AggregatedReleaseNotes releaseNotes) {
        requireNonNull(releaseNotes);

        final StringBuilder body = new StringBuilder();

        releaseNotes.getReleases().forEach(releaseNote ->
            body.append(header(bold(releaseNote.getTitle()), 3))
                .append(NEW_LINE)
                .append(formatReleaseNotes(releaseNote))
                .append(NEW_LINE));

        return body.toString();
    }

    private String label(String text) {
        return "[" + text + "]";
    }
}
