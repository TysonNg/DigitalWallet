package com.tyson.digitalwallet.ddd.infrastructure.receiveqr;

import com.tyson.digitalwallet.ddd.application.media.MediaService;
import com.tyson.digitalwallet.ddd.application.port.out.qr.ReceiveQrRenderer;
import com.tyson.digitalwallet.ddd.application.receiveqr.ReceiveQrService;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.LookupRecipientUseCase;
import com.tyson.digitalwallet.ddd.domain.repository.ReceiveCodeRepository;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(name = "media.enabled", havingValue = "true")
public class ReceiveQrConfiguration {

  @Bean
  ReceiveCodeRepository receiveCodeRepository(JdbcTemplate jdbc) {
    return new JdbcReceiveCodeRepository(jdbc);
  }

  @Bean
  ReceiveQrRenderer receiveQrRenderer() {
    return new ZxingReceiveQrRenderer();
  }

  @Bean
  ReceiveQrService receiveQrService(
      ReceiveCodeRepository codes,
      MediaService media,
      ReceiveQrRenderer renderer,
      WalletRepository wallets,
      UserRepository users,
      LookupRecipientUseCase lookup,
      @Value("${media.frontend-public-url:http://localhost:3000}") String publicUrl) {
    return new ReceiveQrService(codes, media, renderer, wallets, users, lookup, publicUrl);
  }
}