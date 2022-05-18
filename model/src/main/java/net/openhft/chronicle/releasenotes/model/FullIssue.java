package net.openhft.chronicle.releasenotes.model;

import java.net.URL;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Issue which was parsed from API and has richer information.
 */
public final class FullIssue implements Issue {

    private final int number;
    private final String title;
    private final List<String> labels;
    private final List<IssueComment> comments;
    private final URL url;

    public FullIssue(int number, String title, List<String> labels, List<IssueComment> comments, URL url) {
        this.number = number;
        this.title = requireNonNull(title);
        this.labels = requireNonNull(labels);
        this.comments = requireNonNull(comments);
        this.url = requireNonNull(url);
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public List<String> getLabels() {
        return Collections.unmodifiableList(labels);
    }

    public List<IssueComment> getComments() {
        return Collections.unmodifiableList(comments);
    }

    @Override
    public Optional<String> getComment() {
        return getComments().stream()
                .filter(issueComment -> issueComment.getBody().startsWith("#comment "))
                .sorted(Comparator.reverseOrder())
                .map(issueComment -> issueComment.getBody().substring(issueComment.getBody().indexOf("#comment ") + "#comment ".length()))
                .filter(issueComment -> !issueComment.trim().isEmpty())
                .findFirst();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FullIssue issue = (FullIssue) o;
        return number == issue.number
            && title.equals(issue.title)
            && labels.equals(issue.labels)
            && comments.equals(issue.comments)
            && url.equals(issue.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, title, labels, comments, url);
    }

    @Override
    public String toString() {
        return "FullIssue{" +
                "number=" + number +
                ", title='" + title + '\'' +
                ", labels=" + labels +
                ", comments=" + comments +
                ", url=" + url.toString() +
                '}';
    }
}
