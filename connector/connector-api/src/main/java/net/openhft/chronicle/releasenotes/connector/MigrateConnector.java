package net.openhft.chronicle.releasenotes.connector;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Mislav Milicevic
 */
public interface MigrateConnector extends Connector, AutoCloseable {

    /**
     * Migrates all issues from a list of a source milestones into a
     * singular target milestone. The results of the operation are
     * stored and returned in a {@link MigrateResult}.
     *
     * @param fromMilestones a list source milestones
     * @param toMilestone destination milestones
     */
    MigrateResult migrateMilestones(String repository, List<String> fromMilestones, String toMilestone, MigrateOptions migrateOptions);

    @Override
    default void close() throws Exception {

    }

    @Deprecated
    default MigrateResult migrateMilestones(String repository, List<String> fromMilestones, String toMilestone, List<String> ignoredLabels) {
        final MigrateOptions migrateOptions = new MigrateOptions.Builder()
            .ignoreLabels(ignoredLabels)
            .build();

        return migrateMilestones(repository, fromMilestones, toMilestone, migrateOptions);
    }

    /**
     * @author Mislav Milicevic
     */
    class MigrateResult {
        private final MigrateException error;

        private MigrateResult(MigrateException error) {
            this.error = error;
        }

        public MigrateException getError() {
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

        public static MigrateResult fail(MigrateException error) {
            requireNonNull(error);

            return new MigrateResult(error);
        }

        public static MigrateResult fail(Throwable error) {
            return fail(new MigrateException(error.getMessage()));
        }
    }

    /**
     * @author Mislav Milicevic
     */
    class MigrateOptions {
        public static final MigrateOptions DEFAULT = new MigrateOptions(
            new ArrayList<>()
        );

        private final List<String> ignoredLabels;

        private MigrateOptions(List<String> ignoredLabels) {
            this.ignoredLabels = ignoredLabels;
        }

        public List<String> getIgnoredLabels() {
            return ignoredLabels;
        }

        public static final class Builder {
            private final List<String> ignoredLabels = new ArrayList<>();

            public Builder ignoreLabels(String... labels) {
                requireNonNull(labels);

                ignoredLabels.addAll(Arrays.asList(labels));
                return this;
            }

            public Builder ignoreLabels(List<String> labels) {
                requireNonNull(labels);

                ignoredLabels.addAll(labels);
                return this;
            }

            public MigrateOptions build() {
                return new MigrateOptions(
                    ignoredLabels
                );
            }
        }
    }

}
