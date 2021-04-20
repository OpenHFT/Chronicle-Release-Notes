package net.openhft.chronicle.releasenotes.connector.github;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import net.openhft.chronicle.releasenotes.connector.ConnectorProviderKey;
import net.openhft.chronicle.releasenotes.connector.ReleaseConnector;
import net.openhft.chronicle.releasenotes.connector.exception.TagNotFoundException;
import net.openhft.chronicle.releasenotes.connector.github.graphql.GitHubGraphQLClient;
import net.openhft.chronicle.releasenotes.connector.github.graphql.model.Tag;
import net.openhft.chronicle.releasenotes.creator.ReleaseNoteCreator;
import net.openhft.chronicle.releasenotes.model.Issue;
import net.openhft.chronicle.releasenotes.model.Label;
import net.openhft.chronicle.releasenotes.model.ReleaseNote;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitQueryBuilder;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTag;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.RateLimitHandler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Mislav Milicevic
 */
public final class GitHubReleaseConnector implements ReleaseConnector {

    private static final int REQUEST_PAGE_SIZE = 100;
    private static final List<String> CLOSING_KEYWORDS = Arrays.asList(
        "close",
        "closes",
        "closed",
        "fix",
        "fixes",
        "fixed",
        "resolve",
        "resolves",
        "resolved"
    );

    private final GitHub github;
    private final GitHubGraphQLClient graphQLClient;
    private final ReleaseNoteCreator releaseNoteCreator;

    private final Logger logger;

    public GitHubReleaseConnector(String token) throws IOException {
        this(token, LogManager.getLogger(GitHubReleaseConnector.class));
    }

    public GitHubReleaseConnector(String token, Logger logger) throws IOException {
        requireNonNull(token);
        requireNonNull(logger);

        this.github = new GitHubBuilder()
            .withOAuthToken(token)
            .withRateLimitHandler(new RateLimitHandler() {
                @Override
                public void onError(IOException e, HttpURLConnection uc) throws IOException {
                    throw e;
                }
            })
            .build();
        this.graphQLClient = new GitHubGraphQLClient(token);
        this.releaseNoteCreator = ReleaseNoteCreator.markdown();
        this.logger = logger;
    }

    @Override
    public ReleaseResult createReleaseFromBranch(String repository, String tag, String branch, BranchReleaseOptions releaseOptions) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(branch);

        final GHRepository repositoryRef = getRepository(repository);

