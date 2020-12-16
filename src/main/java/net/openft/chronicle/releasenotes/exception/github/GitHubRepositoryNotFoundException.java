package net.openft.chronicle.releasenotes.exception.github;

public final class GitHubRepositoryNotFoundException extends RuntimeException {

    public GitHubRepositoryNotFoundException(String repository) {
        super("Repository '" + repository + "' not found");
    }
}
