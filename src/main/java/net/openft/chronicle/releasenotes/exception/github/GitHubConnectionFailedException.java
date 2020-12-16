package net.openft.chronicle.releasenotes.exception.github;

public final class GitHubConnectionFailedException extends RuntimeException {

    public GitHubConnectionFailedException() {
        super("Failed to connect to GitHub");
    }
}
