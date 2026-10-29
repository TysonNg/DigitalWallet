package com.tyson.digitalwallet.ddd.application.port.out.storage;

import java.io.InputStream;

public interface MediaStoragePort {
  void put(String bucket, String key, String contentType, byte[] bytes);

  InputStream open(String bucket, String key);

  void delete(String bucket, String key);
}
