package net.openhft.chronicle.releasenotes.connector;

import static java.util.Objects.requireNonNull;

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
     * <p>
     * If {@code ignoredLabels} is not {@code null} or is not empty, then
     * all of the issues which contain one of the provided labels are
     * ignored in the release note generation process.
     * <p>
     * In case a release for the provided tag already exists and {@code override}
     * is {@code true}, the existing release will be updated with the contents
     * of the newly generated release. Otherwise, a {@link RuntimeException}
     * will be thrown.
     *
     * @param repository reference
     * @param tag name
     * @param branch reference
     * @param ignoredLabels a list of ignored labels
     * @param override an existing release
     * @return {@link ReleaseResult}
     */
    ReleaseResult createReleaseFromBranch(String repository, String tag, String branch, List<String> ignoredLabels, boolean override);

    /**
     * Creates a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code branch} is used
     * as a reference to generate the contents of the release
     * notes associated with this release. The contents of the
     * release are all issues contained between the provided
     * {@code tag} and the tag that chronologically came before
     * it.
     * <p>
     * If {@code ignoredLabels} is not {@code null} or is not empty, then
     * all of the issues which contain one of the provided labels are
     * ignored in the release note generation process.
     *
     * @param repository reference
     * @param tag name
     * @param branch reference
     * @param ignoredLabels a list of ignored labels
     * @return {@link ReleaseResult}
     */
    default ReleaseResult createReleaseFromBranch(String repository, String tag, String branch, List<String> ignoredLabels) {
        return createReleaseFromBranch(repository, tag, branch, ignoredLabels, false);
    }

    /**
     * Creates a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code branch} is used
     * as a reference to generate the contents of the release
     * notes associated with this release. The contents of the
     * release are all issues contained between the provided
     * {@code tag} and {@code endTag}.
     * <p>
     * If {@code ignoredLabels} is not {@code null} or is not empty, then
     * all of the issues which contain one of the provided labels are
     * ignored in the release note generation process.
     * <p>
     * In case a release for the provided tag already exists and {@code override}
     * is {@code true}, the existing release will be updated with the contents
     * of the newly generated release. Otherwise, a {@link RuntimeException}
     * will be thrown.
     *
     * @param repository reference
     * @param tag name
     * @param endTag name
     * @param branch reference
     * @param ignoredLabels a list of ignored labels
     * @param override an existing release
     * @return {@link ReleaseResult}
     */
    ReleaseResult createReleaseFromBranch(String repository, String tag, String endTag, String branch, List<String> ignoredLabels, boolean override);

    /**
     * Creates a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code branch} is used
     * as a reference to generate the contents of the release
     * notes associated with this release. The contents of the
     * release are all issues contained between the provided
     * {@code tag} and {@code endTag}.
     * <p>
     * If {@code ignoredLabels} is not {@code null} or is not empty, then
     * all of the issues which contain one of the provided labels are
     * ignored in the release note generation process.
     *
     * @param repository reference
     * @param tag name
     * @param endTag name
     * @param branch reference
     * @param ignoredLabels a list of ignored labels
     * @return {@link ReleaseResult}
     */
    default ReleaseResult createReleaseFromBranch(String repository, String tag, String endTag, String branch, List<String> ignoredLabels) {
        return createReleaseFromBranch(repository, tag, endTag, branch, ignoredLabels, false);
    }

    /**
     * Creates a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}.. The provided {@code milestone} is used
     * as a reference to generate the contents of the release notes
     * associated with this release.
     * <p>
     * If {@code ignoredLabels} is not {@code null} or is not empty, then
     * all of the issues which contain one of the provided labels are
     * ignored in the release note generation process.
     * <p>
     * In case a release for the provided tag already exists and {@code override}
     * is {@code true}, the existing release will be updated with the contents
     * of the newly generated release. Otherwise, a {@link RuntimeException}
     * will be thrown.
     *
     * @param repository reference
     * @param tag name
     * @param milestone issues to include in the release
     * @param ignoredLabels a list of ignored labels
     * @param override an existing release
     * @return {@link ReleaseResult}
     */
    ReleaseResult createReleaseFromMilestone(String repository, String tag, String milestone, List<String> ignoredLabels, boolean override);

    /**
     * Creates a release for a provided {@code tag} and returns a
     * {@link ReleaseResult}. The provided {@code milestone} is used
     * as a reference to generate the contents of the release notes
     * associated with this release.
     * <p>
     * If {@code ignoredLabels} is not {@code null} or is not empty, then
     * all of the issues which contain one of the provided labels are
     * ignored in the release note generation process.
     * <p>
     * @param repository reference
     * @param tag name
     * @param milestone issues to include in the release
     * @param ignoredLabels a list of ignored labels
     * @return {@link ReleaseResult}
     */
    default ReleaseResult createReleaseFromMilestone(String repository, String tag, String milestone, List<String> ignoredLabels) {
        return createReleaseFromMilestone(repository, tag, milestone, ignoredLabels, false);
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
     * @author Mislav Milicevic
     */
    class ReleaseResult {
        private final URL releaseUrl;
        private final RuntimeException error;

        private ReleaseResult(URL releaseUrl, RuntimeException error) {
            this.releaseUrl = releaseUrl;
            this.error = error;
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

        public static ReleaseResult success(URL url) {
            requireNonNull(url);

            return new ReleaseResult(url, null);
        }

        public static ReleaseResult fail(RuntimeException error) {
            return new ReleaseResult(null, error);
        }
    }
}
