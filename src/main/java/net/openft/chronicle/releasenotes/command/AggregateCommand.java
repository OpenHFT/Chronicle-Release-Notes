package net.openft.chronicle.releasenotes.command;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

import net.openft.chronicle.releasenotes.git.Git;
import net.openft.chronicle.releasenotes.git.GitHubConnector;
import net.openft.chronicle.releasenotes.git.release.cli.ReleaseReference;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Set;
import java.util.stream.Collectors;

@Command(
    name = "aggregate",
    description = "Generates aggregated release notes from a set of releases"
)
public final class AggregateCommand implements Runnable {

    @Option(
        names = {"-t", "--tag"},
        description = "Specifies a tag for which the release notes will get generated",
        required = true
    )
    private String tag;

    @Option(
        names = {"-R", "--releases"},
        description = "Specifies which releases to include in the aggregated release notes",
        split = ",",
        arity = "0..*"
    )
    private Set<ReleaseReference> releases;

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

        final var releaseRef = releases.stream()
            .distinct()
            .collect(groupingBy(ReleaseReference::getRepository, mapping(ReleaseReference::getRelease, Collectors.toSet())));

        final var release = github.createAggregatedRelease(repository, tag, releaseRef, override);

        System.out.println("Created release for tag '" + tag + "': " + release.getHtmlUrl().toString());
    }
}
