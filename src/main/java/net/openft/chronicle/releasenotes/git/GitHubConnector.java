package net.openft.chronicle.releasenotes.git;

import static java.util.Objects.requireNonNull;

import net.openft.chronicle.releasenotes.git.release.Release;
import net.openft.chronicle.releasenotes.git.release.ReleaseCreator;
import net.openft.chronicle.releasenotes.git.release.cli.ReleaseReference;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Mislav Milicevic
 */
public final class GitHubConnector {

    private final GitHub gitHub;
    private final ReleaseCreator releaseCreator;

    private GitHubConnector(String token) throws IOException {
        this.gitHub = new GitHubBuilder().withOAuthToken(requireNonNull(token)).build();
        this.releaseCreator = ReleaseCreator.getInstance();
    }

    /**
     * Returns a {@link GHRepository} reference based on the provided
     * {@code repository} name. The provided repository name must
     * be in the format {@code owner/repository}.
     *
     * A {@link RuntimeException} is thrown if the repository is not found.
     *
     * @param repository name
     * @return a {@link GHRepository} reference based on the provided
     *         {@code repository} name
     */
    public GHRepository getRepository(String repository) {
        requireNonNull(repository);

        try {
            return gitHub.getRepository(repository);
        } catch (IOException e) {
            throw new RuntimeException("Repository '" + repository + "' not found");
        }
    }

    private GHBranch getBranch(String repository, String branch) {
        requireNonNull(repository);
        requireNonNull(branch);

        try {
            return getRepository(repository).getBranch(branch);
        } catch (IOException e) {
            throw new RuntimeException("Branch '" + branch + "' not found in repository '" + repository + "'");
        }
    }

    public List<GHCommit> getCommitsForBranch(String repository, String branch, String startTag, String endTag) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(startTag);
        requireNonNull(endTag);

        final var branchRef = getBranch(repository, branch);
        final var startCommit = getTag(repository, startTag).getCommit();
        final var endCommit = getTag(repository, endTag).getCommit();

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
                .list()
                .toList();

            if (commits.stream().noneMatch(commit -> commit.getSHA1().equals(startCommit.getSHA1()))) {
                throw new RuntimeException("Tag '" + startTag + "' not found on branch '" + branch + "'");
            }

            if (commits.stream().noneMatch(commit -> commit.getSHA1().equals(endCommit.getSHA1()))) {
                throw new RuntimeException("Tag '" + endTag + "' not found on branch '" + branch + "'");
            }

