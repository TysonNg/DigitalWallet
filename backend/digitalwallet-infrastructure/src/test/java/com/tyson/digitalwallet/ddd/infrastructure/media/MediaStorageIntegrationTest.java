package com.tyson.digitalwallet.ddd.infrastructure.media;

import static org.junit.Assert.*;

import com.tyson.digitalwallet.ddd.application.media.*;
import com.tyson.digitalwallet.ddd.domain.model.entity.MediaFile;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.time.Clock;
import java.util.*;
import org.junit.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** Opt-in: requires an ISOLATED PostgreSQL database and initialized private MinIO bucket. */
public class MediaStorageIntegrationTest {
  String schema;
  JdbcTemplate admin;
  UUID owner = UUID.randomUUID();
  S3Client client;
  MediaService service;
  JdbcMediaRepository repository;

  @Before
  public void setup() throws Exception {
    var url = System.getProperty("media.test.jdbc");
    var endpoint = System.getProperty("media.test.endpoint");
    Assume.assumeTrue(
        "Isolated PostgreSQL and initialized MinIO are required", url != null && endpoint != null);
    String user = System.getenv("MEDIA_TEST_DB_USER"),
        password = System.getenv("MEDIA_TEST_DB_PASSWORD");
    admin = new JdbcTemplate(new DriverManagerDataSource(url, user, password));
    schema = "media_test_" + UUID.randomUUID().toString().replace("-", "");
    admin.execute("CREATE SCHEMA " + schema);
    var ds =
        new DriverManagerDataSource(
            url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, user, password);
    var jdbc = new JdbcTemplate(ds);
    jdbc.execute("CREATE TABLE users(id UUID PRIMARY KEY)");
    jdbc.update("INSERT INTO users VALUES (?)", owner);
    try (var in = getClass().getResourceAsStream("/db/migration/V19__create_media_files.sql")) {
      for (var statement :
          new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split(";"))
        if (!statement.isBlank()) jdbc.execute(statement);
    }
    repository = new JdbcMediaRepository(jdbc);
    client =
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.US_EAST_1)
            .forcePathStyle(true)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        System.getenv("MEDIA_S3_ACCESS_KEY"),
                        System.getenv("MEDIA_S3_SECRET_KEY"))))
            .build();
    service =
        new MediaService(
            repository,
            new S3MediaStorage(client),
            System.getProperty("media.test.bucket", "digital-wallet-media"),
            Clock.systemUTC(),
            new SimpleMeterRegistry());
  }

  @After
  public void teardown() {
    if (client != null) client.close();
    if (schema != null) admin.execute("DROP SCHEMA " + schema + " CASCADE");
  }

  @Test
  public void fullStorageLifecycleAndPrivateBucket() throws Exception {
    var output = new ByteArrayOutputStream();
    javax.imageio.ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
    byte[] bytes = output.toByteArray();
    var file = service.upload(owner, "photo.jpg", bytes);
    try {
      assertEquals(MediaFile.Status.READY, repository.find(file.id()).orElseThrow().status());
      try (var stream = service.open(owner, file.id())) {
        assertArrayEquals(bytes, stream.readAllBytes());
      }
      assertEquals(
          404,
          assertThrows(MediaException.class, () -> service.metadata(UUID.randomUUID(), file.id()))
              .status());
      var connection =
          (HttpURLConnection)
              URI.create(
                      System.getProperty("media.test.endpoint")
                          + "/"
                          + file.bucket()
                          + "/"
                          + file.objectKey())
                  .toURL()
                  .openConnection();
      try {
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        assertEquals(403, connection.getResponseCode());
      } finally {
        connection.disconnect();
      }
      var restartContainer = System.getProperty("media.test.restart-container");
      if (restartContainer != null) {
        if (!restartContainer.matches("[0-9a-f]{64}"))
          throw new IllegalArgumentException("Expected an isolated test container ID");
        var inspect =
            new ProcessBuilder(
                    "docker",
                    "inspect",
                    "--format",
                    "{{json .Config.Labels}}",
                    restartContainer)
                .redirectErrorStream(true)
                .start();
        if (!inspect.waitFor(15, java.util.concurrent.TimeUnit.SECONDS))
          throw new IllegalStateException("Container inspection timed out");
        assertEquals(
            "media-integration",
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(inspect.getInputStream()).path("digitalwallet.test-purpose").asText());
        var restart =
            new ProcessBuilder("docker", "restart", "--time", "5", restartContainer)
                .redirectErrorStream(true)
                .start();
        if (!restart.waitFor(45, java.util.concurrent.TimeUnit.SECONDS))
          throw new IllegalStateException("MinIO restart timed out");
        assertEquals(0, restart.exitValue());
        boolean ready = false;
        for (int attempt = 0; attempt < 30; attempt++) {
          try {
            client.headBucket(b -> b.bucket(file.bucket()));
            ready = true;
            break;
          } catch (RuntimeException unavailable) {
            Thread.sleep(1000);
          }
        }
        assertTrue("MinIO must recover after restart", ready);
      }
      // Verify durable metadata and object bytes after a real isolated-container restart when
      // enabled.
      var reopened =
          new MediaService(
              repository,
              new S3MediaStorage(client),
              file.bucket(),
              Clock.systemUTC(),
              new SimpleMeterRegistry());
      try (var stream = reopened.open(owner, file.id())) {
        assertArrayEquals(bytes, stream.readAllBytes());
      }
    } finally {
      service.delete(owner, file.id());
    }
    assertEquals(MediaFile.Status.DELETED, repository.find(file.id()).orElseThrow().status());
    service.delete(owner, file.id());
    assertThrows(MediaException.class, () -> service.metadata(owner, file.id()));
  }
  @Test
  public void personalReceiveQrV20StableReservationCasAndPrivatePng() throws Exception {
    String url=System.getProperty("media.test.jdbc");
    var ds=new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+schema,
        System.getenv("MEDIA_TEST_DB_USER"),System.getenv("MEDIA_TEST_DB_PASSWORD"));
    var jdbc=new JdbcTemplate(ds);
    UUID wallet=UUID.randomUUID();
    jdbc.execute("CREATE TABLE wallets(id UUID PRIMARY KEY)");
    jdbc.update("INSERT INTO wallets VALUES (?)",wallet);
    try(var in=getClass().getResourceAsStream("/db/migration/V20__create_wallet_receive_codes.sql")) {
      jdbc.execute(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
    }
    var factory=new org.springframework.aop.framework.ProxyFactory(
        new com.tyson.digitalwallet.ddd.infrastructure.receiveqr.JdbcReceiveCodeRepository(jdbc));
    factory.setProxyTargetClass(true);
    factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(
        new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds),
        new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
    var codes=(com.tyson.digitalwallet.ddd.application.receiveqr.ReceiveCodeRepository)factory.getProxy();
    String token=UUID.randomUUID().toString();
    var reservation=codes.reserve(wallet,token);
    assertEquals(reservation,codes.reserve(wallet,UUID.randomUUID().toString()));
    String receiveUrl="http://localhost:3000/vi/receive/"+token;
    byte[] png=new com.tyson.digitalwallet.ddd.infrastructure.receiveqr.ZxingReceiveQrRenderer().render(receiveUrl);
    var generated=new ArrayList<UUID>();
    try {
      var winner=service.storeGeneratedImage(owner,"receive-qr.png",png);generated.add(winner.id());
      var loser=service.storeGeneratedImage(owner,"receive-qr.png",png);generated.add(loser.id());
      assertTrue(codes.attach(wallet,null,winner.id()));
      assertFalse(codes.attach(wallet,null,loser.id()));
      service.delete(owner,loser.id());
      assertEquals(winner.id(),codes.findByToken(token).orElseThrow().mediaId());
      try(var stream=service.open(owner,winner.id())) {
        var image=javax.imageio.ImageIO.read(stream);
        assertEquals(360,image.getWidth());assertEquals(360,image.getHeight());
        var bitmap=new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(
            new com.google.zxing.client.j2se.BufferedImageLuminanceSource(image)));
        assertEquals(receiveUrl,new com.google.zxing.MultiFormatReader().decode(bitmap).getText());
      }
      assertEquals(404,assertThrows(MediaException.class,()->service.metadata(UUID.randomUUID(),winner.id())).status());
      var connection=(HttpURLConnection)URI.create(System.getProperty("media.test.endpoint")+"/"+winner.bucket()+"/"+winner.objectKey()).toURL().openConnection();
      try { connection.setConnectTimeout(5000);connection.setReadTimeout(5000);assertEquals(403,connection.getResponseCode()); }
      finally { connection.disconnect(); }
      service.delete(owner,winner.id());
      var replacement=service.storeGeneratedImage(owner,"receive-qr.png",png);generated.add(replacement.id());
      assertTrue(codes.attach(wallet,winner.id(),replacement.id()));
      assertEquals(token,codes.findByWallet(wallet).orElseThrow().token());
      assertEquals(replacement.id(),codes.findByWallet(wallet).orElseThrow().mediaId());
    } finally {
      for(UUID id:generated) service.delete(owner,id);
    }
  }
}