package com.tyson.digitalwallet.ddd.infrastructure.media;

import static org.junit.Assert.*;

import com.tyson.digitalwallet.ddd.application.media.MediaService;
import java.util.Map;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

public class MediaConfigurationTest {
  @Test
  public void disabledFeatureDoesNotRequireStorageOrDatabaseBeans() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context
          .getEnvironment()
          .getPropertySources()
          .addFirst(new MapPropertySource("media-test", Map.of("media.enabled", "false")));
      context.register(MediaConfiguration.class, com.tyson.digitalwallet.ddd.infrastructure.receiveqr.ReceiveQrConfiguration.class);
      context.refresh();
      assertTrue(context.getBeansOfType(MediaService.class).isEmpty());
      assertTrue(context.getBeansOfType(com.tyson.digitalwallet.ddd.application.receiveqr.ReceiveQrService.class).isEmpty());
      assertTrue(context.getBeansOfType(com.tyson.digitalwallet.ddd.application.receiveqr.ReceiveCodeRepository.class).isEmpty());
      assertTrue(context.getBeansOfType(com.tyson.digitalwallet.ddd.application.receiveqr.ReceiveQrRenderer.class).isEmpty());
      assertTrue(
          context.getBeansOfType(software.amazon.awssdk.services.s3.S3Client.class).isEmpty());
    }
  }
}
