package net.openhft.chronicle.releasenotes.creator;

import net.openhft.chronicle.releasenotes.creator.internal.MarkdownReleaseNoteCreator;
import net.openhft.chronicle.releasenotes.model.Issue;
import net.openhft.chronicle.releasenotes.model.ReleaseNote;

import java.util.List;

public interface ReleaseNoteCreator {

    ReleaseNote createReleaseNote(String tag, List<Issue> issues);

    ReleaseNote createAggregatedReleaseNote(String tag, List<ReleaseNote> releaseNotes);

    static ReleaseNoteCreator markdown() {
        return new MarkdownReleaseNoteCreator();
    }
}
