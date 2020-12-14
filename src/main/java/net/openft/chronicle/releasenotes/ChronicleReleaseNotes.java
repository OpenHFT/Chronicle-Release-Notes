package net.openft.chronicle.releasenotes;

import net.openft.chronicle.releasenotes.command.MigrateCommand;
import net.openft.chronicle.releasenotes.command.ReleaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "chronicle-release-notes",
    description = "Handles release note generation and issue migration between milestones",
    subcommands = {
        ReleaseCommand.class,
        MigrateCommand.class
    },
    mixinStandardHelpOptions = true
)
public final class ChronicleReleaseNotes {

    public static void main(String[] args) {
        var commandLine = new CommandLine(new ChronicleReleaseNotes());

        commandLine.addMixin("commandPreset", new CommandPreset());
        commandLine.getSubcommands().values().forEach(subcommand -> subcommand.addMixin("commandPreset", new CommandPreset()));

        commandLine.execute(args);
    }

    @Command(
        descriptionHeading   = "%nDescription: ",
        parameterListHeading = "%nParameters:%n%n",
        optionListHeading    = "%nOptions:%n",
        commandListHeading   = "%nCommands:%n"
    )
    private static final class CommandPreset {}
}
