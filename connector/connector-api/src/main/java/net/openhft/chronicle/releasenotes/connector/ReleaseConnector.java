package net.openhft.chronicle.releasenotes.connector;

import static java.util.Objects.requireNonNull;

import net.openhft.chronicle.releasenotes.model.AggregatedReleaseNotes;
import net.openhft.chronicle.releasenotes.model.ReleaseNotes;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Mislav Milicevic
 */
public interface ReleaseConnector extends Connector, AutoCloseable {

    /**
     * Creates a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code branch} is used
     * as a reference to generate the contents of the release
     * notes associated with this release. The contents of the
     * release are all issues contained between the provided
     * {@code tag} and the tag that chronologically came before
     * it.
     *
     * @param repository reference
     * @param tag name
     * @param branch reference
     * @return {@link ReleaseResult}
     */
    ReleaseResult<ReleaseNotes> createReleaseFromBranch(String repository, String tag, String branch, BranchReleaseOptions releaseOptions);

    /**
     * Creates a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code branch} is used
     * as a reference to generate the contents of the release
     * notes associated with this release. The contents of the
     * release are all issues contained between the provided
     * {@code tag} and the tag that chronologically came before
     * it.
     *
     * @param repository reference
     * @param tag name
     * @param branch reference
     * @return {@link ReleaseResult}
     */
    default ReleaseResult<ReleaseNotes> createReleaseFromBranch(String repository, String tag, String branch) {
        return createReleaseFromBranch(repository, tag, branch, BranchReleaseOptions.DEFAULT);
    }

    /**
     * Creates a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code branch} is used
     * as a reference to generate the contents of the release
     * notes associated with this release. The contents of the
     * release are all issues contained between the provided
     * {@code tag} and {@code endTag}.
     *
     * @param repository reference
     * @param tag name
     * @param endTag name
     * @param branch reference
     * @return {@link ReleaseResult}
     */
    ReleaseResult<ReleaseNotes> createReleaseFromBranch(String repository, String tag, String endTag, String branch, BranchReleaseOptions releaseOptions);

    /**
     * Creates a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code branch} is used
     * as a reference to generate the contents of the release
     * notes associated with this release. The contents of the
     * release are all issues contained between the provided
     * {@code tag} and {@code endTag}.
     *
     * @param repository reference
     * @param tag name
     * @param endTag name
     * @param branch reference
     * @return {@link ReleaseResult}
     */
    default ReleaseResult<ReleaseNotes> createReleaseFromBranch(String repository, String tag, String endTag, String branch) {
        return createReleaseFromBranch(repository, tag, endTag, branch, BranchReleaseOptions.DEFAULT);
    }

    /**
     * Creates a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}.. The provided {@code milestone} is used
     * as a reference to generate the contents of the release notes
     * associated with this release.
     *
     * @param repository reference
     * @param tag name
     * @param milestone issues to include in the release
     * @return {@link ReleaseResult}
     */
    ReleaseResult<ReleaseNotes> createReleaseFromMilestone(String repository, String tag, String milestone, MilestoneReleaseOptions milestoneReleaseOptions);

    /**
     * Creates a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}.. The provided {@code milestone} is used
     * as a reference to generate the contents of the release notes
     * associated with this release.
     *
     * @param repository reference
     * @param tag name
     * @param milestone issues to include in the release
     * @return {@link ReleaseResult}
     */
    default ReleaseResult<ReleaseNotes> createReleaseFromMilestone(String repository, String tag, String milestone) {
        return createReleaseFromMilestone(repository, tag, milestone, MilestoneReleaseOptions.DEFAULT);
    }

    /**
     * Creates and a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code releases} are used as
     * a reference to generate the contents of the release notes associated
     * with this aggregated release.
     *
     * @param repository reference
     * @param tag name
     * @param releases to include in the aggregated release
     * @return {@link ReleaseResult}
     */
    ReleaseResult<AggregatedReleaseNotes> createAggregatedRelease(String repository, String tag, Map<String, List<String>> releases, AggregateReleaseOptions releaseOptions);

