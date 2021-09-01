package net.openhft.chronicle.releasenotes.creator.internal.util;

import static java.util.Objects.requireNonNull;

public final class MarkdownUtil {

    private MarkdownUtil() {
    }

    public static String header(String text, int size) {
        requireNonNull(text);

        if (size < 1) {
            throw new RuntimeException(String.format("Invalid header size %d (minimum size = 1)", size));
        }

        if (size > 6) {
            throw new RuntimeException(String.format("Invalid header size %d (maximum size = 6)", size));
        }

        final StringBuilder headerBuilder = new StringBuilder();

        for (int i = 0; i < size; i++) {
            headerBuilder.append("#");
        }

        headerBuilder.append(" ").append(text);

        return headerBuilder.toString();
    }

    public static String bold(String text) {
        requireNonNull(text);

        return "**" + text + "**";
    }

    public static String italic(String text) {
        requireNonNull(text);

        return "*" + text + "*";
    }

    public static String entry(String entry) {
        requireNonNull(entry);

        return "- " + entry;
    }

    public static String link(String placeholder, String link) {
        requireNonNull(placeholder);
        requireNonNull(link);

        return String.format("[%s](%s)", placeholder, link);
    }
}
