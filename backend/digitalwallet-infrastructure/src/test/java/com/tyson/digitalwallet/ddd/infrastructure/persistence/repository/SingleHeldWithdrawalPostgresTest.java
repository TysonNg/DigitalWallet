package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import static org.junit.Assert.*;

import org.junit.*;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Run with -Dwithdrawal.test.jdbc=jdbc:postgresql://127.0.0.1:55439/postgres?user=withdrawal_test
 * against an isolated database.
 */
public class SingleHeldWithdrawalPostgresTest {
    private String url, schema;
    private final UUID wallet = UUID.randomUUID();
    private String migration;

    @Before
    public void setup() throws Exception {
        url = System.getProperty("withdrawal.test.jdbc");
        Assume.assumeTrue("An isolated PostgreSQL JDBC URL is required", url != null);
        schema = "hold_test_" + UUID.randomUUID().toString().replace("-", "");
        migration =
                Files.readString(
                        Path.of(
                                "src/main/resources/db/migration/V16__one_held_withdrawal_per_wallet.sql"));
        try (var c = DriverManager.getConnection(url);
                var s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
            s.execute("SET search_path TO " + schema);
            s.execute("CREATE TABLE wallets(id UUID PRIMARY KEY,balance NUMERIC NOT NULL)");
            s.execute(
                    "CREATE TABLE withdrawal_requests(id UUID PRIMARY KEY,wallet_id UUID NOT"
                        + " NULL,status VARCHAR NOT NULL,idempotency_key VARCHAR NOT NULL)");
            s.execute("INSERT INTO wallets VALUES ('" + wallet + "',1000000)");
        }
    }

    private Connection connection() throws SQLException {
        var c = DriverManager.getConnection(url);
        c.createStatement().execute("SET search_path TO " + schema);
        return c;
    }

    @After
    public void cleanup() throws Exception {
        if (schema != null)
            try (var c = DriverManager.getConnection(url);
                    var s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
            }
    }

    private void migrate() throws Exception {
        try (var c = connection();
                var s = c.createStatement()) {
            s.execute(migration);
        }
    }

    private void insert(String state) throws Exception {
        try (var c = connection();
                var s = c.prepareStatement("INSERT INTO withdrawal_requests VALUES (?,?,?,?)")) {
            s.setObject(1, UUID.randomUUID());
            s.setObject(2, wallet);
            s.setString(3, state);
            s.setString(4, UUID.randomUUID().toString());
            s.executeUpdate();
        }
    }

    @Test
    public void eachHeldStateBlocksAnotherHoldAndTerminalStatesReleaseTheSlot() throws Exception {
        migrate();
        for (String held : List.of("PENDING", "PROCESSING", "REVIEW_REQUIRED")) {
            insert(held);
            SQLException duplicate = assertThrows(SQLException.class, () -> insert("PENDING"));
            assertEquals("23505", duplicate.getSQLState());
            try (var c = connection();
                    var s = c.createStatement()) {
                s.execute("DELETE FROM withdrawal_requests");
            }
        }
        for (String terminal : List.of("SUCCEEDED", "FAILED", "RETURNED")) insert(terminal);
        insert("PENDING");
    }

    @Test
    public void existingMultipleHoldsStopMigrationWithoutChangingData() throws Exception {
        insert("PENDING");
        insert("REVIEW_REQUIRED");
        SQLException error = assertThrows(SQLException.class, this::migrate);
        assertTrue(error.getMessage().contains("reconcile wallets"));
        try (var c = connection();
                var s = c.createStatement();
                var r = s.executeQuery("SELECT count(*) FROM withdrawal_requests")) {
            r.next();
            assertEquals(2, r.getInt(1));
        }
    }

    @Test
    public void concurrentDifferentKeysReserveOnlyOnceUnderWalletLock() throws Exception {
        migrate();
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        Callable<Boolean> request =
                () -> {
                    start.await();
                    try (var c = connection()) {
                        c.setAutoCommit(false);
                        try (var s = c.createStatement()) {
                            s.execute("SET LOCAL lock_timeout='5s'");
                            s.executeQuery(
                                            "SELECT id FROM wallets WHERE id='"
                                                    + wallet
                                                    + "' FOR UPDATE")
                                    .close();
                            try (var r =
                                    s.executeQuery(
                                            "SELECT count(*) FROM withdrawal_requests WHERE"
                                                + " wallet_id='"
                                                    + wallet
                                                    + "' AND status IN"
                                                    + " ('PENDING','PROCESSING','REVIEW_REQUIRED')")) {
                                r.next();
                                if (r.getInt(1) > 0) {
                                    c.rollback();
                                    return false;
                                }
                            }
                            s.executeUpdate(
                                    "UPDATE wallets SET balance=balance-200000 WHERE id='"
                                            + wallet
                                            + "'");
                            s.executeUpdate(
                                    "INSERT INTO withdrawal_requests VALUES ('"
                                            + UUID.randomUUID()
                                            + "','"
                                            + wallet
                                            + "','PENDING','"
                                            + UUID.randomUUID()
                                            + "')");
                            c.commit();
                            return true;
                        } catch (Exception ex) {
                            c.rollback();
                            throw ex;
                        }
                    }
                };
        try {
            var a = pool.submit(request);
            var b = pool.submit(request);
            start.countDown();
            assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            try (var c = connection();
                    var s = c.createStatement();
                    var r =
                            s.executeQuery(
                                    "SELECT balance,(SELECT count(*) FROM withdrawal_requests) FROM"
                                        + " wallets")) {
                r.next();
                assertEquals(800000, r.getInt(1));
                assertEquals(1, r.getInt(2));
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
