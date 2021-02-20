package net.openhft.chronicle.releasenotes.cli.command;

import net.openhft.chronicle.releasenotes.cli.util.Git;
import net.openhft.chronicle.releasenotes.connector.ConnectorProvider;
import net.openhft.chronicle.releasenotes.connector.ConnectorProviderFactory;
import net.openhft.chronicle.releasenotes.connector.ConnectorProviderKeys;
import net.openhft.chronicle.releasenotes.connector.MigrateConnector;
import net.openhft.chronicle.releasenotes.connector.MigrateConnector.MigrateResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Set;

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
    private Set<String> from;

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
    private Set<String> ignoreLabels;

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

        final ConnectorProvider<MigrateConnector> migrateConnectorProvider = ConnectorProviderFactory.getInstance()
            .getMigrateConnectorProvider(ConnectorProviderKeys.GITHUB)
            .orElseThrow(() -> new RuntimeException("Failed to find GitHub migration provider"));

        final MigrateConnector migrateConnector = migrateConnectorProvider.connect(token)
            .orElseThrow(() -> new RuntimeException("Failed to connect to GitHub"));

        final MigrateResult migrateResult = migrateConnector.migrateMilestones(repository, from, to, ignoreLabels);

        migrateResult.throwIfFail();
    }
}
