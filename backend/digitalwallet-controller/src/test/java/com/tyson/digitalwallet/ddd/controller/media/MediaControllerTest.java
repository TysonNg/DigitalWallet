package com.tyson.digitalwallet.ddd.controller.media;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.tyson.digitalwallet.ddd.application.media.*;
import com.tyson.digitalwallet.ddd.controller.exception.MediaExceptionHandler;
import com.tyson.digitalwallet.ddd.controller.resource.MediaController;
import com.tyson.digitalwallet.ddd.domain.model.entity.MediaFile;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;
import org.junit.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class MediaControllerTest {
  MediaService service = mock(MediaService.class);
  MockMvc mvc;
  UUID owner = UUID.randomUUID(), id = UUID.randomUUID();
  MediaFile file =
      new MediaFile(
          id,
          owner,
          "private",
          "hidden/key",
          "photo.png",
          "image/png",
          3,
          MediaFile.Status.READY,
          Instant.now(),
          Instant.now());

  @Before
  public void setup() {
    mvc =
        MockMvcBuilders.standaloneSetup(new MediaController(service))
            .setControllerAdvice(new MediaExceptionHandler())
            .build();
  }

  @Test
  public void authenticationRequired() throws Exception {
    assertEquals(401, mvc.perform(get("/media/" + id)).andReturn().getResponse().getStatus());
    verifyNoInteractions(service);
  }

  @Test
  public void uploadReturnsOnlyPublicMetadata() throws Exception {
    when(service.upload(eq(owner), any(), any())).thenReturn(file);
    var result =
        mvc.perform(
                multipart("/media")
                    .file(
                        new MockMultipartFile(
                            "file", "pretend.jpg", "text/plain", new byte[] {1, 2, 3}))
                    .principal(() -> owner.toString()))
            .andReturn()
            .getResponse();
    assertEquals(201, result.getStatus());
    assertTrue(result.getContentAsString().contains("/api/v1/media/"));
    assertFalse(result.getContentAsString().contains("hidden/key"));
    assertFalse(result.getContentAsString().contains("private"));
  }

  @Test
  public void streamsPrivateContent() throws Exception {
    when(service.metadata(owner, id)).thenReturn(file);
    when(service.open(owner, id)).thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));
    var response =
        mvc.perform(get("/media/" + id + "/content").principal(() -> owner.toString()))
            .andReturn()
            .getResponse();
    assertEquals(200, response.getStatus());
    assertArrayEquals(new byte[] {1, 2, 3}, response.getContentAsByteArray());
    assertEquals("private, no-store", response.getHeader("Cache-Control"));
    assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
  }

  @Test
  public void notFoundAndUnavailableUseEnvelope() throws Exception {
    when(service.metadata(owner, id)).thenThrow(MediaException.missing());
    assertEquals(
        404,
        mvc.perform(get("/media/" + id).principal(() -> owner.toString()))
            .andReturn()
            .getResponse()
            .getStatus());
    doThrow(MediaException.unavailable()).when(service).metadata(owner, id);
    assertEquals(
        503,
        mvc.perform(get("/media/" + id).principal(() -> owner.toString()))
            .andReturn()
            .getResponse()
            .getStatus());
  }

  @Test
  public void transactionFailureReturnsSanitized503() throws Exception {
    when(service.metadata(owner, id))
        .thenThrow(
            new org.springframework.transaction.CannotCreateTransactionException(
                "private-database-host"));
    var response =
        mvc.perform(get("/media/" + id).principal(() -> owner.toString()))
            .andReturn()
            .getResponse();
    assertEquals(503, response.getStatus());
    assertFalse(response.getContentAsString().contains("private-database-host"));
    assertTrue(response.getContentAsString().contains("MEDIA_UNAVAILABLE"));
  }

  @Test
  public void deletesWithoutResponseBody() throws Exception {
    var result =
        mvc.perform(delete("/media/" + id).principal(() -> owner.toString()))
            .andReturn()
            .getResponse();
    assertEquals(204, result.getStatus());
    verify(service).delete(owner, id);
  }
}
