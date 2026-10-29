package com.tyson.digitalwallet.ddd.infrastructure.receiveqr;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.Test;

public class ZxingReceiveQrRendererTest {
  @Test public void renders360PixelPngWithWhiteQuietZone() throws Exception {
    String url="https://wallet.example/vi/receive/12345678-1234-4234-8234-123456789abc";
    byte[] png=new ZxingReceiveQrRenderer().render(url);
    var image = ImageIO.read(new ByteArrayInputStream(png));
    assertNotNull(image);
    var bitmap=new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(
        new com.google.zxing.client.j2se.BufferedImageLuminanceSource(image)));
    assertEquals(url,new com.google.zxing.MultiFormatReader().decode(bitmap).getText());
    assertEquals(360, image.getWidth());
    assertEquals(360, image.getHeight());
    for (int n=0;n<360;n++) {
      assertEquals(-1, image.getRGB(n, 0));
      assertEquals(-1, image.getRGB(0, n));
      assertEquals(-1, image.getRGB(n, 359));
      assertEquals(-1, image.getRGB(359, n));
    }
  }
}
