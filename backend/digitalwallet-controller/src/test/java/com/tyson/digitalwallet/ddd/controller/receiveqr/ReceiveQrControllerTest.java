package com.tyson.digitalwallet.ddd.controller.receiveqr;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import com.tyson.digitalwallet.ddd.application.receiveqr.ReceiveQrService;
import com.tyson.digitalwallet.ddd.application.exception.*;
import com.tyson.digitalwallet.ddd.application.media.MediaException;
import com.tyson.digitalwallet.ddd.controller.exception.*;
import com.tyson.digitalwallet.ddd.controller.resource.ReceiveQrController;
import java.util.UUID;
import org.junit.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReceiveQrControllerTest {
  ReceiveQrService service=mock(ReceiveQrService.class);
  MockMvc mvc;
  UUID caller=UUID.randomUUID(),id=UUID.randomUUID();
  String token="12345678-1234-4234-8234-123456789abc";
  @Before public void setup() {
    mvc=MockMvcBuilders.standaloneSetup(new ReceiveQrController(service))
      .setControllerAdvice(new MediaExceptionHandler(),new GlobalExceptionHandler()).build();
  }
  @Test public void bothRoutesAndAliasesRequireAuthentication() throws Exception {
    for(String prefix:new String[]{"/wallet","/api/v1/wallet"}) {
      assertEquals(401,mvc.perform(post(prefix+"/receive-qr")).andReturn().getResponse().getStatus());
      assertEquals(401,mvc.perform(get(prefix+"/receive-qr/"+token+"/recipient")).andReturn().getResponse().getStatus());
    }
    verifyNoInteractions(service);
  }
  @Test public void invalidPrincipalRejected() throws Exception {
    assertEquals(401,mvc.perform(post("/wallet/receive-qr").principal(()->"invalid")).andReturn().getResponse().getStatus());
    verifyNoInteractions(service);
  }
  @Test public void createReturnsOnlyThreePublicFields() throws Exception {
    when(service.getOrCreate(caller)).thenReturn(new ReceiveQrService.Qr(id,"/api/v1/media/"+id+"/content","https://wallet.example/vi/receive/"+token));
    for(String prefix:new String[]{"/wallet","/api/v1/wallet"}) {
      var res=mvc.perform(post(prefix+"/receive-qr").principal(()->caller.toString())).andReturn().getResponse();
      assertEquals(200,res.getStatus());
      var data=new ObjectMapper().readTree(res.getContentAsString()).get("data");
      assertEquals(3,data.size()); assertEquals(id.toString(),data.get("mediaId").asText());
      assertEquals("/api/v1/media/"+id+"/content",data.get("contentUrl").asText());
      assertEquals("https://wallet.example/vi/receive/"+token,data.get("receiveUrl").asText());
    }
  }
  @Test public void resolverProjectsExactlyWalletNameAndCurrency() throws Exception {
    when(service.resolve(caller,token)).thenReturn(new ReceiveQrService.Recipient(id,"Name","VND"));
    var res=mvc.perform(get("/wallet/receive-qr/"+token+"/recipient").principal(()->caller.toString())).andReturn().getResponse();
    assertEquals(200,res.getStatus());
    var data=new ObjectMapper().readTree(res.getContentAsString()).get("data");
    assertEquals(3,data.size());assertEquals(id.toString(),data.get("walletId").asText());
    assertEquals("Name",data.get("fullName").asText());assertEquals("VND",data.get("currency").asText());
    verify(service).resolve(caller,token);
  }
  @Test public void failuresAreSanitizedEvenWithLeakyGlobalHandler() throws Exception {
    for(RuntimeException failure:new RuntimeException[]{new IllegalStateException("secret-key"),
        new org.springframework.dao.DataAccessResourceFailureException("secret-host"),
        new org.springframework.transaction.TransactionSystemException("secret-commit"),MediaException.unavailable()}) {
      doThrow(failure).when(service).getOrCreate(caller);
      var res=mvc.perform(post("/wallet/receive-qr").principal(()->caller.toString())).andReturn().getResponse();
      assertEquals(failure instanceof IllegalStateException?500:503,res.getStatus());
      assertFalse(res.getContentAsString().contains("secret-"));
    }
  }
  @Test public void policyErrorsKeepExistingStatusAndCode() throws Exception {
    when(service.resolve(caller,token)).thenThrow(new AppException(ErrorCode.CANNOT_TRANSFER_TO_SELF,"Cannot transfer money to yourself"));
    var res=mvc.perform(get("/wallet/receive-qr/"+token+"/recipient").principal(()->caller.toString())).andReturn().getResponse();
    assertEquals(400,res.getStatus());assertTrue(res.getContentAsString().contains("CANNOT_TRANSFER_TO_SELF"));
    doThrow(new MediaException(404,"RECEIVE_QR_NOT_FOUND","Receive QR not found")).when(service).resolve(caller,token);
    assertEquals(404,mvc.perform(get("/wallet/receive-qr/"+token+"/recipient").principal(()->caller.toString())).andReturn().getResponse().getStatus());
  }
}
