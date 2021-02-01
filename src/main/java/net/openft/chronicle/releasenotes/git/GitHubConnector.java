package net.openft.chronicle.releasenotes.git;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import net.openft.chronicle.releasenotes.git.release.Release;
import net.openft.chronicle.releasenotes.git.release.ReleaseCreator;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTag;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Mislav Milicevic
 */
public final class GitHubConnector {

    private final GitHub gitHub;
    private final ReleaseCreator releaseCreator;

    private GitHubConnector(String token) throws IOException {
        this.gitHub = new GitHubBuilder()
            .withOAuthToken(requireNonNull(token))
            .build();
        this.releaseCreator = ReleaseCreator.getInstance();
    }

    /**
     * Creates and returns a {@link GHRelease} reference for a specified
     * {@code tag}. The provided {@code branch} is used as a reference
     * to generate the contents of the release notes associated with
     * this release.
     *
     * If {@code ignoredLabels} is not {@code null} or is not empty, then
     * all of the issues which contain one of the provided labels are
     * ignored in the release note generation process.
     *
     * In case a release for the specified tag already exists and {@code override}
     * is {@code true}, the existing release will be updated with the contents
     * of the newly generated release. Otherwise, a {@link RuntimeException}
     * will be thrown.
     *
     * A {@link RuntimeException} is thrown if the provided {@code tag}
     * does not exist or if the release creation fails.
     *
     * @param repository reference
     * @param tag name
     * @param branch reference
     * @param ignoredLabels a list of ignored labels
     * @param override an existing release
     * @return a {@link GHRelease} reference for a specified {@code tag}
     */
    public GHRelease createReleaseFromBranch(String repository, String tag, String endTag, String branch, Set<String> ignoredLabels, boolean override) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(endTag);
        requireNonNull(branch);

        final var repositoryRef = getRepository(repository);

