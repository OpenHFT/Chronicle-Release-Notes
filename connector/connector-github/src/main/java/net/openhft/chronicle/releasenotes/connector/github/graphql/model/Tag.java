package net.openhft.chronicle.releasenotes.connector.github.graphql.model;

public final class Tag {

    private final String name;
    private final String commitSha1;

    public Tag(String name, String commitSha1) {
        this.name = name;
        this.commitSha1 = commitSha1;
    }

    public String getName() {
        return name;
    }

    public String getCommitSHA1() {
        return commitSha1;
    }
}
