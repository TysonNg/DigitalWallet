package com.tyson.digitalwallet.ddd.application.port.out.qr;

public interface ReceiveQrRenderer {
  byte[] render(String receiveUrl);
}
