package com.tyson.digitalwallet.ddd.application.media;

import com.tyson.digitalwallet.ddd.application.port.out.storage.MediaStoragePort;
import com.tyson.digitalwallet.ddd.application.usecase.media.*;
import com.tyson.digitalwallet.ddd.domain.model.entity.MediaFile;
import com.tyson.digitalwallet.ddd.domain.model.entity.MediaFile.Status;
import com.tyson.digitalwallet.ddd.domain.repository.MediaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.InputStream;
import java.time.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Each repository operation commits independently; never hold a DB transaction across storage I/O.
 */
public final class MediaService implements
    UploadMediaUseCase,
    GetMediaMetadataUseCase,
    GetMediaContentUseCase,
    DeleteMediaUseCase,
    StoreGeneratedImageUseCase,
    CleanupMediaUseCase {

  private static final Logger log = LoggerFactory.getLogger(MediaService.class);
  private final MediaRepository repository;
  private final MediaStoragePort storage;
  private final String bucket;
  private final Clock clock;
  private final MeterRegistry metrics;
  private final ImageValidator validator = new ImageValidator();

  public MediaService(
      MediaRepository repository,
      MediaStoragePort storage,
      String bucket,
      Clock clock,
      MeterRegistry metrics) {
    this.repository = repository;
    this.storage = storage;
    this.bucket = bucket;
    this.clock = clock;
    this.metrics = metrics;
  }

  @Override
  public MediaFile upload(UUID owner, String originalName, byte[] bytes) {
    Objects.requireNonNull(owner);
    long start = System.nanoTime();
    String type = validator.validate(bytes);
    UUID id = UUID.randomUUID();
    Instant now = clock.instant();
    String key = "media/" + owner + "/" + id + (type.equals("image/png") ? ".png" : ".jpg");
    String name =
        originalName == null ? "image" : originalName.replaceAll("[\\p{Cntrl}\\\\/]", "_");
    if (name.length() > 255) name = name.substring(0, 255);
    MediaFile file =
        new MediaFile(id, owner, bucket, key, name, type, bytes.length, Status.PENDING, now, now);
    repository.insert(file);
    try {
      storage.put(bucket, key, type, bytes);
      if (!repository.transition(id, Status.PENDING, Status.READY, clock.instant()))
        throw MediaException.unavailable();
      log.info(
          "mediaId={} operation=upload result=success elapsedMs={}",
          id,
          (System.nanoTime() - start) / 1_000_000);
      return new MediaFile(
          id, owner, bucket, key, name, type, bytes.length, Status.READY, now, clock.instant());
    } catch (RuntimeException ex) {
      metrics.counter("media.upload.errors").increment();
      // A failed commit acknowledgement does not prove that READY failed to commit.
      // Never delete bytes until a durable DELETING state is confirmed.
      try {
        var persisted = repository.find(id).orElseThrow(MediaException::missing);
        if (persisted.status() == Status.READY) {
          log.info(
              "mediaId={} operation=upload result=recovered elapsedMs={}",
              id,
              (System.nanoTime() - start) / 1_000_000);
          return persisted;
        }
        if (persisted.status() == Status.DELETING
            || (persisted.status() == Status.PENDING
                && repository.transition(id, Status.PENDING, Status.DELETING, clock.instant()))) {
          if (!remove(persisted)) metrics.counter("media.cleanup.errors").increment();
        }
      } catch (RuntimeException ignored) {
        // Preserve the object while the outcome is unknown. A PENDING row is cleaned later;
        // a committed READY row remains valid and must never point to deleted bytes.
        metrics.counter("media.cleanup.errors").increment();
      }
      log.warn(
          "mediaId={} operation=upload result=failed elapsedMs={}",
          id,
          (System.nanoTime() - start) / 1_000_000);
      throw MediaException.unavailable();
    }
  }

  /** Trusted internal entry point, not exposed as an HTTP upload mode. */
  @Override
  public MediaFile storeGeneratedImage(UUID owner, String name, byte[] bytes) {
    return upload(owner, name, bytes);
  }

  @Override
  public MediaFile metadata(UUID owner, UUID id) {
    var file = owned(owner, id);
    if (file.status() != Status.READY) throw MediaException.missing();
    return file;
  }

  @Override
  public InputStream open(UUID owner, UUID id) {
    var file = metadata(owner, id);
    return storage.open(file.bucket(), file.objectKey());
  }

  private MediaFile owned(UUID owner, UUID id) {
    return repository
        .find(id)
        .filter(f -> f.ownerId().equals(owner))
        .orElseThrow(MediaException::missing);
  }

  @Override
  public void delete(UUID owner, UUID id) {
    var file = owned(owner, id);
    if (file.status() == Status.DELETED) return;
    if (file.status() == Status.PENDING) throw MediaException.missing();
    if (file.status() == Status.READY
        && !repository.transition(id, Status.READY, Status.DELETING, clock.instant())) {
      // A concurrent request may have advanced the state; recheck ownership/state.
      delete(owner, id);
      return;
    }
    remove(file);
  }

  private boolean remove(MediaFile file) {
    long start = System.nanoTime();
    try {
      storage.delete(file.bucket(), file.objectKey());
      repository.transition(file.id(), Status.DELETING, Status.DELETED, clock.instant());
      log.info(
          "mediaId={} operation=delete result=success elapsedMs={}",
          file.id(),
          (System.nanoTime() - start) / 1_000_000);
      return true;
    } catch (RuntimeException ex) {
      metrics.counter("media.delete.errors").increment();
      // Rotate failed candidates behind older work so a batch of permanent failures cannot starve
      // others.
      try {
        repository.transition(file.id(), Status.DELETING, Status.DELETING, clock.instant());
      } catch (RuntimeException ignored) {
        /* A later sweep retries after the database recovers. */
      }
      log.warn(
          "mediaId={} operation=delete result=retry elapsedMs={}",
          file.id(),
          (System.nanoTime() - start) / 1_000_000);
      return false;
    }
  }

  @Override
  public void cleanup() {
    for (var file : repository.cleanupCandidates(clock.instant().minus(Duration.ofHours(1)), 100)) {
      try {
        if (file.status() == Status.PENDING
            && !repository.transition(file.id(), Status.PENDING, Status.DELETING, clock.instant()))
          continue;
        if (!remove(file)) metrics.counter("media.cleanup.errors").increment();
      } catch (RuntimeException ex) {
        metrics.counter("media.cleanup.errors").increment();
        log.warn("mediaId={} operation=cleanup result=retry", file.id());
      }
    }
  }
}
