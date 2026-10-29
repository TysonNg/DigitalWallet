package com.tyson.digitalwallet.ddd.infrastructure.media;

import static org.junit.Assert.*;

import com.tyson.digitalwallet.ddd.domain.model.entity.MediaFile;
import java.time.*;
import java.util.*;
import org.junit.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class JdbcMediaRepositoryTest {
  DriverManagerDataSource dataSource;
  JdbcMediaRepository repository;
  JdbcTemplate jdbc;
  UUID owner = UUID.randomUUID();
  Instant now = Instant.parse("2026-10-29T00:00:00Z");

  @Before
  public void setup() throws Exception {
    var ds =
        new DriverManagerDataSource(
            "jdbc:h2:mem:media_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "sa",
            "");
    dataSource = ds;
    jdbc = new JdbcTemplate(ds);
    jdbc.execute("CREATE DOMAIN TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
    jdbc.execute("CREATE TABLE users(id UUID PRIMARY KEY)");
    var sql =
        new String(
            getClass()
                .getResourceAsStream("/db/migration/V19__create_media_files.sql")
                .readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8);
    // H2 does not support PostgreSQL partial indexes; exercise the actual table schema and queries.
    for (var statement : sql.split(";"))
      if (!statement.isBlank() && !statement.contains("idx_media_cleanup")) jdbc.execute(statement);
    jdbc.update("INSERT INTO users(id) VALUES (?)", owner);
    repository = new JdbcMediaRepository(jdbc);
  }

  MediaFile file(Instant created) {
    var id = UUID.randomUUID();
    return new MediaFile(
        id,
        owner,
        "private",
        id.toString(),
        "a.png",
        "image/png",
        10,
        MediaFile.Status.PENDING,
        created,
        created);
  }

  @Test
  public void metadataCommitSurvivesCallingTransactionRollback() {
    var manager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource);
    var interceptor =
        new org.springframework.transaction.interceptor.TransactionInterceptor(
            manager,
            new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource());
    var factory = new org.springframework.aop.framework.ProxyFactory(repository);
    factory.setProxyTargetClass(true);
    factory.addAdvice(interceptor);
    var proxied =
        (com.tyson.digitalwallet.ddd.application.media.MediaRepository) factory.getProxy();
    var file = file(now);
    new org.springframework.transaction.support.TransactionTemplate(manager)
        .executeWithoutResult(
            tx -> {
              proxied.insert(file);
              tx.setRollbackOnly();
            });
    assertTrue(repository.find(file.id()).isPresent());
  }

  @Test
  public void persistsAndComparesStateBeforeTransition() {
    var f = file(now);
    repository.insert(f);
    assertEquals(f, repository.find(f.id()).orElseThrow());
    assertTrue(
        repository.transition(f.id(), MediaFile.Status.PENDING, MediaFile.Status.READY, now));
    assertFalse(
        repository.transition(f.id(), MediaFile.Status.PENDING, MediaFile.Status.DELETING, now));
    assertEquals(MediaFile.Status.READY, repository.find(f.id()).orElseThrow().status());
  }

  @Test
  public void cleanupSelectsOnlyStalePendingAndDeleting() {
    var stale = file(now.minusSeconds(7200));
    var recent = file(now);
    var ready = file(now.minusSeconds(7200));
    var deleting = file(now);
    for (var f : List.of(stale, recent, ready, deleting)) repository.insert(f);
    repository.transition(ready.id(), MediaFile.Status.PENDING, MediaFile.Status.READY, now);
    repository.transition(deleting.id(), MediaFile.Status.PENDING, MediaFile.Status.DELETING, now);
    var ids =
        repository.cleanupCandidates(now.minusSeconds(3600), 100).stream()
            .map(MediaFile::id)
            .toList();
    assertEquals(2, ids.size());
    assertTrue(ids.contains(stale.id()));
    assertTrue(ids.contains(deleting.id()));
  }
}
