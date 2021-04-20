package net.openhft.chronicle.releasenotes.connector;

import static java.util.Objects.requireNonNull;

import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * @author Mislav Milicevic
 * @param <T>
 */
public interface ConnectorProvider<T extends Connector> {

    ConnectionConfiguration<T> configure();

    default Optional<T> connect(String token) {
        return configure().connect(token);
    }

    abstract class ConnectionConfiguration<T extends Connector> {

        protected Logger logger;

        public ConnectionConfiguration<T> withLogger(Logger logger) {
            this.logger = requireNonNull(logger);

            return this;
        }

        public abstract Optional<T> connect(String token);
    }
}
