package net.openhft.chronicle.releasenotes.model;

import static java.util.Objects.requireNonNull;

import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class Issue {

    private final int number;
    private final String title;
    private final List<Label> labels;
    private final List<IssueComment> comments;
    private final URL url;

    public Issue(int number, String title, List<Label> labels, List<IssueComment> comments, URL url) {
        this.number = number;
        this.title = requireNonNull(title);
        this.labels = requireNonNull(labels);
        this.comments = requireNonNull(comments);
        this.url = requireNonNull(url);
    }

    public int getNumber() {
        return number;
    }

    public String getTitle() {
        return title;
    }

    public List<Label> getLabels() {
        return Collections.unmodifiableList(labels);
    }

    public List<IssueComment> getComments() {
        return Collections.unmodifiableList(comments);
    }

    public URL getUrl() {
        return url;
    }

    public static Comparator<Issue> compareByLabel(String missingLabel) {
        return (o1, o2) -> {
            final String l1 = o1.getLabels().stream().map(Label::getName).findFirst().orElse(missingLabel);
            final String l2 = o2.getLabels().stream().map(Label::getName).findFirst().orElse(missingLabel);

            return l1.compareTo(l2);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Issue issue = (Issue) o;
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
        return "Issue{" +
                "number=" + number +
                ", title='" + title + '\'' +
                ", labels=" + labels +
                ", comments=" + comments +
                ", url=" + url.toString() +
                '}';
    }
}
