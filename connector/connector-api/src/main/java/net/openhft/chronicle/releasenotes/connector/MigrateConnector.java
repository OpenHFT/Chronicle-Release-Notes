package net.openhft.chronicle.releasenotes.connector;

import static java.util.Objects.requireNonNull;

import java.util.Set;

/**
 * @author Mislav Milicevic
 */
public interface MigrateConnector extends Connector {

    /**
     * Migrates all issues from a list of a source milestones into a
     * singular target milestone. The results of the operation are
     * stored and returned in a {@link MigrateResult}.
     * <p>
     * If {@code ignoredLabels} is not {@code null} or is not empty, then
     * all of the issues which contain one of the provided labels are
     * ignored in the migration process.
     *
     * @param fromMilestones a list source milestones
     * @param toMilestone destination milestones
     * @param ignoredLabels a list of ignored labels
     */
    MigrateResult migrateMilestones(String repository, Set<String> fromMilestones, String toMilestone, Set<String> ignoredLabels);

    /**
     * @author Mislav Milicevic
     */
    class MigrateResult {
        private final RuntimeException error;

        private MigrateResult(RuntimeException error) {
            this.error = error;
        }

        public RuntimeException getError() {
            return error;
        }

        public void throwIfFail() {
            if (isFail()) {
                throw error;
            }
        }

        public boolean isSuccess() {
            return error == null;
        }

        public boolean isFail() {
            return !isSuccess();
        }

        public static MigrateResult success() {
            return new MigrateResult(null);
        }

        public static MigrateResult fail(RuntimeException error) {
            requireNonNull(error);

            return new MigrateResult(error);
        }
    }

}