        return createRelease(
            repositoryRef,
            tag,
            () -> getIssuesForBranch(repositoryRef, branch, tag, releaseOptions.includeIssuesWithoutClosingKeyword()),
            releaseOptions.getIgnoredLabels(),
            releaseOptions.overrideRelease()
        );
    }

    @Override
    public ReleaseResult createReleaseFromBranch(String repository, String tag, String endTag, String branch, BranchReleaseOptions releaseOptions) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(endTag);
        requireNonNull(branch);

        final GHRepository repositoryRef = getRepository(repository);

        return createRelease(
            repositoryRef,
            tag,
            () -> getIssuesForBranch(repositoryRef, branch, tag, endTag, releaseOptions.includeIssuesWithoutClosingKeyword()),
            releaseOptions.getIgnoredLabels(),
            releaseOptions.overrideRelease()
        );
    }

    @Override
    public ReleaseResult createReleaseFromMilestone(String repository, String tag, String milestone, MilestoneReleaseOptions releaseOptions) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(milestone);

        final GHRepository repositoryRef = getRepository(repository);

        return createRelease(
            repositoryRef,
            tag, () -> getClosedMilestoneIssues(repositoryRef, milestone),
            releaseOptions.getIgnoredLabels(),
            releaseOptions.overrideRelease()
        );
    }

    @Override
    public ReleaseResult createAggregatedRelease(String repository, String tag, Map<String, List<String>> releases, AggregateReleaseOptions releaseOptions) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(releases);

        final GHRepository repositoryRef = getRepository(repository);

        try {
            if (!checkTagExists(repositoryRef, tag)) {
                return ReleaseResult.fail(new TagNotFoundException(tag));
            }

            final GHRelease remoteRelease = repositoryRef.getReleaseByTagName(tag);

            final Map<String, Map<String, GHRelease>> sourceReleases = releases.entrySet().stream()
                .collect(toMap(Entry::getKey, entry -> {
                    final GHRepository sourceRepository = getRepository(entry.getKey());
                    return entry.getValue().stream()
                        .collect(HashMap::new, (m, v) -> m.put(v, getRelease(sourceRepository, v)), HashMap::putAll);
                }));

            final List<ReleaseNote> normalizedReleaseNotes = sourceReleases.values().stream()
                .map(stringGHReleaseMap -> stringGHReleaseMap.entrySet().stream().map(
                    entry -> {
                        final GHRelease release = entry.getValue();

                        if (release == null) {
                            return new ReleaseNote(entry.getKey(), entry.getKey(), "");
                        }

                        return new ReleaseNote(release.getTagName(), release.getName(), release.getBody());
                    }).collect(toList()))
                .flatMap(Collection::stream)
                .collect(toList());

            if (remoteRelease != null) {
                if (!releaseOptions.overrideRelease()) {
                    throw new RuntimeException("Release for tag '" + tag + "' already exists");
                }

                final ReleaseNote releaseNote = releaseNoteCreator.createAggregatedReleaseNote(tag, normalizedReleaseNotes);

                final GHRelease release = remoteRelease.update()
                    .name(releaseNote.getTitle())
                    .body(releaseNote.getBody())
                    .update();

                return ReleaseResult.success(releaseNote, release.getHtmlUrl());
            }

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
    public ReleaseResult createAggregatedRelease(String repository, String tag, List<ReleaseNote> releaseNotes, AggregateReleaseOptions releaseOptions) {
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
                if (!releaseOptions.overrideRelease()) {
                    throw new RuntimeException("Release for tag '" + tag + "' already exists");
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

    @Override
    public void close() throws Exception {
        ReleaseConnector.super.close();
        graphQLClient.close();
    }

    private ReleaseResult createRelease(GHRepository repository, String tag, Supplier<List<GHIssue>> issueSupplier, List<String> ignoredLabels, boolean override) {
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
                    throw new RuntimeException("Release for tag '" + tag + "' already exists");
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
            final Map<String, GHTag> collectedTags = new HashMap<>();

            for (GHTag tag : repository.listTags().withPageSize(REQUEST_PAGE_SIZE)) {
                if (tagSet.contains(tag.getName())) {
                    collectedTags.put(tag.getName(), tag);
                }

                if (collectedTags.size() == tagSet.size()) {
                    return collectedTags;
                }
            }

            final String missingTags = Arrays.stream(tags)
                .filter(tag -> !collectedTags.containsKey(tag))
                .collect(Collectors.joining(", "));

            throw new RuntimeException("Failed to find tag(s) [" + missingTags + "] in repository '" + repository.getFullName() + "'");
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch tags for repository '" + repository.getFullName() + "'");
        }
    }

    private Optional<GHTag> getPreviousTag(GHRepository repository, GHBranch branch, GHTag tag) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(tag);

        final List<String> commits = getCommitsForBranchFromTag(repository, branch, tag)
            .stream()
            .map(GHCommit::getSHA1)
            .collect(toList());

        final String tagName = graphQLClient.getTags(repository.getOwnerName(), repository.getName()).stream()
            .filter(t -> commits.contains(t.getCommitSHA1()) && !t.getName().equals(tag.getName()))
            .limit(1)
            .findFirst()
            .map(Tag::getName)
            .orElse(null);

        if (tagName == null) {
            return Optional.empty();
        }

        return Optional.of(getTags(repository, tagName).get(tagName));
    }

    private boolean checkTagExists(GHRepository repository, String tag) {
        requireNonNull(repository);
        requireNonNull(tag);

        try {
            return stream(repository.listTags().withPageSize(REQUEST_PAGE_SIZE))
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
                .from(branch.getName())
                .until(getCommitDate(tag.getCommit()))
                .pageSize(REQUEST_PAGE_SIZE)
                .list();

            return commits.toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch commits for branch '" + branch.getName() + "' in repository '" + repository.getName() + "'");
        }
    }

    private List<GHIssue> getIssuesForBranch(GHRepository repository, String branch, String startTag, boolean includeIssuesWithoutClosingKeyword) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(startTag);

        return getIssuesForBranch(repository, branch, startTag, null, includeIssuesWithoutClosingKeyword);
    }

    private List<GHIssue> getIssuesForBranch(GHRepository repository, String branch, String startTag, String endTag, boolean includeIssuesWithoutClosingKeyword) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(startTag);

        final List<GHCommit> commits = getCommitsForBranch(repository, branch, startTag, endTag);
        final List<Integer> issueIds = extractIssueIdsFromCommits(commits, includeIssuesWithoutClosingKeyword);

        return getIssuesFromIds(repository, issueIds);
    }

    private List<GHCommit> getCommitsForBranch(GHRepository repository, String branch, String startTag, String endTag) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(startTag);

        final GHBranch branchRef = getBranch(repository, branch);
        final Map<String, GHTag> tags = endTag == null ? getTags(repository, startTag) : getTags(repository, startTag, endTag);

        final GHTag endTagRef = endTag == null
            ? getPreviousTag(repository, branchRef, tags.get(startTag)).orElse(null)
            : tags.get(endTag);

        final GHCommit startCommit = tags.get(startTag).getCommit();
        final GHCommit endCommit = endTagRef == null ? null : endTagRef.getCommit();

        if (endCommit != null && getCommitDate(startCommit).before(getCommitDate(endCommit))) {
            throw new RuntimeException("Start tag '" + startTag + "' has a commit date before end tag '" + endTag + "'");
        }

        try {
            final GHCommitQueryBuilder commitQueryBuilder = branchRef.getOwner().queryCommits()
                .from(branchRef.getSHA1())
                .until(getCommitDate(startCommit))
                .pageSize(REQUEST_PAGE_SIZE);

            if (endCommit != null) {
                commitQueryBuilder.since(getCommitDate(endCommit));
            }

            final List<GHCommit> commits = commitQueryBuilder.list().withPageSize(REQUEST_PAGE_SIZE).toList();

            if (commits.stream().noneMatch(commit -> commit.getSHA1().equals(startCommit.getSHA1()))) {
                throw new RuntimeException("Tag '" + startTag + "' not found on branch '" + branch + "'");
            }

            if (endCommit != null && commits.stream().noneMatch(commit -> commit.getSHA1().equals(endCommit.getSHA1()))) {
                throw new RuntimeException("Tag '" + endTagRef.getName() + "' not found on branch '" + branch + "'");
            }

            if (endCommit == null) {
                return commits;
            }

            return commits.stream()
                .filter(ghCommit -> getCommitDate(ghCommit).after(getCommitDate(endCommit)))
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch commits for branch '" + branchRef.getName() + "' in repository '" + branchRef.getOwner().getName() + "'");
        }
    }

    private List<Integer> extractIssueIdsFromCommits(List<GHCommit> commits, boolean includeIssuesWithoutClosingKeyword) {
        return commits.stream()
            .map(commit -> extractIssueIdsFromCommit(commit, includeIssuesWithoutClosingKeyword))
            .flatMap(Collection::stream)
            .collect(toList());
    }

    private List<Integer> extractIssueIdsFromCommit(GHCommit commit, boolean includeIssuesWithoutClosingKeyword) {
        requireNonNull(commit);

        try {
            final String commitMessage = commit.getCommitShortInfo().getMessage()
                .replaceAll("\n", " ")
                .replaceAll(" +", " ")
                .replaceAll(",", "");

            final String[] tokens = commitMessage.split(" ");

            final List<Integer> ids = new ArrayList<>();

            for (int i = 0; i < tokens.length; i++) {
                final String token = tokens[i];

                if (!includeIssuesWithoutClosingKeyword) {
                    if (!isClosingKeyword(token)) {
                        continue;
                    }

                    if (i == tokens.length - 1) {
                        continue;
                    }
                }

                final String tokenToCheck = includeIssuesWithoutClosingKeyword ? token : tokens[i + 1];

                if (isLocalIssueReference(tokenToCheck)) {
                    ids.add(Integer.valueOf(tokenToCheck.substring(1)));

                    if (!includeIssuesWithoutClosingKeyword) {
                        i++;
                    }
                    continue;
                }

                if (isUrlIssueReference(commit.getOwner(), tokenToCheck)) {
                    ids.add(Integer.valueOf(tokenToCheck.substring(tokenToCheck.lastIndexOf("/") + 1)));

                    if (!includeIssuesWithoutClosingKeyword) {
                        i++;
                    }
                }
            }

            return ids;
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch commit info for commit '" + commit.getSHA1() + "'");
        }
    }

    private boolean isClosingKeyword(String token) {
        return CLOSING_KEYWORDS.contains(token.toLowerCase());
    }

    private boolean isLocalIssueReference(String token) {
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

    private boolean isUrlIssueReference(GHRepository repository, String token) {
        requireNonNull(token);

        if (!token.startsWith(repository.getHtmlUrl().toString())) {
            return false;
        }

        final String[] split = token.split("/");

        if (!split[split.length - 2].equals("issues")) {
            return false;
        }

        final String issueId = split[split.length - 1];

        if (!issueId.chars().allMatch(Character::isDigit)) {
            return false;
        }

        return issueId.charAt(0) != '0';
    }

    private List<GHIssue> getIssuesFromIds(GHRepository repository, List<Integer> ids) {
        requireNonNull(repository);

        return stream(repository.listIssues(GHIssueState.ALL).withPageSize(REQUEST_PAGE_SIZE))
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

        return stream(repository.listIssues(GHIssueState.CLOSED).withPageSize(REQUEST_PAGE_SIZE))
            .filter(ghIssue -> ghIssue.getMilestone() != null && ghIssue.getMilestone().getNumber() == milestoneRef.getNumber())
            .collect(toList());
    }

    private GHMilestone getMilestone(GHRepository repository, String milestone) {
        requireNonNull(repository);
        requireNonNull(milestone);

        return stream(repository.listMilestones(GHIssueState.ALL).withPageSize(REQUEST_PAGE_SIZE))
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
