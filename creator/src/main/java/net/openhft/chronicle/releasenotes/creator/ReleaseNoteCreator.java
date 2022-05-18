package net.openhft.chronicle.releasenotes.creator;

import net.openhft.chronicle.releasenotes.creator.internal.MarkdownReleaseNoteCreator;
import net.openhft.chronicle.releasenotes.model.AggregatedReleaseNotes;
import net.openhft.chronicle.releasenotes.model.ReleaseNotes;

public interface ReleaseNoteCreator {

    String formatReleaseNotes(ReleaseNotes releaseNotes);

    String formatAggregatedReleaseNotes(AggregatedReleaseNotes releaseNotes);

    static ReleaseNoteCreator markdown() {
        return new MarkdownReleaseNoteCreator();
    }
}
