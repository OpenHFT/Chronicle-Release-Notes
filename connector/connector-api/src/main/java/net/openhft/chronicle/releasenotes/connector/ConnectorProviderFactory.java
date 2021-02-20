package net.openhft.chronicle.releasenotes.connector;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Mislav Milicevic
 */
public final class ConnectorProviderFactory {

    private static final ConnectorProviderFactory INSTANCE = new ConnectorProviderFactory();

    private final Map<Class<? extends ConnectorProviderKey>, ConnectorProvider<ReleaseConnector>> releaseConnectorProviders = new HashMap<>();
    private final Map<Class<? extends ConnectorProviderKey>, ConnectorProvider<MigrateConnector>> migrateConnectorProviders = new HashMap<>();

    private ConnectorProviderFactory() {
    }

    public Optional<ConnectorProvider<ReleaseConnector>> getReleaseConnectorProvider(Class<? extends ConnectorProviderKey> connectorProviderKey) {
        requireNonNull(connectorProviderKey);

        return Optional.ofNullable(releaseConnectorProviders.get(connectorProviderKey));
    }

    public Optional<ConnectorProvider<MigrateConnector>> getMigrateConnectorProvider(Class<? extends ConnectorProviderKey> connectorProviderKey) {
        requireNonNull(connectorProviderKey);

        return Optional.ofNullable(migrateConnectorProviders.get(connectorProviderKey));
    }

    public void registerReleaseConnectorProvider(Class<? extends ConnectorProviderKey> connectorProviderKey, ConnectorProvider<ReleaseConnector> releaseConnector) {
        registerReleaseConnectorProvider(connectorProviderKey, releaseConnector, false);
    }

    public void registerReleaseConnectorProvider(Class<? extends ConnectorProviderKey> connectorProviderKey, ConnectorProvider<ReleaseConnector> releaseConnector, boolean override) {
        requireNonNull(connectorProviderKey);
        requireNonNull(releaseConnector);

        if (override) {
            releaseConnectorProviders.put(connectorProviderKey, releaseConnector);
        } else {
            releaseConnectorProviders.putIfAbsent(connectorProviderKey, releaseConnector);
        }
    }

    public void registerMigrateConnectorProvider(Class<? extends ConnectorProviderKey> connectorProviderKey, ConnectorProvider<MigrateConnector> migrateConnector) {
        registerMigrateConnectorProvider(connectorProviderKey, migrateConnector, false);
    }

    public void registerMigrateConnectorProvider(Class<? extends ConnectorProviderKey> connectorProviderKey, ConnectorProvider<MigrateConnector> migrateConnector, boolean override) {
        requireNonNull(connectorProviderKey);
        requireNonNull(migrateConnector);

        if (override) {
            migrateConnectorProviders.put(connectorProviderKey, migrateConnector);
        } else {
            migrateConnectorProviders.putIfAbsent(connectorProviderKey, migrateConnector);
        }
    }

    public static ConnectorProviderFactory getInstance() {
        return INSTANCE;
    }
}
