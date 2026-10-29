package com.tyson.digitalwallet.ddd.application.usecase.receiveqr;

import com.tyson.digitalwallet.ddd.application.receiveqr.ReceiveQrService;
import java.util.UUID;

public interface ResolveReceiveRecipientUseCase {
  ReceiveQrService.Recipient resolve(UUID caller, String token);
}
