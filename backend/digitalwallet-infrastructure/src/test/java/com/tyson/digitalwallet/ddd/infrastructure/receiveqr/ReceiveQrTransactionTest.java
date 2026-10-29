package com.tyson.digitalwallet.ddd.infrastructure.receiveqr;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.tyson.digitalwallet.ddd.application.receiveqr.*;
import com.tyson.digitalwallet.ddd.application.media.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.LookupRecipientUseCase;
import com.tyson.digitalwallet.ddd.domain.model.entity.ReceiveCode;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import com.tyson.digitalwallet.ddd.domain.repository.*;
import com.tyson.digitalwallet.ddd.domain.repository.ReceiveCodeRepository;
import com.tyson.digitalwallet.ddd.infrastructure.media.JdbcMediaRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.*;

public class ReceiveQrTransactionTest {
  UUID owner=UUID.randomUUID(),walletId=UUID.randomUUID();
  DataSourceTransactionManager manager;
  JdbcTemplate jdbc;
  ReceiveCodeRepository codes;
  MediaService media;
  Map<String,byte[]> objects=new HashMap<>();
  List<String> deleted=new ArrayList<>();
  AtomicBoolean loseCommitAck=new AtomicBoolean();
  <T> T proxy(T target) {
    var factory=new ProxyFactory(target);factory.setProxyTargetClass(true);
    factory.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));
    return (T)factory.getProxy();
  }
  @Before public void setup() throws Exception {
    var ds=new DriverManagerDataSource("jdbc:h2:mem:txqr_"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","");
    manager=new DataSourceTransactionManager(ds) {
      @Override protected void doCommit(DefaultTransactionStatus status) {
        super.doCommit(status);
        if(loseCommitAck.compareAndSet(true,false)) throw new TransactionSystemException("Lost commit acknowledgement");
      }
    };
    jdbc=new JdbcTemplate(ds);
    jdbc.execute("CREATE DOMAIN TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
    jdbc.execute("CREATE TABLE users(id UUID PRIMARY KEY)");jdbc.update("INSERT INTO users VALUES (?)",owner);
    jdbc.execute("CREATE TABLE wallets(id UUID PRIMARY KEY)");jdbc.update("INSERT INTO wallets VALUES (?)",walletId);
    for(String migration:new String[]{"V19__create_media_files.sql","V20__create_wallet_receive_codes.sql"}) {
      String sql=new String(getClass().getResourceAsStream("/db/migration/"+migration).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
      for(String statement:sql.split(";")) if(!statement.isBlank()&&!statement.contains("idx_media_cleanup")) jdbc.execute(statement);
    }
    codes=proxy(new JdbcReceiveCodeRepository(jdbc));
    MediaStoragePort storage=new MediaStoragePort() {
      public void put(String bucket,String key,String type,byte[] bytes) { assertNoTx();objects.put(key,bytes); }
      public InputStream open(String bucket,String key) { assertNoTx();return new ByteArrayInputStream(objects.get(key)); }
      public void delete(String bucket,String key) { assertNoTx();deleted.add(key);objects.remove(key); }
    };
    media=new MediaService(proxy(new JdbcMediaRepository(jdbc)),storage,"private",Clock.systemUTC(),new SimpleMeterRegistry());
  }
  void assertNoTx() { assertFalse("Storage/renderer must run outside any transaction",TransactionSynchronizationManager.isActualTransactionActive()); }
  ReceiveQrService service(ReceiveCodeRepository repository) {
    var wallets=mock(WalletRepository.class);
    when(wallets.findByUserId(owner)).thenReturn(Optional.of(new Wallet(walletId,owner,BigDecimal.ZERO,"VND",WalletStatus.ACTIVE,null,null)));
    ReceiveQrRenderer renderer=url->{assertNoTx();return new ZxingReceiveQrRenderer().render(url);};
    return proxy(new ReceiveQrService(repository,media,renderer,wallets,mock(UserRepository.class),mock(LookupRecipientUseCase.class),"http://localhost:3000"));
  }
  @Test public void ambientTransactionSuspendedForStorageAndResumedWithDurableAttachment() {
    var service=service(codes);
    var qr=new TransactionTemplate(manager).execute(tx->{
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      var result=service.getOrCreate(owner);
      assertTrue("Caller transaction must resume",TransactionSynchronizationManager.isActualTransactionActive());
      tx.setRollbackOnly();return result;
    });
    assertEquals(qr.mediaId(),codes.findByWallet(walletId).orElseThrow().mediaId());
    assertEquals(qr.mediaId(),media.metadata(owner,qr.mediaId()).id());
    assertEquals(1,objects.size());
  }
  @Test public void ambientTransactionAlsoSuspendedDuringCasLoserCleanup() {
    var competing=mock(ReceiveCodeRepository.class,org.mockito.AdditionalAnswers.delegatesTo(codes));
    doAnswer(call->{
      var winner=media.storeGeneratedImage(owner,"winner.png",new ZxingReceiveQrRenderer().render("http://localhost:3000/vi/receive/12345678-1234-4234-8234-123456789abc"));
      assertTrue(codes.attach(walletId,null,winner.id()));
      return codes.attach(call.getArgument(0),call.getArgument(1),call.getArgument(2));
    }).when(competing).attach(any(),any(),any());
    var service=service(competing);
    var qr=new TransactionTemplate(manager).execute(tx->{
      var result=service.getOrCreate(owner);
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      tx.setRollbackOnly();return result;
    });
    assertEquals(qr.mediaId(),codes.findByWallet(walletId).orElseThrow().mediaId());
    assertEquals(1,deleted.size());assertEquals(1,objects.size());
    assertEquals(qr.mediaId(),media.metadata(owner,qr.mediaId()).id());
  }
  @Test public void actualCommitWithLostAcknowledgementPreservesAttachedBytes() {
    var ambiguous=mock(ReceiveCodeRepository.class,org.mockito.AdditionalAnswers.delegatesTo(codes));
    doAnswer(call->{loseCommitAck.set(true);return codes.attach(call.getArgument(0),call.getArgument(1),call.getArgument(2));})
        .when(ambiguous).attach(any(),any(),any());
    var service=service(ambiguous);
    assertThrows(TransactionSystemException.class,()->service.getOrCreate(owner));
    UUID attached=codes.findByWallet(walletId).orElseThrow().mediaId();
    assertNotNull(attached);assertEquals(attached,media.metadata(owner,attached).id());
    assertTrue(deleted.isEmpty());assertEquals(1,objects.size());
  }
}
