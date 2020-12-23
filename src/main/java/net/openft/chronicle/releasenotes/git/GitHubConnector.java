package net.openft.chronicle.releasenotes.git;

import static java.util.Objects.requireNonNull;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHRelease;
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
            throw new RuntimeException("Repository '" + repository + "' not found");
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
                .orElseThrow(() -> new RuntimeException("Milestone '" + milestone + "' not found"));
        } catch (IOException e) {
            throw new RuntimeException("Milestone '" + milestone + "' not found");
        }
    }

    public List<GHIssue> getMilestoneIssues(GHMilestone milestone) {
        requireNonNull(milestone);

        return getMilestoneIssues(milestone, GHIssueState.ALL);
    }

    public List<GHIssue> getClosedMilestoneIssues(GHMilestone milestone) {
        requireNonNull(milestone);

        return getMilestoneIssues(milestone, GHIssueState.CLOSED);
    }

    public List<GHIssue> getMilestoneIssues(GHMilestone milestone, GHIssueState issueState) {
        requireNonNull(milestone);
        requireNonNull(issueState);

        try {
            return milestone.getOwner().listIssues(issueState).toList()
                .stream()
                .filter(ghIssue -> ghIssue.getMilestone() != null && ghIssue.getMilestone().getNumber() == milestone.getNumber())
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch issues");
        }
    }

    public GHRelease createRelease(String tag, GHMilestone milestone, List<String> ignoredLabels) {
        requireNonNull(tag);
        requireNonNull(milestone);

        try {
            if (milestone.getOwner().listTags().toList().stream().noneMatch(ghTag -> ghTag.getName().equals(tag))) {
                throw new RuntimeException("Tag '" + tag + "' not found");
            }

            return milestone.getOwner().createRelease(tag)
                .name(tag)
                .body(createReleaseBody(milestone, ignoredLabels))
                .create();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create release for tag '" + tag + "'");
        }
    }

    private String createReleaseBody(GHMilestone milestone, List<String> ignoredLabels) {
        requireNonNull(milestone);

        final var body = new StringBuilder();

        var stream = getClosedMilestoneIssues(milestone).stream();

        if (ignoredLabels != null) {
            stream = stream.filter(issue -> issue.getLabels().stream().anyMatch(ghLabel -> ignoredLabels.contains(ghLabel.getName())));
        }

        stream.forEach(issue -> body
            .append("- [**")
            .append(issue.getLabels().isEmpty() ? "closed" : issue.getLabels().stream().findFirst().get().getName())
            .append("**] ")
            .append(issue.getTitle())
            .append('\n')
        );

        return body.toString();
    }

    public void migrateIssues(List<GHMilestone> fromMilestones, GHMilestone toMilestone, List<String> ignoredLabels) {
        requireNonNull(fromMilestones);
        requireNonNull(toMilestone);

        var stream = fromMilestones.stream().flatMap(milestone -> getMilestoneIssues(milestone).stream());

        if (ignoredLabels != null) {
            stream = stream.filter(issue -> issue.getLabels().stream().anyMatch(ghLabel -> ignoredLabels.contains(ghLabel.getName())));
        }

        stream.forEach(ghIssue -> {
            try {
                ghIssue.setMilestone(toMilestone);
            } catch (IOException e) {
                throw new RuntimeException("Failed to assign issue #" + ghIssue.getNumber() + " to milestone '" + toMilestone.getTitle() + "'");
            }
        });
    }

    public static GitHubConnector connectWithAccessToken(String token) {
        try {
            return new GitHubConnector(token);
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to GitHub");
        }
    }

}
