package net.openhft.chronicle.releasenotes.cli;

import net.openhft.chronicle.releasenotes.cli.command.AggregateCommand;
import net.openhft.chronicle.releasenotes.cli.command.MigrateCommand;
import net.openhft.chronicle.releasenotes.cli.command.ReleaseCommand;
import net.openhft.chronicle.releasenotes.cli.convertable.LevelConverter;
import net.openhft.chronicle.releasenotes.cli.convertable.ReleaseReference;
import net.openhft.chronicle.releasenotes.cli.convertable.ReleaseReferenceConverter;
import net.openhft.chronicle.releasenotes.cli.mixin.CommandPresetMixin;
import net.openhft.chronicle.releasenotes.cli.mixin.LoggingMixin;
import net.openhft.chronicle.releasenotes.cli.util.Git;
import org.apache.logging.log4j.Level;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Mixin;
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

    @Mixin
    private LoggingMixin loggingMixin;

    public LoggingMixin getLoggingMixin() {
        return loggingMixin;
    }

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
        final CommandLine commandLine = new CommandLine(new ChronicleReleaseNotes());

        registerMixins(commandLine);
        commandLine.setExecutionStrategy(LoggingMixin::executionStrategy);
        commandLine.registerConverter(ReleaseReference.class, new ReleaseReferenceConverter());
        commandLine.registerConverter(Level.class, new LevelConverter());
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        commandLine.setUsageHelpAutoWidth(true);

        commandLine.setExecutionExceptionHandler((exception, cmdLine, parseResult) -> {
            cmdLine.getErr().println(cmdLine.getColorScheme().errorText(exception.getMessage() != null ? exception.getMessage() : "An error has occurred"));

            return cmdLine.getExitCodeExceptionMapper() != null
                ? cmdLine.getExitCodeExceptionMapper().getExitCode(exception)
                : cmdLine.getCommandSpec().exitCodeOnExecutionException();
        });

        commandLine.execute(args);

        System.exit(0);
    }

    private static void registerMixins(CommandLine commandLine) {
        commandLine.addMixin("commandPreset", new CommandPresetMixin());
        commandLine.getSubcommands().values().forEach(subcommand -> subcommand.addMixin("commandPreset", new CommandPresetMixin()));

        commandLine.getSubcommands().values().forEach(subcommand -> subcommand.addMixin("logging", new LoggingMixin()));
    }

    protected static final class VersionProvider implements IVersionProvider {

        @Override
        public String[] getVersion() {
            return new String[] {
                getClass().getPackage().getImplementationVersion()
            };
        }
    }
}
