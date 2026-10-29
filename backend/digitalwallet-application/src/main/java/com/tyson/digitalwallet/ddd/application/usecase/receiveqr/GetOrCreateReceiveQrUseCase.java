package com.tyson.digitalwallet.ddd.application.usecase.receiveqr;

import com.tyson.digitalwallet.ddd.application.receiveqr.ReceiveQrService;
import java.util.UUID;

public interface GetOrCreateReceiveQrUseCase {
  ReceiveQrService.Qr getOrCreate(UUID owner);
}