    /**
     * Creates and a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code releases} are used as
     * a reference to generate the contents of the release notes associated
     * with this aggregated release.
     *
     * @param repository reference
     * @param tag name
     * @param releases to include in the aggregated release
     * @return {@link ReleaseResult}
     */
    default ReleaseResult<AggregatedReleaseNotes> createAggregatedRelease(String repository, String tag, Map<String, List<String>> releases) {
        return createAggregatedRelease(repository, tag, releases, AggregateReleaseOptions.DEFAULT);
    }

    /**
     * Creates and a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code releaseNotes} are used as
     * a reference to generate the aggregated release.
     *
     * @param repository reference
     * @param tag name
     * @param releaseNotes to include in the aggregated release
     * @return {@link ReleaseResult}
     */
    ReleaseResult<AggregatedReleaseNotes> createAggregatedRelease(String repository, String tag, List<ReleaseNotes> releaseNotes, AggregateReleaseOptions releaseOptions);

    /**
     * Creates and a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code releaseNotes} are used as
     * a reference to generate the aggregated release.
     *
     * @param repository reference
     * @param tag name
     * @param releaseNotes to include in the aggregated release
     * @return {@link ReleaseResult}
     */
    default ReleaseResult<AggregatedReleaseNotes> createAggregatedRelease(String repository, String tag, List<ReleaseNotes> releaseNotes) {
        return createAggregatedRelease(repository, tag, releaseNotes, AggregateReleaseOptions.DEFAULT);
    }

    /**
     * Queries an existing release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code branch} is used
     * as a reference to generate the contents of the release
     * notes associated with this release. The contents of the
     * release are all issues contained between the provided
     * {@code tag} and the tag that chronologically came before
     * it.
     *
     * @param repository reference
     * @param tag name
     * @param branch reference
     * @return {@link ReleaseResult}
     */
    ReleaseResult<ReleaseNotes> queryReleaseFromBranch(String repository, String tag, String branch, BranchReleaseOptions releaseOptions);

    /**
     * Queries an existing release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code branch} is used
     * as a reference to generate the contents of the release
     * notes associated with this release. The contents of the
     * release are all issues contained between the provided
     * {@code tag} and the tag that chronologically came before
     * it.
     *
     * @param repository reference
     * @param tag name
     * @param branch reference
     * @return {@link ReleaseResult}
     */
    default ReleaseResult<ReleaseNotes> queryReleaseFromBranch(String repository, String tag, String branch) {
        return createReleaseFromBranch(repository, tag, branch, BranchReleaseOptions.DEFAULT);
    }

    @Override
    default void close() throws Exception {

    }

    @Deprecated
    default ReleaseResult<ReleaseNotes> createReleaseFromBranch(String repository, String tag, String branch, List<String> ignoredLabels, boolean override) {
        final BranchReleaseOptions releaseOptions = new BranchReleaseOptions.Builder()
            .ignoreLabels(ignoredLabels)
            .overrideRelease(override)
            .build();

        return createReleaseFromBranch(repository, tag, branch, releaseOptions);
    }

    @Deprecated
    default ReleaseResult<ReleaseNotes> createReleaseFromBranch(String repository, String tag, String branch, List<String> ignoredLabels) {
        return createReleaseFromBranch(repository, tag, branch, ignoredLabels, false);
    }

    @Deprecated
    default ReleaseResult<ReleaseNotes> createReleaseFromBranch(String repository, String tag, String endTag, String branch, List<String> ignoredLabels, boolean override) {
        final BranchReleaseOptions releaseOptions = new BranchReleaseOptions.Builder()
            .ignoreLabels(ignoredLabels)
            .overrideRelease(override)
            .build();

        return createReleaseFromBranch(repository, tag, endTag, branch, releaseOptions);
    }

    @Deprecated
    default ReleaseResult<ReleaseNotes> createReleaseFromBranch(String repository, String tag, String endTag, String branch, List<String> ignoredLabels) {
        return createReleaseFromBranch(repository, tag, endTag, branch, ignoredLabels, false);
    }