        return createRelease(repositoryRef, tag, () -> getIssuesForBranch(repositoryRef, branch, tag, endTag), ignoredLabels, override);
    }

    /**
     * Creates and returns a {@link GHRelease} reference for a specified
     * {@code tag}. The provided {@code milestone} is used as a reference
     * to generate the contents of the release notes associated with
     * this release.
     *
     * If {@code ignoredLabels} is not {@code null} or is not empty, then
     * all of the issues which contain one of the provided labels are
     * ignored in the release note generation process.
     *
     * In case a release for the specified tag already exists and {@code override}
     * is {@code true}, the existing release will be updated with the contents
     * of the newly generated release. Otherwise, a {@link RuntimeException}
     * will be thrown.
     *
     * A {@link RuntimeException} is thrown if the provided {@code tag}
     * does not exist or if the release creation fails.
     *
     * @param repository reference
     * @param tag name
     * @param milestone issues to include in the release
     * @param ignoredLabels a list of ignored labels
     * @param override an existing release
     * @return a {@link GHRelease} reference for a specified {@code tag}
     */
    public GHRelease createReleaseFromMilestone(String repository, String tag, String milestone, Set<String> ignoredLabels, boolean override) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(milestone);

        final var repositoryRef = getRepository(repository);

        return createRelease(repositoryRef, tag, () -> getClosedMilestoneIssues(repositoryRef, milestone), ignoredLabels, override);
    }

    private GHRelease createRelease(GHRepository repository, String tag, Supplier<Set<GHIssue>> issueSupplier, Set<String> ignoredLabels, boolean override) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(issueSupplier);

        try {
            if (!checkTagExists(repository, tag)) {
                throw new RuntimeException("Tag '" + tag + "' not found");
            }

            final var remoteRelease = repository.getReleaseByTagName(tag);

            if (remoteRelease != null) {
                if (!override) {
                    throw new RuntimeException("Release for tag '" + tag + "' already exists: use --override to force an override of an existing release");
                }

                final var issues = issueSupplier.get();

                final var release = releaseCreator.createRelease(tag, issues, ignoredLabels);

                return remoteRelease.update()
                    .name(release.getTitle())
                    .body(release.getBody())
                    .update();
            }

            final var issues = issueSupplier.get();

            final var release = releaseCreator.createRelease(tag, issues, ignoredLabels);

            return repository.createRelease(release.getTag())
                .name(release.getTitle())
                .body(release.getBody())
                .create();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create release for tag '" + tag + "'");
        }
    }

    /**
     * Creates and returns a {@link GHRelease} reference for a specified
     * {@code tag}. The provided {@code releases} are used as a reference
     * to generate the contents of the release notes associated with
     * this aggregated release.
     *
     * A {@link RuntimeException} is thrown if the provided {@code tag}
     * does not exist or if the release creation fails.
     *
     * @param repository reference
     * @param tag name
     * @param releases to include in the aggregated release
     * @return a {@link GHRelease} reference for a specified {@code tag}
     */
    public GHRelease createAggregatedRelease(String repository, String tag, Map<String, Set<String>> releases) {
        return createAggregatedRelease(repository, tag, releases, false);
    }

    /**
     * Creates and returns a {@link GHRelease} reference for a specified
     * {@code tag}. The provided {@code releases} are used as a reference
     * to generate the contents of the release notes associated with
     * this aggregated release.
     *
     * In case a release for the specified tag already exists and {@code override}
     * is {@code true}, the existing release will be updated with the contents
     * of the newly generated release. Otherwise, a {@link RuntimeException}
     * will be thrown.
     *
     * A {@link RuntimeException} is thrown if the provided {@code tag}
     * does not exist or if the release creation fails.
     *
     * @param repository reference
     * @param tag name
     * @param releases to include in the aggregated release
     * @param override an existing release
     * @return a {@link GHRelease} reference for a specified {@code tag}
     */
    public GHRelease createAggregatedRelease(String repository, String tag, Map<String, Set<String>> releases, boolean override) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(releases);

        final var repositoryRef = getRepository(repository);

        try {
            if (!checkTagExists(repositoryRef, tag)) {
                throw new RuntimeException("Tag '" + tag + "' not found");
            }

            final var remoteRelease = repositoryRef.getReleaseByTagName(tag);

            final var sourceReleases = releases.entrySet().stream()
                .map(entry -> {
                    final var sourceRepository = getRepository(entry.getKey());
                    return entry.getValue().stream().map(release -> getRelease(sourceRepository, release)).collect(toSet());
                })
                .flatMap(Collection::stream).collect(toSet());

            if (remoteRelease != null) {
                if (!override) {
                    throw new RuntimeException("Release for tag '" + tag + "' already exists: use --override to force an override of an existing release");
                }

                final var normalizedReleases = sourceReleases.stream()
                    .map(release -> new Release(release.getTagName(), release.getName(), release.getBody()))
                    .collect(toSet());

                final var release = releaseCreator.createAggregatedRelease(tag, normalizedReleases);

                return remoteRelease.update()
                    .name(release.getTitle())
                    .body(release.getBody())
                    .update();
            }

            final var normalizedReleases = sourceReleases.stream()
                .map(release -> new Release(release.getTagName(), release.getName(), release.getBody()))
                .collect(toSet());

            final var release = releaseCreator.createAggregatedRelease(tag, normalizedReleases);

            return repositoryRef.createRelease(release.getTag())
                .name(release.getTitle())
                .body(release.getBody())
                .create();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create release for tag '" + tag + "'");
        }
    }

    /**
     * Migrates all issues from a list of a source milestones into a
     * singular target milestone.
     *
     * If {@code ignoredLabels} is not {@code null} or is not empty, then
     * all of the issues which contain one of the provided labels are
     * ignored in the migration process.
     *
     * A {@link RuntimeException} is thrown if the migration process fails.
     *
     * @param fromMilestones a list source milestones
     * @param toMilestone destination milestones
     * @param ignoredLabels a list of ignored labels
     */
    public void migrateIssues(String repository, Set<String> fromMilestones, String toMilestone, Set<String> ignoredLabels) {
        requireNonNull(repository);
        requireNonNull(fromMilestones);
        requireNonNull(toMilestone);

        final var repositoryRef = getRepository(repository);
        final var milestones = getMilestones(repositoryRef, fromMilestones);
        final var toMilestoneRef = getMilestone(repositoryRef, toMilestone);

        final var issues = getMilestoneIssues(repositoryRef, (Set<GHMilestone>) milestones.values(), ignoredLabels);

        issues.forEach(ghIssue -> {
            try {
                ghIssue.setMilestone(toMilestoneRef);
            } catch (IOException e) {
                throw new RuntimeException("Failed to assign issue #" + ghIssue.getNumber() + " to milestone '" + toMilestone + "'");
            }
        });
    }

    private GHRelease getRelease(GHRepository repository, String tag) {
        requireNonNull(repository);
        requireNonNull(tag);

        try {
            return repository.getReleaseByTagName(tag);
        } catch (IOException e) {
            throw new RuntimeException("Failed to find release for tag '" + tag + "' in repository '" + repository.getName() + "'");
        }
    }

    private boolean checkTagExists(GHRepository repository, String tag) {
        requireNonNull(repository);
        requireNonNull(tag);

        try {
            return stream(repository.listTags())
                .anyMatch(ghTag -> ghTag.getName().equals(tag));
        } catch (IOException e) {
            return false;
        }
    }

    private GHMilestone getMilestone(GHRepository repository, String milestone) {
        requireNonNull(repository);
        requireNonNull(milestone);

        return stream(repository.listMilestones(GHIssueState.ALL))
            .filter(ghMilestone -> ghMilestone.getTitle().equals(milestone))
            .findAny()
            .orElseThrow(() -> new RuntimeException("Milestone '" + milestone + "' not found"));
    }

    private Map<String, GHMilestone> getMilestones(GHRepository repository, Set<String> milestones) {
        requireNonNull(repository);
        requireNonNull(milestones);

        final var collectedMilestones = stream(repository.listMilestones(GHIssueState.ALL))
            .filter(milestone -> milestones.contains(milestone.getTitle()))
            .collect(Collectors.toMap(GHMilestone::getTitle, Function.identity()));

        if (collectedMilestones.size() == milestones.size()) {
            return collectedMilestones;
        }

        final var missingMilestones = milestones.stream()
            .filter(tag -> !collectedMilestones.containsKey(tag))
            .collect(Collectors.joining(", "));

        throw new RuntimeException("Failed to find milestones(s) [" + missingMilestones + "] in repository '" + repository.getFullName() + "'");
    }

    private Set<GHIssue> getClosedMilestoneIssues(GHRepository repository, String milestone) {
        requireNonNull(repository);
        requireNonNull(milestone);

        final var milestoneRef = getMilestone(repository, milestone);

        return stream(repository.listIssues(GHIssueState.CLOSED))
            .filter(ghIssue -> ghIssue.getMilestone() != null && ghIssue.getMilestone().getNumber() == milestoneRef.getNumber())
            .collect(toSet());
    }

    private GHRepository getRepository(String repository) {
        requireNonNull(repository);

        try {
            return gitHub.getRepository(repository);
        } catch (IOException e) {
            throw new RuntimeException("Repository '" + repository + "' not found");
        }
    }

    private GHBranch getBranch(GHRepository repository, String branch) {
        requireNonNull(repository);
        requireNonNull(branch);

        try {
            return repository.getBranch(branch);
        } catch (IOException e) {
            throw new RuntimeException("Branch '" + branch + "' not found in repository '" + repository + "'");
        }
    }

    private Set<GHIssue> getIssuesForBranch(GHRepository repository, String branch, String startTag, String endTag) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(startTag);
        requireNonNull(endTag);

        final var commits = getCommitsForBranch(repository, branch, startTag, endTag);
        final var issueIds = extractIssueIdsFromCommits(commits);

        return getIssuesFromIds(repository, issueIds);
    }

    private Set<GHCommit> getCommitsForBranch(GHRepository repository, String branch, String startTag, String endTag) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(startTag);
        requireNonNull(endTag);

        final var branchRef = getBranch(repository, branch);
        final var tags = getTags(repository, startTag, endTag);

        final var startCommit = tags.get(startTag).getCommit();
        final var endCommit = tags.get(endTag).getCommit();

        try {
            if (startCommit.getCommitDate().before(endCommit.getCommitDate())) {
                throw new RuntimeException("Start tag '" + startTag + "' has a commit date before end tag '" + endTag + "'");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch commit date for start/end tag");
        }

        try {
            final var commits = branchRef.getOwner().queryCommits()
                .from(branchRef.getSHA1())
                .since(endCommit.getCommitDate().getTime())
                .until(startCommit.getCommitDate().getTime())
                .list();

            if (stream(commits).noneMatch(commit -> commit.getSHA1().equals(startCommit.getSHA1()))) {
                throw new RuntimeException("Tag '" + startTag + "' not found on branch '" + branch + "'");
            }

            if (stream(commits).noneMatch(commit -> commit.getSHA1().equals(endCommit.getSHA1()))) {
                throw new RuntimeException("Tag '" + endTag + "' not found on branch '" + branch + "'");
            }

            return commits.toSet();
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch commits for branch '" + branchRef.getName() + "' in repository '" + branchRef.getOwner().getName() + "'");
        }
    }

    private Map<String, GHTag> getTags(GHRepository repository, String... tags) {
        requireNonNull(repository);
        requireNonNull(tags);

        final var tagSet = Set.of(tags);

        try {
            final var collectedTags = stream(repository.listTags())
                .filter(ghTag -> tagSet.contains(ghTag.getName()))
                .collect(Collectors.toMap(GHTag::getName, Function.identity()));

            if (collectedTags.size() == tags.length) {
                return collectedTags;
            }

            final var missingTags = Arrays.stream(tags)
                .filter(tag -> !collectedTags.containsKey(tag))
                .collect(Collectors.joining(", "));

            throw new RuntimeException("Failed to find tag(s) [" + missingTags + "] in repository '" + repository.getFullName() + "'");
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch tags for repository '" + repository.getFullName() + "'");
        }
    }

    private Set<Integer> extractIssueIdsFromCommits(Set<GHCommit> commits) {
        return commits.stream()
            .map(this::extractIssueIdsFromCommit)
            .flatMap(Collection::stream)
            .collect(toSet());
    }

    private Set<Integer> extractIssueIdsFromCommit(GHCommit commit) {
        requireNonNull(commit);

        try {
            final var commitMessage = commit.getCommitShortInfo().getMessage()
                .replaceAll("\n", " ")
                .replaceAll(" +", " ");

            final var tokens = commitMessage.split(" ");

            return Arrays.stream(tokens)
                .filter(this::isIssueToken)
                .map(token -> Integer.valueOf(token.substring(1)))
                .collect(toSet());
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch commit info for commit '" + commit.getSHA1() + "'");
        }
    }

    private boolean isIssueToken(String token) {
        requireNonNull(token);

        if (!token.startsWith("#")) {
            return false;
        }

        final var issueId = token.substring(1);

        if (!issueId.chars().allMatch(Character::isDigit)) {
            return false;
        }

        return issueId.charAt(0) != '0';
    }

    private Set<GHIssue> getMilestoneIssues(GHRepository repository, Set<GHMilestone> milestones, Set<String> ignoredLabels) {
        requireNonNull(repository);
        requireNonNull(milestones);

        final var milestoneNumbers = milestones.stream().map(GHMilestone::getNumber).collect(toSet());

        var stream = stream(repository.listIssues(GHIssueState.ALL))
            .filter(issue -> issue.getMilestone() != null && milestoneNumbers.contains(issue.getMilestone().getNumber()));

        if (ignoredLabels != null) {
            stream = stream.filter(issue -> issue.getLabels().stream().noneMatch(label -> ignoredLabels.contains(label.getName())));
        }

        return stream.collect(toSet());
    }

    private Set<GHIssue> getIssuesFromIds(GHRepository repository, Set<Integer> ids) {
        requireNonNull(repository);

        return stream(repository.listIssues(GHIssueState.ALL))
            .filter(ghIssue -> ids.contains(ghIssue.getNumber()))
            .collect(toSet());
    }

    private <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Creates and returns a {@link GitHubConnector} instance
     * from a personal access token.
     *
     * A {@link RuntimeException} is thrown if a connection
     * could not be established.
     *
     * @param token personal access token
     * @return a {@link GitHubConnector} instance
     */
    public static GitHubConnector connectWithAccessToken(String token) {
        try {
            return new GitHubConnector(token);
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to GitHub");
        }
    }

}
