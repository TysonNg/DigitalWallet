package com.tyson.digitalwallet.ddd.infrastructure.media;

import com.tyson.digitalwallet.ddd.application.media.MediaService;
import com.tyson.digitalwallet.ddd.application.port.out.storage.MediaStoragePort;
import com.tyson.digitalwallet.ddd.domain.repository.MediaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "media.enabled", havingValue = "true")
public class MediaConfiguration {

  @Bean(destroyMethod = "close")
  S3Client mediaS3Client(
      @Value("${media.s3.endpoint:}") String endpoint,
      @Value("${media.s3.region:us-east-1}") String region,
      @Value("${media.s3.access-key:}") String key,
      @Value("${media.s3.secret-key:}") String secret,
      @Value("${media.s3.path-style:false}") boolean pathStyle) {
    var builder =
        S3Client.builder()
            .region(Region.of(region))
            .forcePathStyle(pathStyle)
            .overrideConfiguration(
                c ->
                    c.apiCallTimeout(Duration.ofSeconds(30))
                        .apiCallAttemptTimeout(Duration.ofSeconds(10)));
    if (!endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint));
    if (!key.isBlank() || !secret.isBlank()) {
      if (key.isBlank() || secret.isBlank())
        throw new IllegalArgumentException("Both media S3 credentials must be provided");
      builder.credentialsProvider(
          StaticCredentialsProvider.create(AwsBasicCredentials.create(key, secret)));
    }
    return builder.build();
  }

  @Bean
  MediaStoragePort mediaStorage(S3Client mediaS3Client) {
    return new S3MediaStorage(mediaS3Client);
  }

  @Bean
  MediaRepository mediaRepository(JdbcTemplate jdbc) {
    return new JdbcMediaRepository(jdbc);
  }

  @Bean
  MediaService mediaService(
      MediaRepository repository,
      MediaStoragePort storage,
      @Value("${media.s3.bucket:digital-wallet-media}") String bucket,
      MeterRegistry registry) {
    return new MediaService(repository, storage, bucket, Clock.systemUTC(), registry);
  }

  @Bean
  MediaCleanupJob mediaCleanupJob(MediaService service, MeterRegistry registry) {
    return new MediaCleanupJob(service, registry);
  }

  static class MediaCleanupJob {
    private final MediaService service;
    private final MeterRegistry registry;

    MediaCleanupJob(MediaService service, MeterRegistry registry) {
      this.service = service;
      this.registry = registry;
    }

    @Scheduled(
        fixedDelayString = "${media.cleanup.interval-ms:600000}",
        initialDelayString = "${media.cleanup.interval-ms:600000}")
    public void run() {
      try {
        service.cleanup();
      } catch (RuntimeException ex) {
        registry.counter("media.cleanup.errors").increment();
      }
    }
  }
}
