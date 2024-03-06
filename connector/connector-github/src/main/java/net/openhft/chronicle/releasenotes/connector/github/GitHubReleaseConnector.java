package net.openhft.chronicle.releasenotes.connector.github;

import net.openhft.chronicle.releasenotes.connector.ConnectorProviderKey;
import net.openhft.chronicle.releasenotes.connector.ReleaseConnector;
import net.openhft.chronicle.releasenotes.connector.github.graphql.GitHubGraphQLClient;
import net.openhft.chronicle.releasenotes.connector.github.graphql.model.Tag;
import net.openhft.chronicle.releasenotes.creator.ReleaseNoteCreator;
import net.openhft.chronicle.releasenotes.model.*;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;

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
        this(token, LoggerFactory.getLogger(GitHubReleaseConnector.class));
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
    public ReleaseResult<ReleaseNotes> createReleaseFromBranch(String repository, String tag, String branch, BranchReleaseOptions releaseOptions) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(branch);

        logger.info("Creating release in repository '{}' for tag '{}' on branch '{}'", repository, tag, branch);
        logger.debug("{}", releaseOptions);

        final GHRepository repositoryRef;

        try {
            repositoryRef = getRepository(repository);

            return getOrCreateRelease(
                repositoryRef,
                tag,
                releaseOptions.getTitle() != null ? releaseOptions.getTitle() : tag,
                () -> getIssuesForBranch(repositoryRef, branch, tag, releaseOptions.includeIssuesWithoutClosingKeyword(), releaseOptions.includePullRequests()),
                releaseOptions.getIgnoredLabels(),
                releaseOptions.overrideRelease() ? ReleaseAction.CREATE_OR_UPDATE : ReleaseAction.CREATE,
                releaseOptions.includeAdditionalContext()
            );
        } catch (RuntimeException e) {
            return ReleaseResult.fail(e);
        }
    }

    @Override
    public ReleaseResult<ReleaseNotes> createReleaseFromBranch(String repository, String tag, String endTag, String branch, BranchReleaseOptions releaseOptions) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(endTag);
        requireNonNull(branch);

        logger.info("Creating release in repository '{}' for tag '{}' until tag '{}' on branch '{}'", repository, tag, endTag, branch);
        logger.debug("{}", releaseOptions);

        final GHRepository repositoryRef;

        try {
            repositoryRef = getRepository(repository);

            return getOrCreateRelease(
                repositoryRef,
                tag,
                releaseOptions.getTitle() != null ? releaseOptions.getTitle() : tag,
                () -> getIssuesForBranch(repositoryRef, branch, tag, endTag, releaseOptions.includeIssuesWithoutClosingKeyword(), releaseOptions.includePullRequests()),
                releaseOptions.getIgnoredLabels(),
                releaseOptions.overrideRelease() ? ReleaseAction.CREATE_OR_UPDATE : ReleaseAction.CREATE,
                releaseOptions.includeAdditionalContext()
            );
        } catch (RuntimeException e) {
            return ReleaseResult.fail(e);
        }
    }

    @Override
    public ReleaseResult<ReleaseNotes> createReleaseFromMilestone(String repository, String tag, String milestone, MilestoneReleaseOptions releaseOptions) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(milestone);

        logger.info("Creating release in repository '{}' for tag '{}' from milestone '{}'", repository, tag, milestone);
        logger.debug("{}", releaseOptions);

        final GHRepository repositoryRef;

        try {
            repositoryRef = getRepository(repository);

            return getOrCreateRelease(
                repositoryRef,
                tag,
                tag,
                () -> getClosedMilestoneIssues(repositoryRef, milestone),
                releaseOptions.getIgnoredLabels(),
                releaseOptions.overrideRelease() ? ReleaseAction.CREATE_OR_UPDATE : ReleaseAction.CREATE,
                releaseOptions.includeAdditionalContext()
            );
        } catch (RuntimeException e) {
            return ReleaseResult.fail(e);
        }
    }

    @Override
    public ReleaseResult<AggregatedReleaseNotes> createAggregatedRelease(String repository, String tag, Map<String, List<String>> releases, AggregateReleaseOptions releaseOptions) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(releases);

        if (logger.isInfoEnabled()) {
            logger.info("Creating aggregated release in repository '{}' for tag '{}'", repository, tag);

            final StringBuilder formattedReleases = new StringBuilder();

            releases.forEach((k, v) -> {
                formattedReleases.append(k).append(":\n");

                v.forEach(release -> formattedReleases.append("\t- ").append(release).append('\n'));

                formattedReleases.append('\n');
            });

            logger.info("Releases: \n{}", formattedReleases);
        }

        final GHRepository repositoryRef;

        try {
            repositoryRef = getRepository(repository);
        } catch (RuntimeException e) {
            return ReleaseResult.fail(e);
        }

        try {
            if (!checkTagExists(repositoryRef, tag)) {
                return ReleaseResult.fail(new RuntimeException("Tag '" + tag + "' not found"));
            }

            final GHRelease remoteRelease = repositoryRef.getReleaseByTagName(tag);

            final StringJoiner missingRepositoriesJoiner = new StringJoiner(", ");
            final StringJoiner missingReleasesJoiner = new StringJoiner(", ");

            final Map<String, Map<String, GHRelease>> sourceReleases = releases.entrySet().stream()
                .collect(toMap(Entry::getKey, entry -> {
                    final GHRepository sourceRepository;

                    try {
                        sourceRepository = getRepository(entry.getKey());
                    } catch (RuntimeException e) {
                        missingRepositoriesJoiner.add(entry.getKey());
                        return new HashMap<>();
                    }

                    return entry.getValue().stream()
                        .collect(HashMap::new, (m, v) -> {
                            final GHRelease release;

                            try {
                                release = getRelease(sourceRepository, v);
                            } catch (RuntimeException e) {
                                missingReleasesJoiner.add(sourceRepository.getName() + ":" + v);
                                return;
                            }

                            m.put(v, release);
                        }, HashMap::putAll);
                }));

            final String missingRepositories = missingRepositoriesJoiner.toString();
            final String missingReleases = missingReleasesJoiner.toString();

            if (!missingRepositories.isEmpty() || !missingReleases.isEmpty()) {
                final StringBuilder exceptionMessage = new StringBuilder();

                if (!missingRepositories.isEmpty()) {
                    exceptionMessage.append("Repositories [").append(missingRepositories).append("] not found");
                }

                if (!missingReleases.isEmpty()) {
                    if (exceptionMessage.length() != 0) {
                        exceptionMessage.append('\n');
                    }

                    exceptionMessage.append("Releases [").append(missingReleases).append("] not found");
                }

                return ReleaseResult.fail(new RuntimeException(exceptionMessage.toString()));
            }

            final List<ReleaseNotes> normalizedReleaseNotes = sourceReleases.values().stream()
                .map(stringGHReleaseMap -> stringGHReleaseMap.entrySet().stream().map(
                    entry -> {
                        final GHRelease release = entry.getValue();

                        if (release == null) {
                            return new ReleaseNotes(entry.getKey(), entry.getKey(), Collections.emptyList());
                        }

                        return new ReleaseNotes(release.getTagName(), release.getName(), Collections.emptyList());
                    }).collect(toList()))
                .flatMap(Collection::stream)
                .collect(toList());

            AggregatedReleaseNotes aggregatedReleaseNotes = new AggregatedReleaseNotes(tag, tag, normalizedReleaseNotes);

            final String body = releaseNoteCreator.formatAggregatedReleaseNotes(aggregatedReleaseNotes);

            if (remoteRelease != null) {
                if (!releaseOptions.overrideRelease()) {
                    return ReleaseResult.fail(new RuntimeException("Release for tag '" + tag + "' already exists"));
                }

                final GHRelease release = remoteRelease.update().name(tag).body(body).update();

                return ReleaseResult.success(aggregatedReleaseNotes, release.getHtmlUrl());
            }

            final GHRelease release = repositoryRef.createRelease(tag).name(tag).body(body).create();

            return ReleaseResult.success(aggregatedReleaseNotes, release.getHtmlUrl());
        } catch (IOException e) {
            return ReleaseResult.fail(new RuntimeException("Failed to create release for tag '" + tag + "'"));
        }
    }

    @Override
    public ReleaseResult<AggregatedReleaseNotes> createAggregatedRelease(String repository, String tag, List<ReleaseNotes> releaseNotes, AggregateReleaseOptions releaseOptions) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(releaseNotes);

        final GHRepository repositoryRef = getRepository(repository);

        try {
            if (!checkTagExists(repositoryRef, tag)) {
                return ReleaseResult.fail(new RuntimeException("Tag '" + tag + "' not found"));
            }

            final GHRelease remoteRelease = repositoryRef.getReleaseByTagName(tag);

            final AggregatedReleaseNotes aggregatedReleaseNotes = new AggregatedReleaseNotes(tag, tag, releaseNotes);

            final String body = releaseNoteCreator.formatAggregatedReleaseNotes(aggregatedReleaseNotes);

            if (remoteRelease != null) {
                if (!releaseOptions.overrideRelease()) {
                    return ReleaseResult.fail(new RuntimeException("Release for tag '" + tag + "' already exists"));
                }

                final GHRelease release = remoteRelease.update().name(tag).body(body).update();

                return ReleaseResult.success(aggregatedReleaseNotes, release.getHtmlUrl());
            }

            final GHRelease release = repositoryRef.createRelease(tag).name(tag).body(body).create();

            return ReleaseResult.success(aggregatedReleaseNotes, release.getHtmlUrl());
        } catch (IOException e) {
            return ReleaseResult.fail(new RuntimeException("Failed to create release for tag '" + tag + "'"));
        }
    }

    @Override
    public ReleaseResult<ReleaseNotes> queryReleaseFromBranch(String repository, String tag, String branch, BranchReleaseOptions releaseOptions) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(branch);

        logger.info("Querying release in repository '{}' for tag '{}' on branch '{}'", repository, tag, branch);
        logger.debug("{}", releaseOptions);

        final GHRepository repositoryRef;

        try {
            repositoryRef = getRepository(repository);

            return getOrCreateRelease(
                    repositoryRef,
                    tag,
                    releaseOptions.getTitle() != null ? releaseOptions.getTitle() : tag,
                    () -> getIssuesForBranch(repositoryRef, branch, tag, releaseOptions.includeIssuesWithoutClosingKeyword(), releaseOptions.includePullRequests()),
                    releaseOptions.getIgnoredLabels(),
                    ReleaseAction.QUERY,
                    releaseOptions.includeAdditionalContext()
            );
        } catch (RuntimeException e) {
            return ReleaseResult.fail(e);
        }
    }

    public Issue getIssue(String repository, int number) throws IOException {

        final GHRepository repositoryRef;

            repositoryRef = getRepository(repository);

            return mapIssue(repositoryRef.getIssue(number), true);
    }

    @Override
    public ReleaseResult<Issue> createIssueComment(String repository, int number, String message) {
        requireNonNull(repository);
        requireNonNull(message);

        logger.info("Commenting issue #{} in repository '{}'", number, repository);

        final GHRepository repositoryRef;

        try {
            repositoryRef = getRepository(repository);

            GHIssue issue = repositoryRef.getIssue(number);

            // Some check to make sure the issue is valid?

            issue.comment(message);

            return ReleaseResult.success(mapIssue(issue, false), issue.getHtmlUrl());
        } catch (IOException e) {
            return ReleaseResult.fail(new RuntimeException("Failed to comment issue #" + number));
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

    private enum ReleaseAction {
        CREATE,
        CREATE_OR_UPDATE,
        QUERY;

        public String displayName() {
            return name().toLowerCase().replace('_', ' ');
        }
    }

    private ReleaseResult<ReleaseNotes> getOrCreateRelease(GHRepository repository, String tag, String title, Supplier<List<GHIssue>> issueSupplier, List<String> ignoredLabels, ReleaseAction action, boolean includeAdditionalContext) {
        requireNonNull(repository);
        requireNonNull(tag);
        requireNonNull(issueSupplier);

        try {
            if (!checkTagExists(repository, tag)) {
                return ReleaseResult.fail(new RuntimeException("Tag '" + tag + "' not found"));
            }

            final List<Issue> issues = filterIssueLabels(issueSupplier.get(), ignoredLabels)
                .stream()
                .map(issue -> mapIssue(issue, includeAdditionalContext))
                .collect(toList());

            final ReleaseNotes releaseNotes = new ReleaseNotes(tag, title, issues);

            final String body = releaseNoteCreator.formatReleaseNotes(releaseNotes);

            final GHRelease remoteRelease = repository.getReleaseByTagName(tag);

            if (remoteRelease != null) {
                if (action == ReleaseAction.CREATE) {
                    return ReleaseResult.fail(new RuntimeException("Release for tag '" + tag + "' already exists"));
                }

                URL htmlUrl;
                if (action == ReleaseAction.CREATE_OR_UPDATE) {
                    final GHRelease release = remoteRelease.update().name(title).body(body).update();

                    htmlUrl = release.getHtmlUrl();
                } else {
                    htmlUrl = remoteRelease.getHtmlUrl();
                }

                return ReleaseResult.success(releaseNotes, htmlUrl);
            } else if (action == ReleaseAction.QUERY) {
                return ReleaseResult.fail(new RuntimeException("Release for tag '" + tag + "' does not exists"));
            }

            final GHRelease release = repository.createRelease(releaseNotes.getTag()).name(title).body(body).create();

            return ReleaseResult.success(releaseNotes, release.getHtmlUrl());
        } catch (IOException e) {
            return ReleaseResult.fail(new RuntimeException("Failed to " + action.displayName() + " release for tag '" + tag + "'"));
        }
    }

    private GHRepository getRepository(String repository) {
        requireNonNull(repository);

        logger.debug("Fetching repository '{}'", repository);

        try {
            return github.getRepository(repository);
        } catch (IOException e) {
            throw new RuntimeException("Repository '" + repository + "' not found");
        }
    }

    private GHBranch getBranch(GHRepository repository, String branch) {
        requireNonNull(repository);
        requireNonNull(branch);

        logger.debug("Fetching branch '{}' in repository '{}'", branch, repository.getFullName());

        try {
            return repository.getBranch(branch);
        } catch (IOException e) {
            throw new RuntimeException("Branch '" + branch + "' not found in repository '" + repository + "'");
        }
    }

    private GHRelease getRelease(GHRepository repository, String tag) {
        requireNonNull(repository);
        requireNonNull(tag);

        logger.debug("Fetching release for tag '{}' in repository '{}'", tag, repository.getFullName());

        try {
            return repository.getReleaseByTagName(tag);
        } catch (IOException e) {
            throw new RuntimeException("Failed to find release for tag '" + tag + "' in repository '" + repository.getName() + "'");
        }
    }

    private Map<String, GHTag> getTags(GHRepository repository, String... tags) {
        requireNonNull(repository);
        requireNonNull(tags);

        logger.debug("Fetching tags [{}] in repository '{}'", String.join(", ", tags), repository.getFullName());

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

        logger.debug("Fetching tag before tag '{}' in repository '{}' on branch '{}'", tag.getName(), repository.getFullName(), branch.getName());

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

        logger.debug("Checking if tag '{}' exists in repository '{}'", tag, repository.getFullName());

        try {
            return stream(repository.listTags().withPageSize(REQUEST_PAGE_SIZE))
                    .anyMatch(ghTag -> ghTag.getName().equals(tag));
        } catch (IOException e) {
            return false;
        }
    }

    private List<GHCommit> getCommitsForBranchFromTag(GHRepository repository, GHBranch branch, GHTag tag) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(tag);

        logger.debug("Fetching commits for branch '{}' from tag '{}' in repository '{}'", branch.getName(), tag.getName(), repository.getFullName());

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

    private List<GHIssue> getIssuesForBranch(GHRepository repository, String branch, String startTag, boolean includeIssuesWithoutClosingKeyword, boolean includePullRequests) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(startTag);

        return getIssuesForBranch(repository, branch, startTag, null, includeIssuesWithoutClosingKeyword, includePullRequests);
    }

    private List<GHIssue> getIssuesForBranch(GHRepository repository, String branch, String startTag, String endTag, boolean includeIssuesWithoutClosingKeyword, boolean includePullRequests) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(startTag);

        logger.debug("Fetching issues on branch '{}' between tags '{}' and '{}' in repository '{}'", branch, startTag, endTag, repository.getFullName());

        final List<GHCommit> commits = getCommitsForBranch(repository, branch, startTag, endTag);
        final Set<Integer> issueIds = extractIssueIdsFromCommits(commits, includeIssuesWithoutClosingKeyword);
        if (includePullRequests) {
            extractPullRequestIdsFromCommits(commits, branch, issueIds);
        }
        return getIssuesFromIds(repository, issueIds, includePullRequests);
    }

    private List<GHCommit> getCommitsForBranch(GHRepository repository, String branch, String startTag, String endTag) {
        requireNonNull(repository);
        requireNonNull(branch);
        requireNonNull(startTag);

        logger.debug("Fetching commits for branch '{}' between tags '{}' and '{}' in repository '{}'", branch, startTag, endTag, repository.getFullName());

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

    private Set<Integer> extractIssueIdsFromCommits(List<GHCommit> commits, boolean includeIssuesWithoutClosingKeyword) {
        logger.debug("Extracting issue ids from {} commits", commits.size());
        return commits.stream()
                .map(commit -> {
                    final Set<Integer> ids = new HashSet<>();
                    extractIssueIdsFromCommit(commit, includeIssuesWithoutClosingKeyword, ids);
                    return ids;
                }).flatMap(Collection::stream)
                .collect(toSet());
    }

    private void extractPullRequestIdsFromCommits(List<GHCommit> commits, String branch, Set<Integer> issueIds) {
        logger.debug("Extracting issue ids from {} commits", commits.size());
        commits.forEach(commit -> updatePullRequestIds(commit, issueIds, branch));
    }

    private void extractIssueIdsFromCommit(GHCommit commit, boolean includeIssuesWithoutClosingKeyword, Set<Integer> ids) {
        requireNonNull(commit);

        logger.debug("Extracting issue ids from commit '{}' in repository '{}'", commit.getSHA1(), commit.getOwner().getFullName());

        try {
            getIssueIdsFromCommitMessage(commit, includeIssuesWithoutClosingKeyword, ids);
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch commit info for commit '" + commit.getSHA1() + "'");
        }
    }

    private void updatePullRequestIds(GHCommit commit, Set<Integer> ids, String branch) {
        PagedIterable<GHPullRequest> ghPullRequests = commit.listPullRequests().withPageSize(REQUEST_PAGE_SIZE);
        for (GHPullRequest ghPullRequest : ghPullRequests) {
//            skip pull request that go from release branch back to development branch
            String ref = ghPullRequest.getHead().getRef();
            if (ref.equals(branch)) continue;
            ids.add(ghPullRequest.getNumber());
        }
    }

    private void getIssueIdsFromCommitMessage(GHCommit commit, boolean includeIssuesWithoutClosingKeyword, Set<Integer> ids) throws IOException {
        final String commitMessage = commit.getCommitShortInfo().getMessage()
                .replaceAll("\n", " ")
                .replaceAll("\r", "")
                .replaceAll(" +", " ")
                .replaceAll(",", "");

        final String[] tokens = commitMessage.split(" ");
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

    private List<GHIssue> getIssuesFromIds(GHRepository repository, Set<Integer> ids, boolean includePullRequests) {
        requireNonNull(repository);

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        logger.debug("Fetching {} issues from repository '{}'", ids.size(), repository.getFullName());

        return stream(repository.listIssues(GHIssueState.CLOSED).withPageSize(REQUEST_PAGE_SIZE))
            .filter(ghIssue -> ids.contains(ghIssue.getNumber()) && (includePullRequests || !ghIssue.isPullRequest()))
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

    private FullIssue mapIssue(GHIssue issue, boolean includeAdditionalContext) {
        return new FullIssue(
            issue.getNumber(),
            issue.getTitle(),
            issue.getLabels().stream().map(GHLabel::getName).collect(toList()),
            mapIssueComments(issue, includeAdditionalContext),
            issue.getHtmlUrl()
        );
    }

    private List<IssueComment> mapIssueComments(GHIssue issue, boolean includeAdditionalContext) {
        if (!includeAdditionalContext) {
            return Collections.emptyList();
        }

        try {
            return issue.getComments().stream().map(this::mapIssueComment).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch comments for issue #" + issue.getNumber());
        }
    }

    private IssueComment mapIssueComment(GHIssueComment issueComment) {
        try {
            return new IssueComment(issueComment.getBody(), issueComment.getCreatedAt());
        } catch (IOException e) {
            throw new RuntimeException("Failed to map issue comment to internal model");
        }
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
