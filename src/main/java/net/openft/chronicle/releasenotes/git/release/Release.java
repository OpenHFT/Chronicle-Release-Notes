package net.openft.chronicle.releasenotes.git.release;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public final class Release {

    private final String title;
    private final String body;

    public Release(final String title, final String body) {
        this.title = requireNonNull(title);
        this.body = requireNonNull(body);
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
        return title.equals(release.title) && body.equals(release.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, body);
    }

    @Override
    public String toString() {
        return "Release{" +
                "title='" + title + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}
