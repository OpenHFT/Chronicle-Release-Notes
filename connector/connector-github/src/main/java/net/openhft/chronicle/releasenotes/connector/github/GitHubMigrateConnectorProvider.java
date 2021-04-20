package net.openhft.chronicle.releasenotes.connector.github;

import net.openhft.chronicle.releasenotes.connector.ConnectorProvider;
import net.openhft.chronicle.releasenotes.connector.MigrateConnector;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Mislav Milicevic
 */
public class GitHubMigrateConnectorProvider implements ConnectorProvider<MigrateConnector> {

    @Override
    public ConnectionConfiguration<MigrateConnector> configure() {
        return new MigrateConnectionConfiguration();
    }

    private static final class MigrateConnectionConfiguration extends ConnectionConfiguration<MigrateConnector> {

        @Override
        public Optional<MigrateConnector> connect(String token) {
            try {
                return Optional.of(logger != null
                    ? new GitHubMigrateConnector(token, logger)
                    : new GitHubMigrateConnector(token)
                );
            } catch (IOException e) {
                return Optional.empty();
            }
        }
    }
}
