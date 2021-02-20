package net.openhft.chronicle.releasenotes.model;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public final class ReleaseNote {

    private final String tag;
    private final String title;
    private final String body;

    public ReleaseNote(final String tag, final String title, final String body) {
        this.tag = requireNonNull(tag);
        this.title = requireNonNull(title);
        this.body = requireNonNull(body);
    }

    public String getTag() {
        return tag;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ReleaseNote releaseNote = (ReleaseNote) o;
        return tag.equals(releaseNote.tag) && title.equals(releaseNote.title) && body.equals(releaseNote.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag, title, body);
    }

    @Override
    public String toString() {
        return "Release{" +
                "tag='" + tag + '\'' +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}