    @Deprecated
    default ReleaseResult<ReleaseNotes> createReleaseFromMilestone(String repository, String tag, String milestone, List<String> ignoredLabels, boolean override) {
        final MilestoneReleaseOptions releaseOptions = new MilestoneReleaseOptions.Builder()
            .ignoreLabels(ignoredLabels)
            .overrideRelease(override)
            .build();

        return createReleaseFromMilestone(repository, tag, milestone, releaseOptions);
    }

    @Deprecated
    default ReleaseResult<ReleaseNotes> createReleaseFromMilestone(String repository, String tag, String milestone, List<String> ignoredLabels) {
        return createReleaseFromMilestone(repository, tag, milestone, ignoredLabels, false);
    }

    @Deprecated
    default ReleaseResult<AggregatedReleaseNotes> createAggregatedRelease(String repository, String tag, Map<String, List<String>> releases, boolean override) {
        final AggregateReleaseOptions releaseOptions = new AggregateReleaseOptions.Builder()
            .overrideRelease(override)
            .build();

        return createAggregatedRelease(repository, tag, releases, releaseOptions);
    }

    @Deprecated
    default ReleaseResult<AggregatedReleaseNotes> createAggregatedRelease(String repository, String tag, List<ReleaseNotes> releaseNotes, boolean override) {
        final AggregateReleaseOptions releaseOptions = new AggregateReleaseOptions.Builder()
            .overrideRelease(override)
            .build();

        return createAggregatedRelease(repository, tag, releaseNotes, releaseOptions);
    }

    /**
     * @author Mislav Milicevic
     */
    final class ReleaseResult<T> {
        private final T releaseNotes;
        private final URL releaseUrl;
        private final ReleaseException error;

        private ReleaseResult(T releaseNotes, URL releaseUrl, ReleaseException error) {
            this.releaseNotes = releaseNotes;
            this.releaseUrl = releaseUrl;
            this.error = error;
        }

        public T getReleaseNotes() {
            return releaseNotes;
        }

        public URL getReleaseUrl() {
            return releaseUrl;
        }

        public ReleaseException getError() {
            return error;
        }

        public void throwIfFail() {
            if (isFail()) {
                throw error;
            }
        }

        public boolean isSuccess() {
            return error == null;
        }

        public boolean isFail() {
            return !isSuccess();
        }

        public static <T> ReleaseResult<T> success(T releaseNotes, URL url) {
            requireNonNull(url);

            return new ReleaseResult<T>(releaseNotes, url, null);
        }

        public static <T> ReleaseResult<T> fail(ReleaseException error) {
            return new ReleaseResult<T>(null, null, error);
        }

        public static <T> ReleaseResult<T> fail(Throwable error) {
            return fail(new ReleaseException(error.getMessage()));
        }

        @Override
        public String toString() {
            return "ReleaseResult{" +
                    "releaseNote=" + releaseNotes +
                    ", releaseUrl=" + releaseUrl +
                    ", error=" + error +
                    '}';
        }
    }

    /**
     * @author Mislav Milicevic
     */
    final class BranchReleaseOptions {
        public static final BranchReleaseOptions DEFAULT = new BranchReleaseOptions(
            null,
            new ArrayList<>(),
            false,
            false,
            false,
            false
        );

        private final String title;
        private final List<String> ignoredLabels;
        private final boolean overrideRelease;
        private final boolean includeIssuesWithoutClosingKeyword;
        private final boolean includePullRequests;
        private final boolean includeAdditionalContext;

        private BranchReleaseOptions(String title, List<String> ignoredLabels, boolean overrideRelease, boolean includeIssuesWithoutClosingKeyword, boolean includePullRequests, boolean includeAdditionalContext) {
            this.title = title;
            this.ignoredLabels = ignoredLabels;
            this.overrideRelease = overrideRelease;
            this.includeIssuesWithoutClosingKeyword = includeIssuesWithoutClosingKeyword;
            this.includePullRequests = includePullRequests;
            this.includeAdditionalContext = includeAdditionalContext;
        }

