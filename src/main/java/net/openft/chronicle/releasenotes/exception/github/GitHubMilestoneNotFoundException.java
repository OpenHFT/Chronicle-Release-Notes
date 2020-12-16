package net.openft.chronicle.releasenotes.exception.github;

public final class GitHubMilestoneNotFoundException extends RuntimeException {

    public GitHubMilestoneNotFoundException(String milestone) {
        super("Milestone '" + milestone + "' not found");
    }
}
