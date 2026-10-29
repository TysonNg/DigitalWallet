package com.tyson.digitalwallet.ddd.infrastructure.receiveqr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.tyson.digitalwallet.ddd.application.media.MediaException;
import com.tyson.digitalwallet.ddd.application.port.out.qr.ReceiveQrRenderer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class ZxingReceiveQrRenderer implements
    com.tyson.digitalwallet.ddd.application.port.out.qr.ReceiveQrRenderer,
    com.tyson.digitalwallet.ddd.application.receiveqr.ReceiveQrRenderer {
  @Override
  public byte[] render(String receiveUrl) {
    try {
      var matrix =
          new QRCodeWriter()
              .encode(
                  receiveUrl,
                  BarcodeFormat.QR_CODE,
                  360,
                  360,
                  Map.of(
                      EncodeHintType.MARGIN, 4,
                      EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                      EncodeHintType.CHARACTER_SET, "UTF-8"));
      var output = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", output);
      return output.toByteArray();
    } catch (WriterException | IOException ex) {
      throw MediaException.unavailable();
    }
  }
}