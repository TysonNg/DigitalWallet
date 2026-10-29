package com.tyson.digitalwallet.ddd.infrastructure.media;

import com.tyson.digitalwallet.ddd.domain.model.entity.MediaFile;
import com.tyson.digitalwallet.ddd.domain.repository.MediaRepository;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRES_NEW)
public class JdbcMediaRepository implements 
    com.tyson.digitalwallet.ddd.domain.repository.MediaRepository,
    com.tyson.digitalwallet.ddd.application.media.MediaRepository {
  private final JdbcTemplate jdbc;

  public JdbcMediaRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void insert(MediaFile f) {
    jdbc.update(
        "INSERT INTO media_files"
            + " (id,owner_id,bucket,object_key,original_name,content_type,size_bytes,status,created_at,updated_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?)",
        f.id(),
        f.ownerId(),
        f.bucket(),
        f.objectKey(),
        f.originalName(),
        f.contentType(),
        f.sizeBytes(),
        f.status().name(),
        Timestamp.from(f.createdAt()),
        Timestamp.from(f.updatedAt()));
  }

  @Override
  public Optional<MediaFile> find(UUID id) {
    return jdbc.query("SELECT * FROM media_files WHERE id=?", this::map, id).stream().findFirst();
  }

  @Override
  public boolean transition(
      UUID id, MediaFile.Status expected, MediaFile.Status next, Instant now) {
    return jdbc.update(
            "UPDATE media_files SET status=?,updated_at=? WHERE id=? AND status=?",
            next.name(),
            Timestamp.from(now),
            id,
            expected.name())
        == 1;
  }

  @Override
  public List<MediaFile> cleanupCandidates(Instant staleBefore, int limit) {
    return jdbc.query(
        "SELECT * FROM media_files WHERE status='DELETING' OR (status='PENDING' AND created_at < ?)"
            + " ORDER BY updated_at LIMIT ?",
        this::map,
        Timestamp.from(staleBefore),
        limit);
  }

  private MediaFile map(ResultSet r, int row) throws SQLException {
    return new MediaFile(
        r.getObject("id", UUID.class),
        r.getObject("owner_id", UUID.class),
        r.getString("bucket"),
        r.getString("object_key"),
        r.getString("original_name"),
        r.getString("content_type"),
        r.getLong("size_bytes"),
        MediaFile.Status.valueOf(r.getString("status")),
        r.getTimestamp("created_at").toInstant(),
        r.getTimestamp("updated_at").toInstant());
  }
}
