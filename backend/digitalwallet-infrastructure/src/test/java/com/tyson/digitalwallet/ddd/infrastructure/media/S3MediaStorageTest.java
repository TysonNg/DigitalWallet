package com.tyson.digitalwallet.ddd.infrastructure.media;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tyson.digitalwallet.ddd.application.media.*;
import org.junit.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

public class S3MediaStorageTest {
  S3Client client = mock(S3Client.class);
  S3MediaStorage storage = new S3MediaStorage(client);

  @Test
  public void missingObjectIs404() {
    when(client.getObject(any(GetObjectRequest.class)))
        .thenThrow(NoSuchKeyException.builder().statusCode(404).build());
    assertEquals(
        404, assertThrows(MediaException.class, () -> storage.open("bucket", "key")).status());
  }

  @Test
  public void missingObjectDeleteIsIdempotent() {
    when(client.deleteObject(any(DeleteObjectRequest.class)))
        .thenThrow(S3Exception.builder().statusCode(404).build());
    storage.delete("bucket", "key");
  }

  @Test
  public void storageOutageIs503() {
    when(client.getObject(any(GetObjectRequest.class)))
        .thenThrow(S3Exception.builder().statusCode(503).build());
    assertEquals(
        503, assertThrows(MediaException.class, () -> storage.open("bucket", "key")).status());
  }
}
