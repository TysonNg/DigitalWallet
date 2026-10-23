package com.tyson.digitalwallet.ddd.application.port.payment;
import java.util.UUID;
/** Enqueue durably in the caller's database transaction, never by sending directly to a broker. */
public interface WithdrawalWorkQueue { void enqueue(UUID withdrawalId); }
