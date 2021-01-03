package net.openft.chronicle.releasenotes.command;

import net.openft.chronicle.releasenotes.git.Git;
import net.openft.chronicle.releasenotes.git.GitHubConnector;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.stream.Collectors;

@Command(
    name = "migrate",
    description = "Migrates issues from one or more milestones to a target milestone"
)
public final class MigrateCommand implements Runnable {

    @Option(
        names = {"-f", "--from"},
        description = "Specifies one or more milestones that will be used as a migration source",
        required = true,
        split = ",",
        arity = "1..*"
    )
    private List<String> from;

    @Option(
        names = {"-t", "--to"},
        description = "Specifies a milestone that will be used as a migration destination",
        required = true
    )
    private String to;

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
        final var gitHub = GitHubConnector.connectWithAccessToken(token);

        final var fromMilestones = from.stream().map(x -> gitHub.getMilestone(repository, x)).collect(Collectors.toList());
        final var toMilestone = gitHub.getMilestone(repository, to);

        gitHub.migrateIssues(fromMilestones, toMilestone, ignoreLabels);
    }
}
