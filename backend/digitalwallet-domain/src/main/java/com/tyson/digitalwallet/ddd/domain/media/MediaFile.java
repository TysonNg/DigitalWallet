package com.tyson.digitalwallet.ddd.domain.media;

import java.time.Instant;
import java.util.UUID;

public record MediaFile(
    UUID id,
    UUID ownerId,
    String bucket,
    String objectKey,
    String originalName,
    String contentType,
    long sizeBytes,
    Status status,
    Instant createdAt,
    Instant updatedAt) {
  public enum Status {
    PENDING,
    READY,
    DELETING,
    DELETED
  }
}
