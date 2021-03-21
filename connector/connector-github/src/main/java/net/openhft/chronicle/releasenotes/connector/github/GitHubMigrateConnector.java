package net.openhft.chronicle.releasenotes.connector.github;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import net.openhft.chronicle.releasenotes.connector.ConnectorProviderKey;
import net.openhft.chronicle.releasenotes.connector.MigrateConnector;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Mislav Milicevic
 */
public final class GitHubMigrateConnector implements MigrateConnector {

    private final GitHub github;

    public GitHubMigrateConnector(String token) throws IOException {
        this.github = new GitHubBuilder()
            .withOAuthToken(requireNonNull(token))
            .build();
    }

    @Override
    public MigrateResult migrateMilestones(String repository, List<String> fromMilestones, String toMilestone, MigrateOptions migrateOptions) {
        requireNonNull(repository);
        requireNonNull(fromMilestones);
        requireNonNull(toMilestone);

        final GHRepository repositoryRef = getRepository(repository);
        final Map<String, GHMilestone> milestonesRef = getMilestones(repositoryRef, fromMilestones);
        final GHMilestone toMilestoneRef = getMilestone(repositoryRef, toMilestone);

        final List<GHIssue> issues = getMilestoneIssues(repositoryRef, (List<GHMilestone>) milestonesRef.values(), migrateOptions.getIgnoredLabels());

        final AtomicReference<MigrateResult> migrateResult = new AtomicReference<>(MigrateResult.success());

        issues.forEach(ghIssue -> {
            try {
                ghIssue.setMilestone(toMilestoneRef);
            } catch (IOException e) {
                migrateResult.set(MigrateResult.fail(
                    new RuntimeException("Failed to assign issue #" + ghIssue.getNumber() + " to milestone '" + toMilestone + "'"))
                );
            }
        });

        return migrateResult.get();
    }

    @Override
    public Class<? extends ConnectorProviderKey> getKey() {
        return GitHubConnectorProviderKey.class;
    }

    private GHRepository getRepository(String repository) {
        requireNonNull(repository);

        try {
            return github.getRepository(repository);
        } catch (IOException e) {
            throw new RuntimeException("Repository '" + repository + "' not found");
        }
    }

    private Map<String, GHMilestone> getMilestones(GHRepository repository, List<String> milestones) {
        requireNonNull(repository);
        requireNonNull(milestones);

        final Map<String, GHMilestone> collectedMilestones = stream(repository.listMilestones(GHIssueState.ALL))
            .filter(milestone -> milestones.contains(milestone.getTitle()))
            .collect(Collectors.toMap(GHMilestone::getTitle, Function.identity()));

        if (collectedMilestones.size() == milestones.size()) {
            return collectedMilestones;
        }

        final String missingMilestones = milestones.stream()
            .filter(tag -> !collectedMilestones.containsKey(tag))
            .collect(Collectors.joining(", "));

        throw new RuntimeException("Failed to find milestones(s) [" + missingMilestones + "] in repository '" + repository.getFullName() + "'");
    }

    private GHMilestone getMilestone(GHRepository repository, String milestone) {
        requireNonNull(repository);
        requireNonNull(milestone);

        return stream(repository.listMilestones(GHIssueState.ALL))
            .filter(ghMilestone -> ghMilestone.getTitle().equals(milestone))
            .findAny()
            .orElseThrow(() -> new RuntimeException("Milestone '" + milestone + "' not found"));
    }

    private List<GHIssue> getMilestoneIssues(GHRepository repository, List<GHMilestone> milestones, List<String> ignoredLabels) {
        requireNonNull(repository);
        requireNonNull(milestones);

        final List<Integer> milestoneNumbers = milestones.stream().map(GHMilestone::getNumber).collect(toList());

        Stream<GHIssue> stream = stream(repository.listIssues(GHIssueState.ALL))
            .filter(issue -> issue.getMilestone() != null && milestoneNumbers.contains(issue.getMilestone().getNumber()));

        if (ignoredLabels != null) {
            stream = stream.filter(issue -> issue.getLabels().stream().noneMatch(label -> ignoredLabels.contains(label.getName())));
        }

        return stream.collect(toList());
    }

    private <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
