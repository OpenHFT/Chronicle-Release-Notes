package net.openhft.chronicle.releasenotes.connector.github.graphql.model;

public final class Tag {

    private final String name;
    private final String sha1;
    private final String commitSha1;

    public Tag(String name, String sha1, String commitSha1) {
        this.name = name;
        this.sha1 = sha1;
        this.commitSha1 = commitSha1;
    }

    public String getName() {
        return name;
    }

    public String getSHA1() {
        return sha1;
    }

    public String getCommitSHA1() {
        return commitSha1;
    }
}
