package com.tyson.digitalwallet.ddd.application.media;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.Test;

public class ImageValidatorTest {
  private final ImageValidator validator = new ImageValidator();

  @Test
  public void acceptsPngAndJpegBasedOnContent() throws Exception {
    for (String format : new String[] {"png", "jpeg"}) {
      var out = new ByteArrayOutputStream();
      ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, out);
      assertEquals("image/" + format, validator.validate(out.toByteArray()));
    }
  }

  @Test
  public void rejectsInvalidContent() {
    for (byte[] bytes :
        new byte[][] {
          new byte[0], "GIF89a".getBytes(), new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}
        }) assertThrows(MediaException.class, () -> validator.validate(bytes));
  }

  @Test
  public void rejectsOversizedFile() {
    assertEquals(
        413,
        assertThrows(MediaException.class, () -> validator.validate(new byte[5 * 1024 * 1024 + 1]))
            .status());
  }

  @Test
  public void rejectsOversizedDimensions() throws Exception {
    var out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(5000, 4001, BufferedImage.TYPE_BYTE_GRAY), "png", out);
    assertEquals(
        415,
        assertThrows(MediaException.class, () -> validator.validate(out.toByteArray())).status());
  }
}
