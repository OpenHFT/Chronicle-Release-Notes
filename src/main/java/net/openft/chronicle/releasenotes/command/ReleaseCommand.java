package net.openft.chronicle.releasenotes.command;

import picocli.CommandLine.Command;

@Command(
    name = "release",
    description = "Generates release notes for a specific tag",
    mixinStandardHelpOptions = true
)
public final class ReleaseCommand implements Runnable {

    @Override
    public void run() {

    }
}