            return commits;
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch commits for branch '" + branchRef.getName() + "' in repository '" + branchRef.getOwner().getName() + "'");
        }
    }

    // TODO: Optimize query logic, currently all tags are being pulled
    private GHTag getTag(String repository, String tag) {
        requireNonNull(repository);
        requireNonNull(tag);

        final var repositoryRef = getRepository(repository);

        try {
            final var tags = repositoryRef.listTags().toList();

            return tags.stream()
                .filter(ghTag -> ghTag.getName().equals(tag))
                .findAny().orElseThrow(() -> new RuntimeException("Tag '" + tag + "' not found in repository '" + repository + "'"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch tag '" + tag + "' in repository '" + repository + "'");
        }
    }

    public List<GHIssue> extractIssuesFromCommit(GHCommit commit) {
        try {
            final var commitMessage = commit.getCommitShortInfo().getMessage()
                .replaceAll("\n", " ")
                .replaceAll(" +", " ");

            final var tokens = commitMessage.split(" ");

            return Arrays.stream(tokens)
                .filter(token -> {
                    if (!token.startsWith("#")) {
                        return false;
                    }

                    final var issueId = token.substring(1);

                    if (!issueId.chars().allMatch(Character::isDigit)) {
                        return false;
                    }

                    return issueId.charAt(0) != '0';
                })
                .map(token -> Integer.valueOf(token.substring(1)))
                .map(issueId -> getIssue(commit.getOwner(), issueId))
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch commit info fro commit '" + commit.getSHA1() + "'");
        }
    }

    /**
     * Returns a {@link GHMilestone} reference based on the provided
     * {@code repository} and {@code milestone} names. The provided
     * repository name must be in the format {@code owner/repository}.
     *
     * A {@link RuntimeException} is thrown if the milestone is not found.
     *
     * @param repository name
     * @param milestone name
     * @return a {@link GHMilestone} reference based on the provided
     *         {@code repository} and {@code milestone} names
     */
    public GHMilestone getMilestone(String repository, String milestone) {
        requireNonNull(repository);
        requireNonNull(milestone);

        try {
            return getRepository(repository).listMilestones(GHIssueState.ALL).toList()
                .stream()
                .filter(ghMilestone -> ghMilestone.getTitle().equals(milestone))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Milestone '" + milestone + "' not found"));
        } catch (IOException e) {
            throw new RuntimeException("Milestone '" + milestone + "' not found");
        }
    }

    /**
     * Returns a list of {@link GHIssue} that are contained in the
     * provided {@code milestone}.
     *
     * A {@link RuntimeException} is thrown if the issues could not
     * be fetched.
     *
     * @param milestone reference
     * @return a list of {@link GHIssue} that are contained in the
     *         provided {@code milestone}
     */
    public List<GHIssue> getMilestoneIssues(GHMilestone milestone) {
        requireNonNull(milestone);

        return getMilestoneIssues(milestone, GHIssueState.ALL);
    }

    /**
     * Returns a list of closed {@link GHIssue} that are contained in
     * the provided {@code milestone}.
     *
     * A {@link RuntimeException} is thrown if the issues could not
     * be fetched.
     *
     * @param milestone reference
     * @return a list of closed {@link GHIssue} that are contained in
     *         the provided {@code milestone}
     */
    public List<GHIssue> getClosedMilestoneIssues(GHMilestone milestone) {
        requireNonNull(milestone);

        return getMilestoneIssues(milestone, GHIssueState.CLOSED);
    }

    private List<GHIssue> getMilestoneIssues(GHMilestone milestone, GHIssueState issueState) {
        requireNonNull(milestone);
        requireNonNull(issueState);

        try {
            return milestone.getOwner().listIssues(issueState).toList()
                .stream()
                .filter(ghIssue -> ghIssue.getMilestone() != null && ghIssue.getMilestone().getNumber() == milestone.getNumber())
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch issues");
        }
    }

    private GHIssue getIssue(GHRepository repository, int number) {
        requireNonNull(repository);

        try {
            return repository.listIssues(GHIssueState.ALL).toList()
                .stream()
                .filter(ghIssue -> ghIssue.getNumber() == number)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Failed to find issue #" + number + "' in repository '" + repository.getName() + "'"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch issues for repository '" + repository.getName() + "'");
        }
    }

    public GHRelease getRelease(ReleaseReference releaseReference) {
        requireNonNull(releaseReference);

        final var repository = getRepository(releaseReference.getRepository());

        try {
            return repository.getReleaseByTagName(releaseReference.getRelease());
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch release for tag '" + releaseReference.getRelease() + "'");
        }
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
     * A {@link RuntimeException} is thrown if the provided {@code tag}
     * does not exist or if the release creation fails.
     *
     * @param repository reference
     * @param tag name
     * @param issues to include in the release
     * @param ignoredLabels a list of ignored labels
     * @return a {@link GHRelease} reference for a specified {@code tag}
     */
    public GHRelease createRelease(GHRepository repository, String tag, List<GHIssue> issues, List<String> ignoredLabels) {
        return createRelease(repository, tag, issues, ignoredLabels, false);
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
     * @param issues to include in the release
     * @param ignoredLabels a list of ignored labels
     * @param override an existing release
     * @return a {@link GHRelease} reference for a specified {@code tag}
     */
    public GHRelease createRelease(GHRepository repository, String tag, List<GHIssue> issues, List<String> ignoredLabels, boolean override) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(issues);

        try {
            if (repository.listTags().toList().stream().noneMatch(ghTag -> ghTag.getName().equals(tag))) {
                throw new RuntimeException("Tag '" + tag + "' not found");
            }

            final var remoteRelease = repository.getReleaseByTagName(tag);

            if (remoteRelease != null) {
                if (!override) {
                    throw new RuntimeException("Release for tag '" + tag + "' already exists: use --override to force an override of an existing release");
                }

                final var release = releaseCreator.createRelease(tag, issues, ignoredLabels);

                return remoteRelease.update()
                    .name(release.getTitle())
                    .body(release.getBody())
                    .update();
            }

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
    public GHRelease createAggregatedRelease(GHRepository repository, String tag, List<GHRelease> releases) {
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
    public GHRelease createAggregatedRelease(GHRepository repository, String tag, List<GHRelease> releases, boolean override) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(releases);

        try {
            final var tags = repository.listTags().toList();

            if (tags.stream().noneMatch(ghTag -> ghTag.getName().equals(tag))) {
                throw new RuntimeException("Tag '" + tag + "' not found");
            }

            final var remoteRelease = repository.getReleaseByTagName(tag);

            if (remoteRelease != null) {
                if (!override) {
                    throw new RuntimeException("Release for tag '" + tag + "' already exists: use --override to force an override of an existing release");
                }

                final var normalizedReleases = releases.stream()
                    .map(release -> new Release(release.getTagName(), release.getName(), release.getBody()))
                    .collect(Collectors.toList());

                final var release = releaseCreator.createAggregatedRelease(tag, normalizedReleases);

                return remoteRelease.update()
                    .name(release.getTitle())
                    .body(release.getBody())
                    .update();
            }

            final var normalizedReleases = releases.stream()
                .map(release -> new Release(release.getTagName(), release.getName(), release.getBody()))
                .collect(Collectors.toList());

            final var release = releaseCreator.createAggregatedRelease(tag, normalizedReleases);

            return repository.createRelease(release.getTag())
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
    public void migrateIssues(List<GHMilestone> fromMilestones, GHMilestone toMilestone, List<String> ignoredLabels) {
        requireNonNull(fromMilestones);
        requireNonNull(toMilestone);

        var stream = fromMilestones.stream().flatMap(milestone -> getMilestoneIssues(milestone).stream());

        if (ignoredLabels != null) {
            stream = stream.filter(issue -> issue.getLabels().stream().noneMatch(ghLabel -> ignoredLabels.contains(ghLabel.getName())));
        }

        stream.forEach(ghIssue -> {
            try {
                ghIssue.setMilestone(toMilestone);
            } catch (IOException e) {
                throw new RuntimeException("Failed to assign issue #" + ghIssue.getNumber() + " to milestone '" + toMilestone.getTitle() + "'");
            }
        });
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
