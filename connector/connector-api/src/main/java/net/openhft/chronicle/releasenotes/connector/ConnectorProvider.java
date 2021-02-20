package net.openhft.chronicle.releasenotes.connector;

import java.util.Optional;

/**
 * @author Mislav Milicevic
 * @param <T>
 */
public interface ConnectorProvider<T extends Connector> {

    Optional<T> connect(String token);
}
