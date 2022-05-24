package net.openhft.chronicle.releasenotes.model;

import java.net.URL;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Issue which was parsed from a release note line.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class SimpleIssue implements Issue {

    private final int number;
    private final String title;
    private final URL url;
    private final List<String> labels;
    private final Optional<String> comment;

    public SimpleIssue(int number, String title, List<String> labels, Optional<String> comment, URL url) {
        this.number = number;
        this.title = requireNonNull(title);
        this.labels = Collections.unmodifiableList(requireNonNull(labels));
        this.comment = requireNonNull(comment);
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
        return labels;
    }

    @Override
    public Optional<String> getComment() {
        return comment;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SimpleIssue issue = (SimpleIssue) o;
        return number == issue.number
                && title.equals(issue.title)
                && labels.equals(issue.labels)
                && comment.equals(issue.comment)
                && url.equals(issue.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, title, labels, comment, url);
    }

    @Override
    public String toString() {
        return "FullIssue{" +
                "number=" + number +
                ", title='" + title + '\'' +
                ", labels=" + labels +
                ", comment=" + comment +
                ", url=" + url.toString() +
                '}';
    }
}
