package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.media.MediaException;
import com.tyson.digitalwallet.ddd.application.receiveqr.ReceiveQrService;
import com.tyson.digitalwallet.ddd.application.usecase.receiveqr.GetOrCreateReceiveQrUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.receiveqr.ResolveReceiveRecipientUseCase;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import java.security.Principal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/wallet/receive-qr", "/api/v1/wallet/receive-qr"})
@ConditionalOnProperty(name = "media.enabled", havingValue = "true")
public class ReceiveQrController {
  private final GetOrCreateReceiveQrUseCase getOrCreateReceiveQrUseCase;
  private final ResolveReceiveRecipientUseCase resolveReceiveRecipientUseCase;

  @Autowired
  public ReceiveQrController(
      GetOrCreateReceiveQrUseCase getOrCreateReceiveQrUseCase,
      ResolveReceiveRecipientUseCase resolveReceiveRecipientUseCase) {
    this.getOrCreateReceiveQrUseCase = getOrCreateReceiveQrUseCase;
    this.resolveReceiveRecipientUseCase = resolveReceiveRecipientUseCase;
  }

  public ReceiveQrController(ReceiveQrService service) {
    this(service, service);
  }

  @PostMapping
  public ResponseEntity<ApiResponse<ReceiveQrService.Qr>> create(Principal principal) {
    return ApiResponse.ok(
        "Receive QR retrieved", getOrCreateReceiveQrUseCase.getOrCreate(owner(principal)));
  }

  @GetMapping("/{token}/recipient")
  public ResponseEntity<ApiResponse<ReceiveQrService.Recipient>> recipient(
      @PathVariable("token") String token, Principal principal) {
    return ApiResponse.ok(
        "Recipient found", resolveReceiveRecipientUseCase.resolve(owner(principal), token));
  }

  private UUID owner(Principal principal) {
    if (principal == null) {
      throw new MediaException(401, "UNAUTHORIZED", "Authentication is required");
    }
    try {
      return UUID.fromString(principal.getName());
    } catch (IllegalArgumentException ex) {
      throw new MediaException(401, "UNAUTHORIZED", "Invalid authenticated user");
    }
  }
}
