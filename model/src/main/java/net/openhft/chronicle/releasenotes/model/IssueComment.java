package net.openhft.chronicle.releasenotes.model;

import static java.util.Objects.requireNonNull;

import java.util.Date;
import java.util.Objects;

public final class IssueComment implements Comparable<IssueComment> {

    private final String body;
    private final Date createdAt;

    public IssueComment(String body, Date createdAt) {
        this.body = requireNonNull(body);
        this.createdAt = requireNonNull(createdAt);
    }

    public String getBody() {
        return body;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final IssueComment that = (IssueComment) o;
        return body.equals(that.body) && createdAt.equals(that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(body, createdAt);
    }

    @Override
    public String toString() {
        return "IssueComment{" +
                "body='" + body + '\'' +
                ", createAt=" + createdAt +
                '}';
    }

    @Override
    public int compareTo(IssueComment o) {
        return getCreatedAt().compareTo(o.getCreatedAt());
    }
}
