package com.tyson.digitalwallet.ddd.application.receiveqr;

import com.tyson.digitalwallet.ddd.application.exception.*;
import com.tyson.digitalwallet.ddd.application.media.MediaException;
import com.tyson.digitalwallet.ddd.application.media.MediaService;
import com.tyson.digitalwallet.ddd.application.port.out.qr.ReceiveQrRenderer;
import com.tyson.digitalwallet.ddd.application.usecase.receiveqr.GetOrCreateReceiveQrUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.receiveqr.ResolveReceiveRecipientUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.LookupRecipientUseCase;
import com.tyson.digitalwallet.ddd.domain.model.entity.ReceiveCode;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import com.tyson.digitalwallet.ddd.domain.repository.ReceiveCodeRepository;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import java.net.URI;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.*;

/**
 * Suspends any caller transaction before independently committed DB operations and storage I/O.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ReceiveQrService implements GetOrCreateReceiveQrUseCase, ResolveReceiveRecipientUseCase {
  private static final Logger log = LoggerFactory.getLogger(ReceiveQrService.class);

  private final ReceiveCodeRepository codes;
  private final MediaService media;
  private final ReceiveQrRenderer renderer;
  private final WalletRepository wallets;
  private final UserRepository users;
  private final LookupRecipientUseCase lookup;
  private final String publicUrl;

  public ReceiveQrService(
      ReceiveCodeRepository codes,
      MediaService media,
      ReceiveQrRenderer renderer,
      WalletRepository wallets,
      UserRepository users,
      LookupRecipientUseCase lookup,
      String publicUrl) {
    this.codes = codes;
    this.media = media;
    this.renderer = renderer;
    this.wallets = wallets;
    this.users = users;
    this.lookup = lookup;

    var uri = URI.create(publicUrl);
    if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null) {
      throw new IllegalArgumentException("Invalid frontend public URL");
    }
    this.publicUrl = publicUrl.replaceAll("/+$", "");
  }

  public record Qr(UUID mediaId, String contentUrl, String receiveUrl) {}

  public record Recipient(UUID walletId, String fullName, String currency) {}

  @Override
  public Qr getOrCreate(UUID owner) {
    authenticated(owner);
    var wallet =
        wallets
            .findByUserId(owner)
            .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
    active(wallet);
    var code = codes.reserve(wallet.getId(), UUID.randomUUID().toString());

    for (int attempt = 0; attempt < 3; attempt++) {
      String receiveUrl = publicUrl + "/vi/receive/" + code.token();
      if (code.mediaId() != null) {
        try {
          media.metadata(owner, code.mediaId());
          return response(code.mediaId(), receiveUrl);
        } catch (MediaException ex) {
          if (ex.status() != 404) throw ex;
        }
      }

      var generated = media.storeGeneratedImage(owner, "receive-qr.png", renderer.render(receiveUrl));
      log.info(
          "mediaId={} walletId={} operation=receive-qr result=generated",
          generated.id(),
          wallet.getId());

      // Never catch an attachment exception and delete: the commit may have succeeded.
      if (codes.attach(wallet.getId(), code.mediaId(), generated.id())) {
        return response(generated.id(), receiveUrl);
      }

      // CAS returned false, so this private, unique candidate was definitively never referenced.
      try {
        media.delete(owner, generated.id());
      } catch (RuntimeException ex) {
        log.warn("mediaId={} operation=receive-qr result=loser-cleanup-retry", generated.id());
      }
      code = codes.findByWallet(wallet.getId()).orElseThrow(MediaException::unavailable);
    }
    throw MediaException.unavailable();
  }

  @Override
  public Recipient resolve(UUID caller, String token) {
    authenticated(caller);
    if (token == null
        || !token.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
      throw missingCode();
    }
    var code = codes.findByToken(token).orElseThrow(ReceiveQrService::missingCode);
    var wallet = wallets.findById(code.walletId()).orElseThrow(ReceiveQrService::missingCode);
    active(wallet);
    var user =
        users
            .findById(wallet.getUserId())
            .orElseThrow(() -> new UserNotFoundException("Recipient not found"));
    if (!user.isActive()) throw new UserNotActiveException("Recipient account is not active");

    // Existing lookup enforces self-transfer and recipient policy. Fresh reads above prevent its
    // ten-minute cache from making an inactive recipient appear transferable. Project fresh data.
    lookup.lookup(caller, wallet.getId().toString());
    return new Recipient(wallet.getId(), user.getFullName(), wallet.getCurrency());
  }

  private static Qr response(UUID id, String url) {
    return new Qr(id, "/api/v1/media/" + id + "/content", url);
  }

  private static MediaException missingCode() {
    return new MediaException(404, "RECEIVE_QR_NOT_FOUND", "Receive QR not found");
  }

  private static void authenticated(UUID caller) {
    if (caller == null) throw new MediaException(401, "UNAUTHORIZED", "Authentication is required");
  }

  private static void active(Wallet wallet) {
    if (wallet.getStatus() != WalletStatus.ACTIVE) {
      throw new AppException(ErrorCode.WALLET_NOT_ACTIVE, "Wallet is not active");
    }
  }
}