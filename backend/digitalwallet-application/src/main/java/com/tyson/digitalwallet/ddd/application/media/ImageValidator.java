package com.tyson.digitalwallet.ddd.application.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

public final class ImageValidator {
  public static final int MAX_BYTES = 5 * 1024 * 1024;

  public String validate(byte[] bytes) {
    if (bytes.length > MAX_BYTES)
      throw new MediaException(413, "MEDIA_TOO_LARGE", "Maximum file size is 5 MiB");
    if (bytes.length == 0) throw invalid();
    boolean png =
        bytes.length >= 8
            && bytes[0] == (byte) 137
            && bytes[1] == 80
            && bytes[2] == 78
            && bytes[3] == 71
            && bytes[4] == 13
            && bytes[5] == 10
            && bytes[6] == 26
            && bytes[7] == 10;
    boolean jpeg =
        bytes.length >= 3
            && bytes[0] == (byte) 255
            && bytes[1] == (byte) 216
            && bytes[2] == (byte) 255;
    if (!png && !jpeg) throw invalid();
    try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) throw invalid();
      ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!(png && format.equals("png"))
            && !(jpeg && (format.equals("jpeg") || format.equals("jpg")))) throw invalid();
        int width = reader.getWidth(0), height = reader.getHeight(0);
        if (width <= 0 || height <= 0 || (long) width * height > 20_000_000) throw invalid();
        if (reader.read(0) == null) throw invalid();
        return png ? "image/png" : "image/jpeg";
      } finally {
        reader.dispose();
      }
    } catch (IOException | IllegalArgumentException ex) {
      throw invalid();
    }
  }

  private MediaException invalid() {
    return new MediaException(
        415, "MEDIA_INVALID_IMAGE", "Expected a valid PNG/JPEG image of at most 20 megapixels");
  }
}
