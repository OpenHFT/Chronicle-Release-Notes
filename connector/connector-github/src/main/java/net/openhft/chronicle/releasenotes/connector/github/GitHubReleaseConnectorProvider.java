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
    public Optional<ReleaseConnector> connect(String token) {
        try {
            return Optional.of(new GitHubReleaseConnector(token));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
