package net.openft.chronicle.releasenotes;

import net.openft.chronicle.releasenotes.command.AggregateCommand;
import net.openft.chronicle.releasenotes.command.MigrateCommand;
import net.openft.chronicle.releasenotes.command.ReleaseCommand;
import net.openft.chronicle.releasenotes.git.Git;
import net.openft.chronicle.releasenotes.git.release.cli.ReleaseReference;
import net.openft.chronicle.releasenotes.git.release.cli.ReleaseReferenceConverter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(
    name = "chronicle-release-notes",
    description = "Handles release note generation and issue migration between milestones",
    subcommands = {
        ReleaseCommand.class,
        AggregateCommand.class,
        MigrateCommand.class
    },
    versionProvider = ChronicleReleaseNotes.VersionProvider.class,
    mixinStandardHelpOptions = true
)
public final class ChronicleReleaseNotes {

    @Option(
        names = {"-r", "--repository"},
        description = "Specifies a target Git repository",
        scope = ScopeType.INHERIT
    )
    void setRepository(String repository) {
        if (repository != null) {
            Git.setConfiguredRepository(repository);
        }
    }

    public static void main(String[] args) {
        final var commandLine = new CommandLine(new ChronicleReleaseNotes());

        commandLine.addMixin("commandPreset", new CommandPreset());
        commandLine.getSubcommands().values().forEach(subcommand -> subcommand.addMixin("commandPreset", new CommandPreset()));
        commandLine.registerConverter(ReleaseReference.class, new ReleaseReferenceConverter());

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
