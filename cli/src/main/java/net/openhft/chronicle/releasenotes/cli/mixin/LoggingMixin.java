package net.openhft.chronicle.releasenotes.cli.mixin;

import static java.util.Objects.requireNonNull;

import net.openhft.chronicle.releasenotes.cli.ChronicleReleaseNotes;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

import java.util.function.Consumer;

public final class LoggingMixin {

    @Spec(Target.MIXEE)
    private CommandSpec mixee;

    private Level level = Level.INFO;

    @Option(names = {"-l", "--log-level"})
    public void setLevel(Level level) {
        executeOnRootMixin(loggingMixin -> loggingMixin.level = level);
    }

    public static int executionStrategy(ParseResult parseResult) {
        requireNonNull(parseResult);

        executeOnRootMixin(parseResult.commandSpec(), LoggingMixin::configureLoggers);
        return new CommandLine.RunLast().execute(parseResult);
    }

    public void configureLoggers() {
        executeOnRootMixin(loggingMixin -> {
            final Level rootLevel = loggingMixin.level;

            final LoggerContext loggerContext = LoggerContext.getContext(false);
            final LoggerConfig rootConfig = loggerContext.getConfiguration().getRootLogger();

            rootConfig.getAppenders().values()
                .stream()
                .filter(appender -> appender instanceof ConsoleAppender)
                .forEach(appender -> {
                    rootConfig.removeAppender(appender.getName());
                    rootConfig.addAppender(appender, rootLevel, null);
                });

            if (rootConfig.getLevel().isMoreSpecificThan(rootLevel)) {
                rootConfig.setLevel(rootLevel);
            }

            loggerContext.updateLoggers();
        });
    }

    private void executeOnRootMixin(Consumer<LoggingMixin> loggingMixinConsumer) {
        executeOnRootMixin(mixee, loggingMixinConsumer);
    }

    private static void executeOnRootMixin(CommandSpec commandSpec, Consumer<LoggingMixin> loggingMixinConsumer) {
        requireNonNull(commandSpec);
        requireNonNull(loggingMixinConsumer);

        loggingMixinConsumer.accept(((ChronicleReleaseNotes) commandSpec.root().userObject()).getLoggingMixin());
    }
}
