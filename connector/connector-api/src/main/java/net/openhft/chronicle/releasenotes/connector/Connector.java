package net.openhft.chronicle.releasenotes.connector;

/**
 * @author Mislav Milicevic
 */
public interface Connector {

    Class<? extends ConnectorProviderKey> getKey();
}
