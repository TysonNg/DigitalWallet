package com.tyson.digitalwallet.ddd.infrastructure.receiveqr;

import com.tyson.digitalwallet.ddd.domain.model.entity.ReceiveCode;
import com.tyson.digitalwallet.ddd.domain.repository.ReceiveCodeRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRES_NEW)
public class JdbcReceiveCodeRepository implements
    com.tyson.digitalwallet.ddd.domain.repository.ReceiveCodeRepository,
    com.tyson.digitalwallet.ddd.application.receiveqr.ReceiveCodeRepository {
  private final JdbcTemplate jdbc;

  public JdbcReceiveCodeRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReceiveCode reserve(UUID walletId, String candidateToken) {
    jdbc.update(
        "INSERT INTO wallet_receive_codes(wallet_id,token) VALUES (?,?) ON CONFLICT DO NOTHING",
        walletId,
        candidateToken);
    return findByWallet(walletId)
        .orElseThrow(
            () -> new DataIntegrityViolationException("Receive code reservation conflict"));
  }

  @Override
  public Optional<ReceiveCode> findByWallet(UUID walletId) {
    return find("wallet_id", walletId);
  }

  @Override
  public Optional<ReceiveCode> findByToken(String token) {
    return find("token", token);
  }

  private Optional<ReceiveCode> find(String column, Object value) {
    return jdbc.query(
            "SELECT wallet_id,token,media_id FROM wallet_receive_codes WHERE " + column + "=?",
            (r, n) ->
                new ReceiveCode(
                    r.getObject("wallet_id", UUID.class),
                    r.getString("token"),
                    r.getObject("media_id", UUID.class)),
            value)
        .stream()
        .findFirst();
  }

  @Override
  public boolean attach(UUID walletId, UUID expectedMediaId, UUID candidateMediaId) {
    return jdbc.update(
            "UPDATE wallet_receive_codes SET media_id=? WHERE wallet_id=? AND media_id IS NOT DISTINCT FROM ?",
            candidateMediaId,
            walletId,
            expectedMediaId)
        == 1;
  }
}