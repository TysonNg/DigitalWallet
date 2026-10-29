package com.tyson.digitalwallet.ddd.infrastructure.media;

import com.tyson.digitalwallet.ddd.application.media.MediaException;
import com.tyson.digitalwallet.ddd.application.port.out.storage.MediaStoragePort;
import java.io.InputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

public final class S3MediaStorage implements
    com.tyson.digitalwallet.ddd.application.port.out.storage.MediaStoragePort,
    com.tyson.digitalwallet.ddd.application.media.MediaStoragePort {
  private final S3Client client;

  public S3MediaStorage(S3Client client) {
    this.client = client;
  }

  @Override
  public void put(String bucket, String key, String type, byte[] bytes) {
    try {
      client.putObject(
          PutObjectRequest.builder().bucket(bucket).key(key).contentType(type).build(),
          RequestBody.fromBytes(bytes));
    } catch (RuntimeException ex) {
      throw MediaException.unavailable();
    }
  }

  @Override
  public InputStream open(String bucket, String key) {
    try {
      return client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    } catch (NoSuchKeyException ex) {
      throw MediaException.missing();
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) throw MediaException.missing();
      throw MediaException.unavailable();
    } catch (RuntimeException ex) {
      throw MediaException.unavailable();
    }
  }

  @Override
  public void delete(String bucket, String key) {
    try {
      client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    } catch (S3Exception ex) {
      if (ex.statusCode() != 404) throw MediaException.unavailable();
    } catch (RuntimeException ex) {
      throw MediaException.unavailable();
    }
  }
}
