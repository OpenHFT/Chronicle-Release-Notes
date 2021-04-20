package net.openhft.chronicle.releasenotes.connector.github;

import net.openhft.chronicle.releasenotes.connector.ConnectorProvider;
import net.openhft.chronicle.releasenotes.connector.ReleaseConnector;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Mislav Milicevic
 */
public class GitHubReleaseConnectorProvider implements ConnectorProvider<ReleaseConnector> {

    @Override
    public ConnectionConfiguration<ReleaseConnector> configure() {
        return new ReleaseConnectionConfiguration();
    }

    private static final class ReleaseConnectionConfiguration extends ConnectionConfiguration<ReleaseConnector> {

        @Override
        public Optional<ReleaseConnector> connect(String token) {
            try {
                return Optional.of(logger != null
                    ? new GitHubReleaseConnector(token, logger)
                    : new GitHubReleaseConnector(token)
                );
            } catch (IOException e) {
                return Optional.empty();
            }
        }
    }
}
