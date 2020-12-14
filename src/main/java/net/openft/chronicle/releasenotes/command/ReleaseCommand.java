package net.openft.chronicle.releasenotes.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "release",
    description = "Generates release notes for a specific tag"
)
public final class ReleaseCommand implements Runnable {

    @Option(
        names = "--tag",
        description = "Specifies a tag for which the release notes will get generated"
    )
    private String tag;

    @Option(
        names = "--milestone",
        description = "Specifies a milestone that will be used as a reference for the issues included in the generated release notes",
        required = true
    )
    private String milestone;

    @Option(
        names = "--token",
        description = "Specifies a GitHub personal access token used to gain access to the GitHub API",
        required = true
    )
    private String token;

    @Override
    public void run() {

    }
}
