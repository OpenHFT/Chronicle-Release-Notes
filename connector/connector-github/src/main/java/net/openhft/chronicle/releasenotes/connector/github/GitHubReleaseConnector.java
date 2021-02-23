package net.openhft.chronicle.releasenotes.connector.github;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import net.openhft.chronicle.releasenotes.connector.ConnectorProviderKey;
import net.openhft.chronicle.releasenotes.connector.ReleaseConnector;
import net.openhft.chronicle.releasenotes.connector.exception.TagNotFoundException;
import net.openhft.chronicle.releasenotes.creator.ReleaseNoteCreator;
import net.openhft.chronicle.releasenotes.model.Issue;
import net.openhft.chronicle.releasenotes.model.Label;
import net.openhft.chronicle.releasenotes.model.ReleaseNote;
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
import org.kohsuke.github.PagedIterable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Mislav Milicevic
 */
public final class GitHubReleaseConnector implements ReleaseConnector {

    private final GitHub github;
    private final ReleaseNoteCreator releaseNoteCreator;

    public GitHubReleaseConnector(String token) throws IOException {
        this.github = new GitHubBuilder()
            .withOAuthToken(requireNonNull(token))
            .build();
        this.releaseNoteCreator = ReleaseNoteCreator.markdown();
    }

    @Override
    public ReleaseResult createReleaseFromBranch(String repository, String tag, String branch, List<String> ignoredLabels, boolean override) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(branch);

        final GHRepository repositoryRef = getRepository(repository);

