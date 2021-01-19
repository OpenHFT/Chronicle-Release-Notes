package net.openft.chronicle.releasenotes.git.release.cli;

import static java.util.Objects.requireNonNull;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

import java.util.regex.Pattern;

public final class ReleaseReferenceConverter implements ITypeConverter<ReleaseReference> {

    private static final Pattern RELEASE_PATTERN = Pattern.compile("^[a-z0-9-_.]+/[a-z0-9-_.]+:[a-z0-9-_./@$#!()]+$");

    @Override
    public ReleaseReference convert(String value) {
        requireNonNull(value);

        if (!RELEASE_PATTERN.matcher(value).find()) {
            throw new TypeConversionException("Invalid format: must be 'owner/repository:tag' but was '" + value + "'");
        }

        final var split = value.split(":");

        if (split.length != 2) {
            throw new TypeConversionException("Invalid format: must be 'owner/repository:tag' but was '" + value + "'");
        }

        return new ReleaseReference(split[0], split[1]);
    }
}
