package net.openhft.chronicle.releasenotes.model;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ReleaseNotes {

    private final String tag;
    private final String title;
    private final List<Issue> issues;

    public ReleaseNotes(final String tag, final String title, final List<Issue> issues) {
        this.tag = requireNonNull(tag);
        this.title = requireNonNull(title);
        this.issues = Collections.unmodifiableList(requireNonNull(issues));
    }

    public String getTag() {
        return tag;
    }

    public String getTitle() {
        return title;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ReleaseNotes releaseNotes = (ReleaseNotes) o;
        return tag.equals(releaseNotes.tag) && title.equals(releaseNotes.title) && issues.equals(releaseNotes.issues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag, title, issues);
    }

    @Override
    public String toString() {
        return "Release{" +
                "tag='" + tag + '\'' +
                ", title='" + title + '\'' +
                ", issues=" + issues +
                '}';
    }
}