        return createRelease(repositoryRef, tag, () -> getIssuesForBranch(repositoryRef, branch, tag), ignoredLabels, override);
    }

    @Override
    public ReleaseResult createReleaseFromBranch(String repository, String tag, String endTag, String branch, List<String> ignoredLabels, boolean override) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(endTag);
        requireNonNull(branch);

        final GHRepository repositoryRef = getRepository(repository);

        return createRelease(repositoryRef, tag, () -> getIssuesForBranch(repositoryRef, branch, tag, endTag), ignoredLabels, override);
    }

    @Override
        public ReleaseResult createReleaseFromMilestone(String repository, String tag, String milestone, List<String> ignoredLabels, boolean override) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(milestone);

        final GHRepository repositoryRef = getRepository(repository);

        return createRelease(repositoryRef, tag, () -> getClosedMilestoneIssues(repositoryRef, milestone), ignoredLabels, override);
    }

    @Override
    public ReleaseResult createAggregatedRelease(String repository, String tag, Map<String, List<String>> releases, boolean override) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(releases);

        final GHRepository repositoryRef = getRepository(repository);

        try {
            if (!checkTagExists(repositoryRef, tag)) {
                return ReleaseResult.fail(new TagNotFoundException(tag));
            }

            final GHRelease remoteRelease = repositoryRef.getReleaseByTagName(tag);

            final List<GHRelease> sourceReleases = releases.entrySet().stream()
                    .map(entry -> {
                        final GHRepository sourceRepository = getRepository(entry.getKey());
                        return entry.getValue().stream().map(release -> getRelease(sourceRepository, release)).collect(toList());
                    })
                    .flatMap(Collection::stream).collect(toList());

            if (remoteRelease != null) {
                if (!override) {
                    throw new RuntimeException("Release for tag '" + tag + "' already exists: use --override to force an override of an existing release");
                }

                final List<ReleaseNote> normalizedReleaseNotes = sourceReleases.stream()
                    .map(release -> new ReleaseNote(release.getTagName(), release.getName(), release.getBody()))
                    .collect(toList());

                final ReleaseNote releaseNote = releaseNoteCreator.createAggregatedReleaseNote(tag, normalizedReleaseNotes);

                final GHRelease release = remoteRelease.update()
                    .name(releaseNote.getTitle())
                    .body(releaseNote.getBody())
                    .update();

                return ReleaseResult.success(releaseNote, release.getHtmlUrl());
            }

            final List<ReleaseNote> normalizedReleaseNotes = sourceReleases.stream()
                .map(release -> new ReleaseNote(release.getTagName(), release.getName(), release.getBody()))
                .collect(toList());

            final ReleaseNote releaseNote = releaseNoteCreator.createAggregatedReleaseNote(tag, normalizedReleaseNotes);

            final GHRelease release = repositoryRef.createRelease(releaseNote.getTag())
                .name(releaseNote.getTitle())
                .body(releaseNote.getBody())
                .create();

            return ReleaseResult.success(releaseNote, release.getHtmlUrl());
        } catch (IOException e) {
            return ReleaseResult.fail(new RuntimeException("Failed to create release for tag '" + tag + "'"));
        }
    }

    @Override
    public ReleaseResult createAggregatedRelease(String repository, String tag, List<ReleaseNote> releaseNotes, boolean override) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(releaseNotes);

        final GHRepository repositoryRef = getRepository(repository);

        try {
            if (!checkTagExists(repositoryRef, tag)) {
                return ReleaseResult.fail(new TagNotFoundException(tag));
            }

            final GHRelease remoteRelease = repositoryRef.getReleaseByTagName(tag);

            if (remoteRelease != null) {
                if (!override) {
                    throw new RuntimeException("Release for tag '" + tag + "' already exists: use --override to force an override of an existing release");
                }

                final ReleaseNote releaseNote = releaseNoteCreator.createAggregatedReleaseNote(tag, releaseNotes);

                final GHRelease release = remoteRelease.update()
                    .name(releaseNote.getTitle())
                    .body(releaseNote.getBody())
                    .update();

                return ReleaseResult.success(releaseNote, release.getHtmlUrl());
            }

            final ReleaseNote releaseNote = releaseNoteCreator.createAggregatedReleaseNote(tag, releaseNotes);

            final GHRelease release = repositoryRef.createRelease(releaseNote.getTag())
                .name(releaseNote.getTitle())
                .body(releaseNote.getBody())
                .create();

            return ReleaseResult.success(releaseNote, release.getHtmlUrl());
        } catch (IOException e) {
            return ReleaseResult.fail(new RuntimeException("Failed to create release for tag '" + tag + "'"));
        }
    }

    @Override
    public Class<? extends ConnectorProviderKey> getKey() {
        return GitHubConnectorProviderKey.class;
    }

    private ReleaseResult createRelease(
            GHRepository repository, String tag, Supplier<List<GHIssue>> issueSupplier, List<String> ignoredLabels, boolean override) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(issueSupplier);

        try {
            if (!checkTagExists(repository, tag)) {
                return ReleaseResult.fail(new TagNotFoundException(tag));
            }

            final GHRelease remoteRelease = repository.getReleaseByTagName(tag);

            if (remoteRelease != null) {
                if (!override) {
                    throw new RuntimeException("Release for tag '" + tag + "' already exists: use --override to force an override of an existing release");
                }

                final List<Issue> issues = filterIssueLabels(issueSupplier.get(), ignoredLabels)
                    .stream()
                    .map(this::mapIssue)
                    .collect(toList());

                final ReleaseNote releaseNote = releaseNoteCreator.createReleaseNote(tag, issues);

                final GHRelease release = remoteRelease.update()
                    .name(releaseNote.getTitle())
                    .body(releaseNote.getBody())
                    .update();

                return ReleaseResult.success(releaseNote, release.getHtmlUrl());
            }

            final List<Issue> issues = filterIssueLabels(issueSupplier.get(), ignoredLabels)
                .stream()
                .map(this::mapIssue)
                .collect(toList());

            final ReleaseNote releaseNote = releaseNoteCreator.createReleaseNote(tag, issues);

            final GHRelease release = repository.createRelease(releaseNote.getTag())
                .name(releaseNote.getTitle())
                .body(releaseNote.getBody())
                .create();

            return ReleaseResult.success(releaseNote, release.getHtmlUrl());
        } catch (IOException e) {
            return ReleaseResult.fail(new RuntimeException("Failed to create release for tag '" + tag + "'"));
        }
    }

    private GHRepository getRepository(String repository) {
        requireNonNull(repository);

        try {
            return github.getRepository(repository);
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

    private GHRelease getRelease(GHRepository repository, String tag) {
        requireNonNull(repository);
        requireNonNull(tag);

        try {
            return repository.getReleaseByTagName(tag);
        } catch (IOException e) {
            throw new RuntimeException("Failed to find release for tag '" + tag + "' in repository '" + repository.getName() + "'");
        }
    }

    private Map<String, GHTag> getTags(GHRepository repository, String... tags) {
        requireNonNull(repository);
        requireNonNull(tags);

        final List<String> tagSet = Arrays.asList(tags);

        try {
            final Map<String, GHTag> collectedTags = stream(repository.listTags())
                .filter(ghTag -> tagSet.contains(ghTag.getName()))
                .collect(Collectors.toMap(GHTag::getName, Function.identity()));

            if (collectedTags.size() == tags.length) {
                return collectedTags;
            }

            final String missingTags = Arrays.stream(tags)
                .filter(tag -> !collectedTags.containsKey(tag))
                .collect(Collectors.joining(", "));

            throw new RuntimeException("Failed to find tag(s) [" + missingTags + "] in repository '" + repository.getFullName() + "'");
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch tags for repository '" + repository.getFullName() + "'");
        }
    }

    private GHTag getPreviousTag(GHRepository repository, GHBranch branch, GHTag tag) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(tag);

        final Date tagDate = getCommitDate(tag.getCommit());

        try {
            return stream(repository.listTags())
                .filter(ghTag -> getCommitDate(ghTag.getCommit()).before(tagDate))
                .filter(ghTag -> isTagOnBranch(repository, tag, branch))
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Failed to find a tag before tag '" + tag + "' on branch '" + branch + "' for repository '" + repository.getName() + "'"));

        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch tags for repository '" + repository.getFullName() + "'");
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

    private boolean isTagOnBranch(GHRepository repository, GHTag tag, GHBranch branch) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(branch);

        final List<GHCommit> commits = getCommitsForBranchFromTag(repository, branch, tag);

        return commits.stream().anyMatch(commit -> commit.getSHA1().equals(tag.getCommit().getSHA1()));
    }

    private List<GHCommit> getCommitsForBranchFromTag(GHRepository repository, GHBranch branch, GHTag tag) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(tag);

        try {
            final PagedIterable<GHCommit> commits =  repository.queryCommits()
                .from(branch.getSHA1())
                .since(getCommitDate(tag.getCommit()))
                .list();

            return commits.toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch commits for branch '" + branch.getName() + "' in repository '" + repository.getName() + "'");
        }
    }

    private List<GHIssue> getIssuesForBranch(GHRepository repository, String branch, String startTag) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(startTag);

        return getIssuesForBranch(repository, branch, startTag, null);
    }

    private List<GHIssue> getIssuesForBranch(GHRepository repository, String branch, String startTag, String endTag) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(startTag);

        final List<GHCommit> commits = getCommitsForBranch(repository, branch, startTag, endTag);
        final List<Integer> issueIds = extractIssueIdsFromCommits(commits);

        return getIssuesFromIds(repository, issueIds);
    }

    private List<GHCommit> getCommitsForBranch(GHRepository repository, String branch, String startTag, String endTag) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(startTag);

        final GHBranch branchRef = getBranch(repository, branch);
        final Map<String, GHTag> tags = endTag == null ? getTags(repository, startTag) : getTags(repository, startTag, endTag);

        final GHTag endTagRef = endTag == null ? getPreviousTag(repository, branchRef, tags.get(startTag)) : tags.get(endTag);

        final GHCommit startCommit = tags.get(startTag).getCommit();
        final GHCommit endCommit = endTagRef.getCommit();

        if (getCommitDate(startCommit).before(getCommitDate(endCommit))) {
            throw new RuntimeException("Start tag '" + startTag + "' has a commit date before end tag '" + endTag + "'");
        }

        try {
            final PagedIterable<GHCommit> commits = branchRef.getOwner().queryCommits()
                .from(branchRef.getSHA1())
                .since(getCommitDate(endCommit))
                .until(getCommitDate(startCommit))
                .list();

            if (stream(commits).noneMatch(commit -> commit.getSHA1().equals(startCommit.getSHA1()))) {
                throw new RuntimeException("Tag '" + startTag + "' not found on branch '" + branch + "'");
            }

            if (stream(commits).noneMatch(commit -> commit.getSHA1().equals(endCommit.getSHA1()))) {
                throw new RuntimeException("Tag '" + endTagRef.getName() + "' not found on branch '" + branch + "'");
            }

            return commits.toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch commits for branch '" + branchRef.getName() + "' in repository '" + branchRef.getOwner().getName() + "'");
        }
    }

    private List<Integer> extractIssueIdsFromCommits(List<GHCommit> commits) {
        return commits.stream()
            .map(this::extractIssueIdsFromCommit)
            .flatMap(Collection::stream)
            .collect(toList());
    }

    private List<Integer> extractIssueIdsFromCommit(GHCommit commit) {
        requireNonNull(commit);

        try {
            final String commitMessage = commit.getCommitShortInfo().getMessage()
                .replaceAll("\n", " ")
                .replaceAll(" +", " ");

            final String[] tokens = commitMessage.split(" ");

            return Arrays.stream(tokens)
                .filter(this::isIssueToken)
                .map(token -> Integer.valueOf(token.substring(1)))
                .collect(toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch commit info for commit '" + commit.getSHA1() + "'");
        }
    }

    private boolean isIssueToken(String token) {
        requireNonNull(token);

        if (!token.startsWith("#")) {
            return false;
        }

        final String issueId = token.substring(1);

        if (!issueId.chars().allMatch(Character::isDigit)) {
            return false;
        }

        return issueId.charAt(0) != '0';
    }

    private List<GHIssue> getIssuesFromIds(GHRepository repository, List<Integer> ids) {
        requireNonNull(repository);

        return stream(repository.listIssues(GHIssueState.ALL))
                .filter(ghIssue -> ids.contains(ghIssue.getNumber()))
                .collect(toList());
    }

    private List<GHIssue> filterIssueLabels(List<GHIssue> issues, List<String> ignoredLabels) {
        requireNonNull(issues);

        if (ignoredLabels == null) {
            return issues;
        }

        return issues.stream()
            .filter(issue -> issue.getLabels().stream()
                .noneMatch(ghLabel -> ignoredLabels.contains(ghLabel.getName())))
            .collect(toList());
    }

    private Issue mapIssue(GHIssue issue) {
        return new Issue(
            issue.getNumber(),
            issue.getTitle(),
            issue.getLabels().stream().map(ghLabel -> new Label(ghLabel.getName())).collect(toList()),
            issue.getHtmlUrl()
        );
    }

    private List<GHIssue> getClosedMilestoneIssues(GHRepository repository, String milestone) {
        requireNonNull(repository);
        requireNonNull(milestone);

        final GHMilestone milestoneRef = getMilestone(repository, milestone);

        return stream(repository.listIssues(GHIssueState.CLOSED))
            .filter(ghIssue -> ghIssue.getMilestone() != null && ghIssue.getMilestone().getNumber() == milestoneRef.getNumber())
            .collect(toList());
    }

    private GHMilestone getMilestone(GHRepository repository, String milestone) {
        requireNonNull(repository);
        requireNonNull(milestone);

        return stream(repository.listMilestones(GHIssueState.ALL))
            .filter(ghMilestone -> ghMilestone.getTitle().equals(milestone))
            .findAny()
            .orElseThrow(() -> new RuntimeException("Milestone '" + milestone + "' not found"));
    }

    private Date getCommitDate(GHCommit commit) {
        try {
            return commit.getCommitDate();
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch commit date for commit '" + commit.getSHA1() + "'");
        }
    }

    private <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
