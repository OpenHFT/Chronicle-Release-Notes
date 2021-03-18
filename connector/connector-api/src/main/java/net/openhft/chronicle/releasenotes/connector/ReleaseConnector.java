package net.openhft.chronicle.releasenotes.connector;

import static java.util.Objects.requireNonNull;

import net.openhft.chronicle.releasenotes.model.ReleaseNote;

import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * @author Mislav Milicevic
 */
public interface ReleaseConnector extends Connector {

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
    ReleaseResult createReleaseFromBranch(String repository, String tag, String branch, ReleaseOptions releaseOptions);

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
    default ReleaseResult createReleaseFromBranch(String repository, String tag, String branch) {
        return createReleaseFromBranch(repository, tag, branch, ReleaseOptions.DEFAULT);
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
    ReleaseResult createReleaseFromBranch(String repository, String tag, String endTag, String branch, ReleaseOptions releaseOptions);

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
    default ReleaseResult createReleaseFromBranch(String repository, String tag, String endTag, String branch) {
        return createReleaseFromBranch(repository, tag, endTag, branch, ReleaseOptions.DEFAULT);
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
    ReleaseResult createReleaseFromMilestone(String repository, String tag, String milestone, ReleaseOptions releaseOptions);

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
    default ReleaseResult createReleaseFromMilestone(String repository, String tag, String milestone) {
        return createReleaseFromMilestone(repository, tag, milestone, ReleaseOptions.DEFAULT);
    }

    /**
     * Creates and a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code releases} are used as
     * a reference to generate the contents of the release notes associated
     * with this aggregated release.
     * <p>
     * In case a release for the provided tag already exists and {@code override}
     * is {@code true}, the existing release will be updated with the contents
     * of the newly generated release. Otherwise, a {@link RuntimeException}
     * will be thrown.
     *
     * @param repository reference
     * @param tag name
     * @param releases to include in the aggregated release
     * @param override an existing release
     * @return {@link ReleaseResult}
     */
    ReleaseResult createAggregatedRelease(String repository, String tag, Map<String, List<String>> releases, boolean override);

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
    default ReleaseResult createAggregatedRelease(String repository, String tag, Map<String, List<String>> releases) {
        return createAggregatedRelease(repository, tag, releases, false);
    }

    /**
     * Creates and a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code releaseNotes} are used as
     * a reference to generate the aggregated release.
     * <p>
     * In case a release for the provided tag already exists and {@code override}
     * is {@code true}, the existing release will be updated with the contents
     * of the newly generated release. Otherwise, a {@link RuntimeException}
     * will be thrown.
     *
     * @param repository reference
     * @param tag name
     * @param releaseNotes to include in the aggregated release
     * @param override an existing release
     * @return {@link ReleaseResult}
     */
    ReleaseResult createAggregatedRelease(String repository, String tag, List<ReleaseNote> releaseNotes, boolean override);

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
    default ReleaseResult createAggregatedRelease(String repository, String tag, List<ReleaseNote> releaseNotes) {
        return createAggregatedRelease(repository, tag, releaseNotes, false);
    }

    @Deprecated
    default ReleaseResult createReleaseFromBranch(String repository, String tag, String branch, List<String> ignoredLabels, boolean override) {
        final ReleaseOptions releaseOptions = new ReleaseOptions.Builder()
            .ignoreLabels(ignoredLabels)
            .overrideRelease(override)
            .build();

        return createReleaseFromBranch(repository, tag, branch, releaseOptions);
    }

    @Deprecated
    default ReleaseResult createReleaseFromBranch(String repository, String tag, String branch, List<String> ignoredLabels) {
        return createReleaseFromBranch(repository, tag, branch, ignoredLabels, false);
    }

    @Deprecated
    default ReleaseResult createReleaseFromBranch(String repository, String tag, String endTag, String branch, List<String> ignoredLabels, boolean override) {
        final ReleaseOptions releaseOptions = new ReleaseOptions.Builder()
            .ignoreLabels(ignoredLabels)
            .overrideRelease(override)
            .build();

        return createReleaseFromBranch(repository, tag, endTag, branch, releaseOptions);
    }

    @Deprecated
    default ReleaseResult createReleaseFromBranch(String repository, String tag, String endTag, String branch, List<String> ignoredLabels) {
        return createReleaseFromBranch(repository, tag, endTag, branch, ignoredLabels, false);
    }

    @Deprecated
    default ReleaseResult createReleaseFromMilestone(String repository, String tag, String milestone, List<String> ignoredLabels, boolean override) {
        final ReleaseOptions releaseOptions = new ReleaseOptions.Builder()
                .ignoreLabels(ignoredLabels)
                .overrideRelease(override)
                .build();

        return createReleaseFromMilestone(repository, tag, milestone, releaseOptions);
    }

    @Deprecated
    default ReleaseResult createReleaseFromMilestone(String repository, String tag, String milestone, List<String> ignoredLabels) {
        return createReleaseFromMilestone(repository, tag, milestone, ignoredLabels, false);
    }

    /**
     * @author Mislav Milicevic
     */
    class ReleaseResult {
        private final ReleaseNote releaseNote;
        private final URL releaseUrl;
        private final RuntimeException error;

        private ReleaseResult(ReleaseNote releaseNote, URL releaseUrl, RuntimeException error) {
            this.releaseNote = releaseNote;
            this.releaseUrl = releaseUrl;
            this.error = error;
        }

        public ReleaseNote getReleaseNote() {
            return releaseNote;
        }

        public URL getReleaseUrl() {
            return releaseUrl;
        }

        public RuntimeException getError() {
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

        public static ReleaseResult success(ReleaseNote releaseNote, URL url) {
            requireNonNull(url);

            return new ReleaseResult(releaseNote, url, null);
        }

        public static ReleaseResult fail(RuntimeException error) {
            return new ReleaseResult(null, null, error);
        }
    }
}
