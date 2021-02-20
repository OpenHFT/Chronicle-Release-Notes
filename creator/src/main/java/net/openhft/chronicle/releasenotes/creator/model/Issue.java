package net.openhft.chronicle.releasenotes.creator.model;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Issue {

    private final int number;
    private final String title;
    private final List<Label> labels;

    public Issue(int number, String title, List<Label> labels) {
        this.number = number;
        this.title = requireNonNull(title);
        this.labels = requireNonNull(labels);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Issue issue = (Issue) o;
        return number == issue.number && title.equals(issue.title) && labels.equals(issue.labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, title, labels);
    }

    @Override
    public String toString() {
        return "Issue{" +
                "number=" + number +
                ", title='" + title + '\'' +
                ", labels=" + labels +
                '}';
    }
}
