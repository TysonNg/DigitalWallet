package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.application.port.payment.PayoutNotificationInbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class JdbcPayoutNotificationInbox implements PayoutNotificationInbox {
    private final JdbcTemplate jdbc;

    public JdbcPayoutNotificationInbox(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void enqueue(String provider, String eventId, UUID withdrawalId) {
        jdbc.update(
                "INSERT INTO payout_notification_inbox(provider,event_id,withdrawal_id) VALUES"
                        + " (?,?,?) ON CONFLICT(provider,event_id) DO NOTHING",
                provider,
                eventId,
                withdrawalId);
    }

    @Override
    public List<Notification> pending() {
        return jdbc.query(
                "SELECT provider,event_id,withdrawal_id FROM payout_notification_inbox WHERE"
                        + " processed_at IS NULL ORDER BY received_at LIMIT 100",
                (rs, i) ->
                        new Notification(
                                rs.getString(1), rs.getString(2), rs.getObject(3, UUID.class)));
    }

    @Override
    public void complete(String provider, String eventId) {
        jdbc.update(
                "UPDATE payout_notification_inbox SET processed_at=CURRENT_TIMESTAMP WHERE"
                        + " provider=? AND event_id=?",
                provider,
                eventId);
    }
}
