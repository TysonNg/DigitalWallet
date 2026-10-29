package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.ReceiveCode;
import java.util.Optional;
import java.util.UUID;

public interface ReceiveCodeRepository {
  /** Commits a reservation independently, returning the existing token when another request wins. */
  ReceiveCode reserve(UUID walletId, String candidateToken);

  Optional<ReceiveCode> findByWallet(UUID walletId);

  Optional<ReceiveCode> findByToken(String token);

  /** A false result definitively means this candidate was not attached. Exceptions are ambiguous. */
  boolean attach(UUID walletId, UUID expectedMediaId, UUID candidateMediaId);
}
