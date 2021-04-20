package net.openhft.chronicle.releasenotes.cli.convertable;

import static java.util.Objects.requireNonNull;

import org.apache.logging.log4j.Level;
import picocli.CommandLine.ITypeConverter;

public class LevelConverter implements ITypeConverter<Level> {

    @Override
    public Level convert(String value) {
        requireNonNull(value);

        return Level.getLevel(value.toUpperCase());
    }
}
