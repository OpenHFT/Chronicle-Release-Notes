package net.openhft.chronicle.releasenotes.cli.command;

import org.slf4j.event.Level;
import picocli.CommandLine.Option;

public final class LoggingCommand implements Runnable {
    private String level = Level.INFO.toString();

    @Option(names = {"-l", "--log-level"})
    public void setLevel(String level) {
        this.level = level;
    }

    @Override
    public void run() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, level);
    }
}
