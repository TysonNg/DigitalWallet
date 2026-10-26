package com.tyson.digitalwallet.ddd.application.port.outbox;

/** Append an integration event within the business operation's existing database transaction. */
public interface OutboxEventWriter {
    void append(OutboxEvent event);
}
