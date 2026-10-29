package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.MediaFile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaRepository {
  void insert(MediaFile file);

  Optional<MediaFile> find(UUID id);

  boolean transition(UUID id, MediaFile.Status expected, MediaFile.Status next, Instant now);

  List<MediaFile> cleanupCandidates(Instant staleBefore, int limit);
}
