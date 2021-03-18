package net.openhft.chronicle.releasenotes.connector;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ReleaseOptions {

    public static final ReleaseOptions DEFAULT = new ReleaseOptions(new ArrayList<>(), false, false);

    private final List<String> ignoredLabels;
    private final boolean overrideRelease;
    private final boolean includeIssuesWithoutClosingKeyword;

    private ReleaseOptions(List<String> ignoredLabels, boolean overrideRelease, boolean includeIssuesWithoutClosingKeyword) {
        this.ignoredLabels = ignoredLabels;
        this.overrideRelease = overrideRelease;
        this.includeIssuesWithoutClosingKeyword = includeIssuesWithoutClosingKeyword;
    }

    public List<String> getIgnoredLabels() {
        return ignoredLabels;
    }

    public boolean overrideRelease() {
        return overrideRelease;
    }

    public boolean includeIssuesWithoutClosingKeyword() {
        return includeIssuesWithoutClosingKeyword;
    }

    public static final class Builder {
        private final List<String> ignoredLabels = new ArrayList<>();

        private boolean overrideRelease;
        private boolean includeIssuesWithoutClosingKeyword;

        public Builder ignoreLabels(String... labels) {
            requireNonNull(labels);

            ignoredLabels.addAll(Arrays.asList(labels));
            return this;
        }

        public Builder ignoreLabels(List<String> labels) {
            requireNonNull(labels);

            ignoredLabels.addAll(labels);
            return this;
        }

        public Builder overrideRelease(boolean overrideRelease) {
            this.overrideRelease = overrideRelease;

            return this;
        }

        public Builder includeIssuesWithoutClosingKeyword(boolean includeIssuesWithoutClosingKeyword) {
            this.includeIssuesWithoutClosingKeyword = includeIssuesWithoutClosingKeyword;

            return this;
        }

        public ReleaseOptions build() {
            return new ReleaseOptions(ignoredLabels, overrideRelease,
                    includeIssuesWithoutClosingKeyword);
        }
    }
}
