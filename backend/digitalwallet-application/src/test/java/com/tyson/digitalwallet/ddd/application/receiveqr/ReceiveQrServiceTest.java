package com.tyson.digitalwallet.ddd.application.receiveqr;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.tyson.digitalwallet.ddd.application.media.*;
import com.tyson.digitalwallet.ddd.application.exception.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.LookupRecipientUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.RecipientLookupResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.MediaFile;
import com.tyson.digitalwallet.ddd.domain.model.entity.ReceiveCode;
import com.tyson.digitalwallet.ddd.domain.model.entity.*;
import com.tyson.digitalwallet.ddd.domain.model.enums.*;
import com.tyson.digitalwallet.ddd.domain.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.*;

public class ReceiveQrServiceTest {
  UUID owner=UUID.randomUUID(), walletId=UUID.randomUUID(), reader=UUID.randomUUID();
  String token="12345678-1234-4234-8234-123456789abc";
  Wallet wallet=new Wallet(walletId,owner,BigDecimal.ZERO,"VND",WalletStatus.ACTIVE,null,null);
  User user=User.create("Current Name","private@email","secret","0901234567",LocalDate.of(1990,1,1));
  WalletRepository wallets=mock(WalletRepository.class);
  UserRepository users=mock(UserRepository.class);
  ReceiveCodeRepository codes=mock(ReceiveCodeRepository.class);
  MediaService media=mock(MediaService.class);
  ReceiveQrRenderer renderer=mock(ReceiveQrRenderer.class);
  LookupRecipientUseCase lookup=mock(LookupRecipientUseCase.class);
  ReceiveQrService service;
  MediaFile image(UUID id) { return new MediaFile(id,owner,"private","secret/key","receive.png","image/png",3,
      MediaFile.Status.READY,Instant.now(),Instant.now()); }
  @Before public void setup() {
    service=new ReceiveQrService(codes,media,renderer,wallets,users,lookup,"https://wallet.example/");
    when(wallets.findByUserId(owner)).thenReturn(Optional.of(wallet));
    when(wallets.findById(walletId)).thenReturn(Optional.of(wallet));
    when(users.findById(owner)).thenReturn(Optional.of(user));
    when(codes.reserve(eq(walletId),anyString())).thenReturn(new ReceiveCode(walletId,token,null));
    when(codes.findByToken(token)).thenReturn(Optional.of(new ReceiveCode(walletId,token,null)));
  }
  @Test public void createsThenReusesStableTokenAndImage() {
    UUID id=UUID.randomUUID(); byte[] png={1,2,3};
    when(renderer.render("https://wallet.example/vi/receive/"+token)).thenReturn(png);
    when(media.storeGeneratedImage(owner,"receive-qr.png",png)).thenReturn(image(id));
    when(codes.attach(walletId,null,id)).thenReturn(true);
    var first=service.getOrCreate(owner);
    assertEquals(id,first.mediaId());
    assertEquals("/api/v1/media/"+id+"/content",first.contentUrl());
    assertEquals("https://wallet.example/vi/receive/"+token,first.receiveUrl());
    when(codes.reserve(eq(walletId),anyString())).thenReturn(new ReceiveCode(walletId,token,id));
    when(media.metadata(owner,id)).thenReturn(image(id));
    assertEquals(first,service.getOrCreate(owner));
    verify(media,times(1)).storeGeneratedImage(eq(owner),anyString(),any());
    verify(codes,times(2)).reserve(eq(walletId),matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
  }
  @Test public void deletedImageRegeneratesWithoutChangingToken() {
    UUID old=UUID.randomUUID(), next=UUID.randomUUID();
    when(codes.reserve(eq(walletId),anyString())).thenReturn(new ReceiveCode(walletId,token,old));
    when(media.metadata(owner,old)).thenThrow(MediaException.missing());
    when(renderer.render(anyString())).thenReturn(new byte[]{1});
    when(media.storeGeneratedImage(eq(owner),anyString(),any())).thenReturn(image(next));
    when(codes.attach(walletId,old,next)).thenReturn(true);
    assertEquals(next,service.getOrCreate(owner).mediaId());
  }
  @Test public void storageOutageDoesNotReplaceExistingImage() {
    UUID id=UUID.randomUUID();
    when(codes.reserve(eq(walletId),anyString())).thenReturn(new ReceiveCode(walletId,token,id));
    when(media.metadata(owner,id)).thenThrow(MediaException.unavailable());
    assertThrows(MediaException.class,()->service.getOrCreate(owner));
    verifyNoInteractions(renderer);
  }
  @Test public void concurrentAttachmentDeletesOnlyLosingGeneratedImage() {
    UUID loser=UUID.randomUUID(), winner=UUID.randomUUID();
    when(renderer.render(anyString())).thenReturn(new byte[]{1});
    when(media.storeGeneratedImage(eq(owner),anyString(),any())).thenReturn(image(loser));
    when(codes.attach(walletId,null,loser)).thenReturn(false);
    when(codes.findByWallet(walletId)).thenReturn(Optional.of(new ReceiveCode(walletId,token,winner)));
    when(media.metadata(owner,winner)).thenReturn(image(winner));
    assertEquals(winner,service.getOrCreate(owner).mediaId());
    verify(media).delete(owner,loser);
    verify(media,never()).delete(owner,winner);
  }
  @Test public void unknownAttachmentCommitPreservesGeneratedBytesEvenIfRereadFails() {
    UUID generated=UUID.randomUUID();
    when(renderer.render(anyString())).thenReturn(new byte[]{1});
    when(media.storeGeneratedImage(eq(owner),anyString(),any())).thenReturn(image(generated));
    when(codes.attach(walletId,null,generated)).thenThrow(new IllegalStateException("unknown commit"));
    when(codes.findByWallet(walletId)).thenThrow(new IllegalStateException("db down"));
    assertThrows(RuntimeException.class,()->service.getOrCreate(owner));
    verify(media,never()).delete(any(),any());
  }
  @Test public void inactiveOwnerCannotGenerate() {
    wallet.frozen();
    assertEquals(ErrorCode.WALLET_NOT_ACTIVE,assertThrows(AppException.class,()->service.getOrCreate(owner)).getErrorCode());
    verifyNoInteractions(codes,media,renderer);
  }
  @Test public void missingWalletCannotGenerate() {
    when(wallets.findByUserId(owner)).thenReturn(Optional.empty());
    assertThrows(WalletNotFoundException.class,()->service.getOrCreate(owner));
    verifyNoInteractions(codes,media,renderer);
  }
  @Test public void resolvesThroughExistingLookupAndProjectsCurrentPublicFieldsOnly() {
    when(lookup.lookup(reader,walletId.toString())).thenReturn(new RecipientLookupResponse(owner,walletId,"Cached Name","secret-phone","secret-email","USD"));
    var recipient=service.resolve(reader,token);
    assertEquals(walletId,recipient.walletId());
    assertEquals("Current Name",recipient.fullName());
    assertEquals("VND",recipient.currency());
    verify(lookup).lookup(reader,walletId.toString());
  }
  @Test public void frozenRecipientRejectedBeforeCachedLookup() {
    wallet.frozen();
    assertEquals(ErrorCode.WALLET_NOT_ACTIVE,assertThrows(AppException.class,()->service.resolve(reader,token)).getErrorCode());
    verifyNoInteractions(lookup);
  }
  @Test public void suspendedRecipientRejectedBeforeCachedLookup() {
    user.suspend();
    assertThrows(UserNotActiveException.class,()->service.resolve(reader,token));
    verifyNoInteractions(lookup);
  }
  @Test public void deletedRecipientRejectedBeforeCachedLookup() {
    when(users.findById(owner)).thenReturn(Optional.empty());
    assertThrows(UserNotFoundException.class,()->service.resolve(reader,token));
    verifyNoInteractions(lookup);
  }
  @Test public void selfCheckDelegatesToExistingLookup() {
    when(lookup.lookup(owner,walletId.toString())).thenThrow(new AppException(ErrorCode.CANNOT_TRANSFER_TO_SELF,"Cannot transfer money to yourself"));
    assertEquals(ErrorCode.CANNOT_TRANSFER_TO_SELF,assertThrows(AppException.class,()->service.resolve(owner,token)).getErrorCode());
  }
  @Test public void unknownAndMalformedTokensRejectWithoutLookup() {
    when(codes.findByToken(token)).thenReturn(Optional.empty());
    assertEquals(404,assertThrows(MediaException.class,()->service.resolve(reader,token)).status());
    assertEquals(404,assertThrows(MediaException.class,()->service.resolve(reader,"bad-token")).status());
    verifyNoInteractions(lookup);
  }
  @Test public void bothOperationsRequireAuthentication() {
    assertEquals(401,assertThrows(MediaException.class,()->service.getOrCreate(null)).status());
    assertEquals(401,assertThrows(MediaException.class,()->service.resolve(null,token)).status());
    verifyNoInteractions(codes,media,lookup);
  }
}
