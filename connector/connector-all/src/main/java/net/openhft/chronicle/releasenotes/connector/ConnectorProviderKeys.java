package net.openhft.chronicle.releasenotes.connector;

import net.openhft.chronicle.releasenotes.connector.github.GitHubConnectorProviderKey;
import net.openhft.chronicle.releasenotes.connector.github.GitHubMigrateConnectorProvider;
import net.openhft.chronicle.releasenotes.connector.github.GitHubReleaseConnectorProvider;

/**
 * Contains all available {@link ConnectorProviderKey}s
 *
 * @author Mislav Milicevic
 */
public final class ConnectorProviderKeys {

    /**
     * A {@link ConnectorProviderKey} used to access GitHub-specific {@link ConnectorProvider}s
     */
    public static final Class<? extends ConnectorProviderKey> GITHUB = GitHubConnectorProviderKey.class;

    static {
        ConnectorProviderFactory.getInstance().registerReleaseConnectorProvider(GITHUB, new GitHubReleaseConnectorProvider());
        ConnectorProviderFactory.getInstance().registerMigrateConnectorProvider(GITHUB, new GitHubMigrateConnectorProvider());
    }

    private ConnectorProviderKeys() {
    }
}
