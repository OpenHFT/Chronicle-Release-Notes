package net.openhft.chronicle.releasenotes.cli.command;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

import net.openhft.chronicle.releasenotes.cli.convertable.ReleaseReference;
import net.openhft.chronicle.releasenotes.cli.util.Git;
import net.openhft.chronicle.releasenotes.connector.ConnectorProvider;
import net.openhft.chronicle.releasenotes.connector.ConnectorProviderFactory;
import net.openhft.chronicle.releasenotes.connector.ConnectorProviderKeys;
import net.openhft.chronicle.releasenotes.connector.ReleaseConnector;
import net.openhft.chronicle.releasenotes.connector.ReleaseConnector.ReleaseResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Map;
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
        final String repository = Git.getCurrentRepository();

        final ConnectorProvider<ReleaseConnector> releaseConnectorProvider = ConnectorProviderFactory
                .getInstance()
                .getReleaseConnectorProvider(ConnectorProviderKeys.GITHUB)
                .orElseThrow(() -> new RuntimeException("Failed to find GitHub release provider"));

        final ReleaseConnector releaseConnector = releaseConnectorProvider.connect(token)
                .orElseThrow(() -> new RuntimeException("Failed to connect to GitHub"));

        final Map<String, List<String>> releaseRef = releases.stream()
            .distinct()
            .collect(groupingBy(ReleaseReference::getRepository, mapping(ReleaseReference::getRelease, Collectors.toList())));

        final ReleaseResult releaseResult = releaseConnector.createAggregatedRelease(repository, tag, releaseRef, override);

        releaseResult.throwIfFail();

        System.out.println("Created release for tag '" + tag + "': " + releaseResult.getReleaseUrl());
    }
}
