package com.tyson.digitalwallet.ddd.infrastructure.receiveqr;

import static org.junit.Assert.*;
import com.tyson.digitalwallet.ddd.application.receiveqr.*;
import com.tyson.digitalwallet.ddd.domain.model.entity.ReceiveCode;
import com.tyson.digitalwallet.ddd.domain.repository.ReceiveCodeRepository;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.*;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.aop.framework.ProxyFactory;

public class JdbcReceiveCodeRepositoryTest {
  DriverManagerDataSource ds;
  JdbcTemplate jdbc;
  ReceiveCodeRepository codes;
  UUID wallet=UUID.randomUUID();
  String token="12345678-1234-4234-8234-123456789abc";
  @Before public void setup() throws Exception {
    ds=new DriverManagerDataSource("jdbc:h2:mem:qr_"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","sa","");
    jdbc=new JdbcTemplate(ds);
    jdbc.execute("CREATE DOMAIN TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
    jdbc.execute("CREATE TABLE wallets(id UUID PRIMARY KEY)");
    jdbc.execute("CREATE TABLE media_files(id UUID PRIMARY KEY)");
    jdbc.execute(new String(getClass().getResourceAsStream("/db/migration/V20__create_wallet_receive_codes.sql").readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
    jdbc.update("INSERT INTO wallets(id) VALUES (?)",wallet);
    var factory=new ProxyFactory(new JdbcReceiveCodeRepository(jdbc));
    factory.setProxyTargetClass(true);
    factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(ds),new AnnotationTransactionAttributeSource()));
    codes=(ReceiveCodeRepository)factory.getProxy();
  }
  UUID media() { UUID id=UUID.randomUUID(); jdbc.update("INSERT INTO media_files(id) VALUES (?)",id); return id; }
  @Test public void reservationAndAttachmentSurviveCallerRollback() {
    UUID image=media();
    new TransactionTemplate(new DataSourceTransactionManager(ds)).executeWithoutResult(tx->{
      assertEquals(token,codes.reserve(wallet,token).token());
      assertTrue(codes.attach(wallet,null,image));
      tx.setRollbackOnly();
    });
    assertEquals(image,codes.findByWallet(wallet).orElseThrow().mediaId());
    assertEquals(wallet,codes.findByToken(token).orElseThrow().walletId());
  }
  @Test public void reservationRaceReturnsSameDurableToken() throws Exception {
    var start=new CountDownLatch(1);
    try(var pool=Executors.newFixedThreadPool(2)) {
      Callable<ReceiveCode> first=()->{start.await();return codes.reserve(wallet,token);};
      Callable<ReceiveCode> second=()->{start.await();return codes.reserve(wallet,"87654321-4321-4321-8321-cba987654321");};
      var a=pool.submit(first); var b=pool.submit(second); start.countDown();
      assertEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
      assertEquals(Integer.valueOf(1),jdbc.queryForObject("SELECT COUNT(*) FROM wallet_receive_codes",Integer.class));
    }
  }
  @Test public void compareAndSetHasOneWinnerAndCanReplaceDeletedImageReference() throws Exception {
    codes.reserve(wallet,token);
    UUID a=media(),b=media(); var start=new CountDownLatch(1);
    try(var pool=Executors.newFixedThreadPool(2)) {
      var x=pool.submit(()->{start.await();return codes.attach(wallet,null,a);});
      var y=pool.submit(()->{start.await();return codes.attach(wallet,null,b);});
      start.countDown();
      assertNotEquals(x.get(15,TimeUnit.SECONDS),y.get(15,TimeUnit.SECONDS));
    }
    UUID winner=codes.findByWallet(wallet).orElseThrow().mediaId();
    UUID next=media();
    assertFalse(codes.attach(wallet,null,next));
    assertTrue(codes.attach(wallet,winner,next));
    assertEquals(token,codes.findByWallet(wallet).orElseThrow().token());
    assertEquals(next,codes.findByToken(token).orElseThrow().mediaId());
  }
  @Test public void duplicateTokenCannotPointToAnotherWallet() {
    codes.reserve(wallet,token);
    UUID other=UUID.randomUUID();jdbc.update("INSERT INTO wallets(id) VALUES (?)",other);
    assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->codes.reserve(other,token));
    assertEquals(wallet,codes.findByToken(token).orElseThrow().walletId());
    assertTrue(codes.findByWallet(other).isEmpty());
  }
}
