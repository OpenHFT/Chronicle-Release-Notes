package net.openft.chronicle.releasenotes.git.release;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public final class Release {

    private final String tag;
    private final String title;
    private final String body;

    public Release(final String tag, final String title, final String body) {
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
        final var release = (Release) o;
        return tag.equals(release.tag) && title.equals(release.title) && body.equals(release.body);
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
