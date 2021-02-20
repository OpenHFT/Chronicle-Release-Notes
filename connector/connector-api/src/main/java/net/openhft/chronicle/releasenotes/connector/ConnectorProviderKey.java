package net.openhft.chronicle.releasenotes.connector;

/**
 * Used as an identifier for {@link ConnectorProvider} implementations.
 * <p>
 * When registering a {@link ConnectorProvider} implementation via the
 * {@link ConnectorProviderFactory}, a unique {@link ConnectorProviderKey}
 * implementation should be used as a key.
 *
 * @author Mislav Milicevic
 */
public interface ConnectorProviderKey {
}
