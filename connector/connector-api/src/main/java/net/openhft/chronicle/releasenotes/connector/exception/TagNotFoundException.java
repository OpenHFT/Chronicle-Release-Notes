package net.openhft.chronicle.releasenotes.connector.exception;

public class TagNotFoundException extends RuntimeException {

    private String tag;

    public TagNotFoundException(String tag) {
        super("Tag '" + tag + "' not found");

        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }
}
