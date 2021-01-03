package net.openft.chronicle.releasenotes.command;

import net.openft.chronicle.releasenotes.git.Git;
import net.openft.chronicle.releasenotes.git.GitHubConnector;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

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
        names = {"-m", "--milestone"},
        description = "Specifies a milestone that will be used as a reference for the issues included in the generated release notes",
        required = true
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

        final var milestoneRef = github.getMilestone(repository, milestone);

        final var release = github.createRelease(tag, milestoneRef, ignoreLabels);

        System.out.println("Created release for tag '" + tag + "': " + release.getHtmlUrl().toString());
    }
}