        public String getTitle() {
            return title;
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

        public boolean includePullRequests() {
            return includePullRequests;
        }

        public boolean includeAdditionalContext() {
            return includeAdditionalContext;
        }

        @Override
        public String toString() {
            return "BranchReleaseOptions{" +
                    "title=" + title +
                    ", ignoredLabels=" + ignoredLabels +
                    ", overrideRelease=" + overrideRelease +
                    ", includeIssuesWithoutClosingKeyword=" + includeIssuesWithoutClosingKeyword +
                    ", includePullRequests=" + includePullRequests +
                    ", includeAdditionalContext=" + includeAdditionalContext +
                    '}';
        }

        public static final class Builder {
            private String title;
            private final List<String> ignoredLabels = new ArrayList<>();

            private boolean overrideRelease;
            private boolean includeIssuesWithoutClosingKeyword;
            private boolean includePullRequests;
            private boolean includeAdditionalContext;

            public Builder title(String title) {
                this.title = title;
                return this;
            }

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

            public Builder includePullRequests(boolean includePullRequests) {
                this.includePullRequests = includePullRequests;

                return this;
            }

            public Builder includeAdditionalContext(boolean includeAdditionalContext) {
                this.includeAdditionalContext = includeAdditionalContext;

                return this;
            }

            public BranchReleaseOptions build() {
                return new BranchReleaseOptions(
                    title,
                    ignoredLabels,
                    overrideRelease,
                    includeIssuesWithoutClosingKeyword,
                    includePullRequests,
                    includeAdditionalContext
                );
            }
        }
    }

    /**
     * @author Mislav Milicevic
     */
    class MilestoneReleaseOptions {
        public static final MilestoneReleaseOptions DEFAULT = new MilestoneReleaseOptions(
            new ArrayList<>(),
            false,
            false
        );

        private final List<String> ignoredLabels;
        private final boolean overrideRelease;
        private final boolean includeAdditionalContext;

        private MilestoneReleaseOptions(List<String> ignoredLabels, boolean overrideRelease, boolean includeAdditionalContext) {
            this.ignoredLabels = ignoredLabels;
            this.overrideRelease = overrideRelease;
            this.includeAdditionalContext = includeAdditionalContext;
        }

        public List<String> getIgnoredLabels() {
            return ignoredLabels;
        }

        public boolean overrideRelease() {
            return overrideRelease;
        }

        public boolean includeAdditionalContext() {
            return includeAdditionalContext;
        }

        @Override
        public String toString() {
            return "MilestoneReleaseOptions{" +
                    "ignoredLabels=" + ignoredLabels +
                    ", overrideRelease=" + overrideRelease +
                    '}';
        }

        public static final class Builder {
            private final List<String> ignoredLabels = new ArrayList<>();

            private boolean overrideRelease;
            private boolean includeAdditionalContext;

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

            public Builder includeAdditionalContext(boolean includeAdditionalContext) {
                this.includeAdditionalContext = includeAdditionalContext;

                return this;
            }

            public MilestoneReleaseOptions build() {
                return new MilestoneReleaseOptions(
                    ignoredLabels,
                    overrideRelease,
                    includeAdditionalContext
                );
            }
        }
    }

    /**
     * @author Mislav Milicevic
     */
    class AggregateReleaseOptions {
        public static final AggregateReleaseOptions DEFAULT = new AggregateReleaseOptions(
            false
        );

        private final boolean overrideRelease;

        private AggregateReleaseOptions(boolean overrideRelease) {
            this.overrideRelease = overrideRelease;
        }

        public boolean overrideRelease() {
            return overrideRelease;
        }

        public static final class Builder {

            private boolean overrideRelease;

            public Builder overrideRelease(boolean overrideRelease) {
                this.overrideRelease = overrideRelease;

                return this;
            }

            public AggregateReleaseOptions build() {
                return new AggregateReleaseOptions(
                    overrideRelease
                );
            }
        }
    }
}
