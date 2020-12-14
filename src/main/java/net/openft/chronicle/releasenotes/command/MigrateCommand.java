package net.openft.chronicle.releasenotes.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

@Command(
    name = "migrate",
    description = "Migrates issues from one or more milestones to a target milestone"
)
public final class MigrateCommand implements Runnable {

    @Option(
        names = "--from",
        description = "Specifies one or more milestones that will be used as a migration source",
        required = true,
        split = ",",
        arity = "1..*"
    )
    private List<String> from;

    @Option(
        names = "--to",
        description = "Specifies a milestone that will be used as a migration destination",
        required = true
    )
    private String to;

    @Option(
        names = "--token",
        description = "Specifies a GitHub personal access token used to gain access to the GitHub API",
        required = true,
        interactive = true,
        arity = "0..1"
    )
    private String token;

    @Override
    public void run() {

    }
}
