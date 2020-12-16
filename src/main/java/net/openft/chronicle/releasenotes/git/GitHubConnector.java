package net.openft.chronicle.releasenotes.git;

import static java.util.Objects.requireNonNull;

import net.openft.chronicle.releasenotes.exception.github.GitHubConnectionFailedException;
import net.openft.chronicle.releasenotes.exception.github.GitHubMilestoneNotFoundException;
import net.openft.chronicle.releasenotes.exception.github.GitHubRepositoryNotFoundException;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public final class GitHubConnector {

    private final GitHub gitHub;

    private GitHubConnector(String token) throws IOException {
        this.gitHub = new GitHubBuilder().withOAuthToken(requireNonNull(token)).build();
    }

    public GHRepository getRepository(String repository) {
        requireNonNull(repository);

        try {
            return gitHub.getRepository(repository);
        } catch (IOException e) {
            throw new GitHubRepositoryNotFoundException(repository);
        }
    }

    public GHMilestone getMilestone(String repository, String milestone) {
        requireNonNull(repository);
        requireNonNull(milestone);

        try {
            return getRepository(repository).listMilestones(GHIssueState.ALL).toList()
                .stream()
                .filter(ghMilestone -> ghMilestone.getTitle().equals(milestone))
                .findAny()
                .orElseThrow(() -> new GitHubMilestoneNotFoundException(milestone));
        } catch (IOException e) {
            throw new GitHubMilestoneNotFoundException(milestone);
        }
    }

    public List<GHIssue> getMilestoneIssues(GHMilestone milestone) {
        requireNonNull(milestone);

        try {
            return milestone.getOwner().listIssues(GHIssueState.ALL).toList()
                .stream()
                .filter(ghIssue -> ghIssue.getMilestone() != null && ghIssue.getMilestone().getNumber() == milestone.getNumber())
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch issues");
        }
    }

    public static GitHubConnector connectWithAccessToken(String token) {
        try {
            return new GitHubConnector(token);
        } catch (IOException e) {
            throw new GitHubConnectionFailedException();
        }
    }

}
