package net.openhft.chronicle.releasenotes.model;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public final class AggregatedReleaseNotes {

    private final String tag;
    private final String title;
    private final List<ReleaseNotes> releases;

    public AggregatedReleaseNotes(final String tag, final String title, final List<ReleaseNotes> releases) {
        this.tag = requireNonNull(tag);
        this.title = requireNonNull(title);
        this.releases = requireNonNull(releases);
    }

    public String getTag() {
        return tag;
    }

    public String getTitle() {
        return title;
    }

    public List<ReleaseNotes> getReleases() {
        return releases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AggregatedReleaseNotes releaseNote = (AggregatedReleaseNotes) o;
        return tag.equals(releaseNote.tag) && title.equals(releaseNote.title) && releases.equals(releaseNote.releases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag, title, releases);
    }

    @Override
    public String toString() {
        return "Release{" +
                "tag='" + tag + '\'' +
                ", title='" + title + '\'' +
                ", releases=" + releases +
                '}';
    }
}
