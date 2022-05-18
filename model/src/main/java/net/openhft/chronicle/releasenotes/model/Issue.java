package net.openhft.chronicle.releasenotes.model;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public interface Issue {

    int getNumber();

    String getTitle();

    List<String> getLabels();

    Optional<String> getComment();

    URL getUrl();

    static Comparator<Issue> compareByLabel(String missingLabel) {
        return (o1, o2) -> {
            final String l1 = o1.getLabels().stream().findFirst().orElse(missingLabel);
            final String l2 = o2.getLabels().stream().findFirst().orElse(missingLabel);

            return l1.compareTo(l2);
        };
    }
}
