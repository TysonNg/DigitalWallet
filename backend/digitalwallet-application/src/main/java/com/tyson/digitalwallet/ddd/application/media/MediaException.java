package com.tyson.digitalwallet.ddd.application.media;

public class MediaException extends RuntimeException {
  private final int status;
  private final String code;

  public MediaException(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public static MediaException missing() {
    return new MediaException(404, "MEDIA_NOT_FOUND", "Media not found");
  }

  public static MediaException unavailable() {
    return new MediaException(503, "MEDIA_UNAVAILABLE", "Media storage is unavailable");
  }
}
