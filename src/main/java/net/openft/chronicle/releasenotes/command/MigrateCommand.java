package net.openft.chronicle.releasenotes.command;

import picocli.CommandLine.Command;

@Command(
    name = "migrate",
    description = "Migrates issues from one or more milestones to a target milestone",
    mixinStandardHelpOptions = true
)
public final class MigrateCommand implements Runnable {

    @Override
    public void run() {

    }
}
