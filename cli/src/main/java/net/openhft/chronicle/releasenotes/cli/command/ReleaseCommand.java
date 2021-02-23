package net.openhft.chronicle.releasenotes.cli.command;

import net.openhft.chronicle.releasenotes.cli.util.Git;
import net.openhft.chronicle.releasenotes.connector.ConnectorProvider;
import net.openhft.chronicle.releasenotes.connector.ConnectorProviderFactory;
import net.openhft.chronicle.releasenotes.connector.ConnectorProviderKeys;
import net.openhft.chronicle.releasenotes.connector.ReleaseConnector;
import net.openhft.chronicle.releasenotes.connector.ReleaseConnector.ReleaseResult;
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
        description = "Specifies a tag for which the release notes will get generated. "
                + "When using source BRANCH, the tag is used as a start point for the scanned commits",
        required = true
    )
    private String tag;

    @Option(
        names = {"-e", "--endTag"},
        description = "Specifies the ending tag for the scanned commits when using source BRANCH"
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
        description = "Specifies a branch that will be used as a reference for the issues included in the generated release notes"
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
        final String repository = Git.getCurrentRepository();

        final ConnectorProvider<ReleaseConnector> releaseConnectorProvider = ConnectorProviderFactory.getInstance()
            .getReleaseConnectorProvider(ConnectorProviderKeys.GITHUB)
            .orElseThrow(() -> new RuntimeException("Failed to find GitHub release provider"));

        final ReleaseConnector releaseConnector = releaseConnectorProvider.connect(token)
            .orElseThrow(() -> new RuntimeException("Failed to connect to GitHub"));

        switch (source) {
            case BRANCH:
                handleBranchSource(repository, releaseConnector);
                break;
            case MILESTONE:
                handleMilestoneSource(repository, releaseConnector);
                break;
            default:
                throw new RuntimeException("Invalid source: " + source);
        }
    }

    private void handleBranchSource(String repository, ReleaseConnector releaseConnector) {
        if (branch == null || branch.isEmpty()) {
            throw new RuntimeException("Using branch source, but no branch was specified: use --branch to specify target branch");
        }

        final ReleaseResult releaseResult = (endTag == null || endTag.isEmpty())
            ? releaseConnector.createReleaseFromBranch(repository, tag, branch, ignoreLabels, override)
            : releaseConnector.createReleaseFromBranch(repository, tag, endTag, branch, ignoreLabels, override);

        releaseResult.throwIfFail();

        System.out.println("Created release for tag '" + tag + "': " + releaseResult.getReleaseUrl().toString());
    }

    private void handleMilestoneSource(String repository, ReleaseConnector releaseConnector) {
        if (milestone == null || milestone.isEmpty()) {
            throw new RuntimeException("Using milestone source, but no milestone was specified: use --milestone to specify target milestone");
        }

        final ReleaseResult releaseResult = releaseConnector.createReleaseFromMilestone(repository, tag, milestone, ignoreLabels, override);

        releaseResult.throwIfFail();

        System.out.println("Created release for tag '" + tag + "': " + releaseResult.getReleaseUrl().toString());
    }

    public enum ReleaseSource {
        BRANCH,
        MILESTONE
    }
}