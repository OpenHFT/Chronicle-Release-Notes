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
    public Optional<MigrateConnector> connect(String token) {
        try {
            return Optional.of(new GitHubMigrateConnector(token));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
