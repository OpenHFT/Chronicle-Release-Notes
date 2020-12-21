package net.openft.chronicle.releasenotes;

import net.openft.chronicle.releasenotes.command.MigrateCommand;
import net.openft.chronicle.releasenotes.command.ReleaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;

@Command(
    name = "chronicle-release-notes",
    description = "Handles release note generation and issue migration between milestones",
    subcommands = {
        ReleaseCommand.class,
        MigrateCommand.class
    },
    versionProvider = ChronicleReleaseNotes.VersionProvider.class,
    mixinStandardHelpOptions = true
)
public final class ChronicleReleaseNotes {

    public static void main(String[] args) {
        var commandLine = new CommandLine(new ChronicleReleaseNotes());

        commandLine.addMixin("commandPreset", new CommandPreset());
        commandLine.getSubcommands().values().forEach(subcommand -> subcommand.addMixin("commandPreset", new CommandPreset()));

        commandLine.setExecutionExceptionHandler((exception, cmdLine, parseResult) -> {
            cmdLine.getErr().println(cmdLine.getColorScheme().errorText(exception.getMessage()));

            return cmdLine.getExitCodeExceptionMapper() != null
                ? cmdLine.getExitCodeExceptionMapper().getExitCode(exception)
                : cmdLine.getCommandSpec().exitCodeOnExecutionException();
        });

        commandLine.execute(args);
    }

    @Command(
        descriptionHeading   = "%nDescription: ",
        parameterListHeading = "%nParameters:%n%n",
        optionListHeading    = "%nOptions:%n",
        commandListHeading   = "%nCommands:%n",
        sortOptions = false
    )
    private static final class CommandPreset {}

    protected static final class VersionProvider implements IVersionProvider {

        @Override
        public String[] getVersion() {
            return new String[] {
                getClass().getPackage().getImplementationVersion()
            };
        }
    }
}
