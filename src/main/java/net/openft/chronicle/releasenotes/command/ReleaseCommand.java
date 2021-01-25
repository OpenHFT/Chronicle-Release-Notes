package net.openft.chronicle.releasenotes.command;

import net.openft.chronicle.releasenotes.git.Git;
import net.openft.chronicle.releasenotes.git.GitHubConnector;
import net.openft.chronicle.releasenotes.git.release.cli.ReleaseSource;
import org.kohsuke.github.GHIssueState;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Command(
    name = "release",
    description = "Generates release notes for a specific tag"
)
public final class ReleaseCommand implements Runnable {

    @Option(
        names = {"-t", "--tag"},
        description = "Specifies a tag for which the release notes will get generated",
        required = true
    )
    private String tag;

    @Option(
        names = {"-e", "--endTag"},
        description = ""
    )
    private String endTag;

    @Option(
        names = {"-s", "--source"},
        description = "Specifies the source used to fetch the issues included in the generated release notes. "
                    + "Valid source values: ${COMPLETION-CANDIDATES} (Default: ${DEFAULT-VALUE})",
        defaultValue = "BRANCH"
    )
    private ReleaseSource source;

    @Option(
        names = {"-b", "--branch"},
        description = "Specifies a branch that will be used as a reference for the issues included in teh generated release notes"
    )
    private String branch;

    @Option(
        names = {"-m", "--milestone"},
        description = "Specifies a milestone that will be used as a reference for the issues included in the generated release notes"
    )
    private String milestone;

    @Option(
        names = {"-i", "--ignoreLabels"},
        description = "Specifies which issues to ignore based on the provided label names",
        split = ",",
        arity = "1..*"
    )
    private List<String> ignoreLabels;

    @Option(
        names = {"-o", "--override"},
        description = "Specifies if the generated release notes should override an already existing release",
        defaultValue = "false"
    )
    private boolean override;

    @Option(
        names = {"-T", "--token"},
        description = "Specifies a GitHub personal access token used to gain access to the GitHub API",
        required = true,
        interactive = true,
        arity = "0..1"
    )
    private String token;

    @Override
    public void run() {
        final var repository = Git.getCurrentRepository();
        final var github = GitHubConnector.connectWithAccessToken(token);

        switch (source) {
            case BRANCH:
                handleBranchSource(repository, github);
                break;
            case MILESTONE:
                handleMilestoneSource(repository, github);
                break;
            default:
                throw new RuntimeException("Invalid source: " + source);
        }
    }

    private void handleBranchSource(String repository, GitHubConnector github) {
        if (branch == null || branch.isEmpty()) {
            throw new RuntimeException("Using branch source, but no branch was specified: use --branch to specify target branch");
        }

        if (endTag == null || endTag.isEmpty()) {
            // TODO: Find previous tag for branch
            throw new RuntimeException("Using branch source, but not end tag was specified: use --endTag to specify end tag");
        }

        final var commits = github.getCommitsForBranch(repository, branch, tag, endTag);

        final var issues = commits.stream()
            .map(github::extractIssuesFromCommit)
            .flatMap(Collection::stream)
            .filter(issue -> issue.getState() == GHIssueState.CLOSED)
            .collect(Collectors.toList());

        final var release = github.createRelease(github.getRepository(repository), tag, issues, ignoreLabels, override);

        System.out.println("Created release for tag '" + tag + "': " + release.getHtmlUrl().toString());
    }

    private void handleMilestoneSource(String repository, GitHubConnector github) {
        if (milestone == null || milestone.isEmpty()) {
            throw new RuntimeException("Using milestone source, but no milestone was specified: use --milestone to specify target milestone");
        }

        final var milestoneRef = github.getMilestone(repository, milestone);
        final var closedIssues = github.getClosedMilestoneIssues(milestoneRef);

        final var release = github.createRelease(milestoneRef.getOwner(), tag, closedIssues, ignoreLabels, override);

        System.out.println("Created release for tag '" + tag + "': " + release.getHtmlUrl().toString());
    }
}
