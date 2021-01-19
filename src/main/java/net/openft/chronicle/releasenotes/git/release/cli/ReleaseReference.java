package net.openft.chronicle.releasenotes.git.release.cli;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public final class ReleaseReference {

    private final String repository;
    private final String release;

    public ReleaseReference(String repository, String release) {
        this.repository = requireNonNull(repository);
        this.release = requireNonNull(release);
    }

    public String getRepository() {
        return repository;
    }

    public String getRelease() {
        return release;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final var that = (ReleaseReference) o;
        return repository.equals(that.repository) && release.equals(that.release);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repository, release);
    }

    @Override
    public String toString() {
        return "ReleaseReference{" +
                "repository='" + repository + '\'' +
                ", release='" + release + '\'' +
                '}';
    }
}
