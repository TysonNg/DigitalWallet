package com.tyson.digitalwallet.ddd.application.media;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tyson.digitalwallet.ddd.domain.model.entity.MediaFile;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.Before;
import org.junit.Test;

public class MediaServiceTest {
  final MediaRepository repository = mock(MediaRepository.class);
  final MediaStoragePort storage = mock(MediaStoragePort.class);
  final UUID owner = UUID.randomUUID();
  final Instant now = Instant.parse("2026-10-29T00:00:00Z");
  final MediaService service =
      new MediaService(
          repository,
          storage,
          "media",
          Clock.fixed(now, ZoneOffset.UTC),
          new SimpleMeterRegistry());
  MediaFile inserted;

  @Before
  public void repositoryDefaults() {
    doAnswer(
            inv -> {
              inserted = inv.getArgument(0);
              return null;
            })
        .when(repository)
        .insert(any());
    when(repository.find(any())).thenAnswer(inv -> Optional.ofNullable(inserted));
    when(repository.transition(
            any(), eq(MediaFile.Status.PENDING), eq(MediaFile.Status.DELETING), any()))
        .thenReturn(true);
  }

  @Test
  public void lostReadyAcknowledgmentDoesNotDeleteCommittedImage() throws Exception {
    when(repository.transition(
            any(), eq(MediaFile.Status.PENDING), eq(MediaFile.Status.READY), any()))
        .thenAnswer(
            inv -> {
              inserted =
                  new MediaFile(
                      inserted.id(),
                      owner,
                      inserted.bucket(),
                      inserted.objectKey(),
                      inserted.originalName(),
                      inserted.contentType(),
                      inserted.sizeBytes(),
                      MediaFile.Status.READY,
                      now,
                      now);
              throw new IllegalStateException("lost commit acknowledgement");
            });
    assertEquals(MediaFile.Status.READY, service.upload(owner, "a", image()).status());
    verify(storage, never()).delete(any(), any());
  }

  @Test
  public void unavailableDatabaseLeavesObjectForRecovery() throws Exception {
    when(repository.transition(
            any(), eq(MediaFile.Status.PENDING), eq(MediaFile.Status.READY), any()))
        .thenThrow(new IllegalStateException());
    when(repository.find(any())).thenThrow(new IllegalStateException());
    assertThrows(MediaException.class, () -> service.upload(owner, "a", image()));
    verify(storage, never()).delete(any(), any());
  }

  @Test
  public void failedCleanupRotatesRetryTimestamp() {
    var file = file(MediaFile.Status.DELETING);
    when(repository.cleanupCandidates(any(), eq(100))).thenReturn(List.of(file));
    doThrow(new IllegalStateException()).when(storage).delete(any(), any());
    service.cleanup();
    verify(repository)
        .transition(file.id(), MediaFile.Status.DELETING, MediaFile.Status.DELETING, now);
  }

  byte[] image() throws Exception {
    var out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
    return out.toByteArray();
  }

  MediaFile file(MediaFile.Status status) {
    return new MediaFile(
        UUID.randomUUID(),
        owner,
        "media",
        "random/key",
        "a.png",
        "image/png",
        12,
        status,
        now,
        now);
  }

  @Test
  public void uploadsAndMarksReady() throws Exception {
    when(repository.transition(
            any(), eq(MediaFile.Status.PENDING), eq(MediaFile.Status.READY), any()))
        .thenReturn(true);
    var result = service.upload(owner, "../fake.jpg", image());
    assertEquals(MediaFile.Status.READY, result.status());
    assertEquals("image/png", result.contentType());
    assertFalse(result.objectKey().contains("fake"));
    verify(repository).insert(argThat(f -> f.status() == MediaFile.Status.PENDING));
    verify(storage).put(eq("media"), eq(result.objectKey()), eq("image/png"), any());
  }

  @Test
  public void hidesOtherOwnersAndPendingFiles() {
    var file = file(MediaFile.Status.READY);
    when(repository.find(file.id())).thenReturn(Optional.of(file));
    assertEquals(
        404,
        assertThrows(MediaException.class, () -> service.metadata(UUID.randomUUID(), file.id()))
            .status());
    var pending = file(MediaFile.Status.PENDING);
    when(repository.find(pending.id())).thenReturn(Optional.of(pending));
    assertThrows(MediaException.class, () -> service.metadata(owner, pending.id()));
    verifyNoInteractions(storage);
  }

  @Test
  public void storageFailureCompensates() throws Exception {
    doThrow(new IllegalStateException()).when(storage).put(any(), any(), any(), any());
    assertEquals(
        503,
        assertThrows(MediaException.class, () -> service.upload(owner, "a", image())).status());
    verify(storage).delete(any(), any());
  }

  @Test
  public void databaseFailureAfterPutCompensates() throws Exception {
    when(repository.transition(
            any(), eq(MediaFile.Status.PENDING), eq(MediaFile.Status.READY), any()))
        .thenThrow(new IllegalStateException());
    assertThrows(MediaException.class, () -> service.upload(owner, "a", image()));
    verify(storage).delete(any(), any());
  }

  @Test
  public void failedDeleteStaysHiddenAndCanRetry() {
    var file = file(MediaFile.Status.READY);
    when(repository.find(file.id())).thenReturn(Optional.of(file));
    when(repository.transition(file.id(), MediaFile.Status.READY, MediaFile.Status.DELETING, now))
        .thenReturn(true);
    doThrow(new IllegalStateException())
        .doNothing()
        .when(storage)
        .delete("media", file.objectKey());
    service.delete(owner, file.id());
    verify(repository, never())
        .transition(file.id(), MediaFile.Status.DELETING, MediaFile.Status.DELETED, now);
    var deleting =
        new MediaFile(
            file.id(),
            owner,
            "media",
            file.objectKey(),
            "a",
            "image/png",
            12,
            MediaFile.Status.DELETING,
            now,
            now);
    when(repository.cleanupCandidates(any(), eq(100))).thenReturn(List.of(deleting));
    service.cleanup();
    verify(repository)
        .transition(file.id(), MediaFile.Status.DELETING, MediaFile.Status.DELETED, now);
  }

  @Test
  public void deletionIsIdempotent() {
    var file = file(MediaFile.Status.DELETED);
    when(repository.find(file.id())).thenReturn(Optional.of(file));
    service.delete(owner, file.id());
    verifyNoInteractions(storage);
  }
}
