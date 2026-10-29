package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.media.ImageValidator;
import com.tyson.digitalwallet.ddd.application.media.MediaException;
import com.tyson.digitalwallet.ddd.application.media.MediaService;
import com.tyson.digitalwallet.ddd.application.usecase.media.DeleteMediaUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.media.GetMediaContentUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.media.GetMediaMetadataUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.media.UploadMediaUseCase;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.MediaFile;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/media", "/api/v1/media"})
@ConditionalOnProperty(name = "media.enabled", havingValue = "true")
public class MediaController {
  private final UploadMediaUseCase uploadMediaUseCase;
  private final GetMediaMetadataUseCase getMediaMetadataUseCase;
  private final GetMediaContentUseCase getMediaContentUseCase;
  private final DeleteMediaUseCase deleteMediaUseCase;

  public MediaController(
      UploadMediaUseCase uploadMediaUseCase,
      GetMediaMetadataUseCase getMediaMetadataUseCase,
      GetMediaContentUseCase getMediaContentUseCase,
      DeleteMediaUseCase deleteMediaUseCase) {
    this.uploadMediaUseCase = uploadMediaUseCase;
    this.getMediaMetadataUseCase = getMediaMetadataUseCase;
    this.getMediaContentUseCase = getMediaContentUseCase;
    this.deleteMediaUseCase = deleteMediaUseCase;
  }

  public MediaController(MediaService media) {
    this(media, media, media, media);
  }

  public record Metadata(
      UUID id,
      String originalName,
      String contentType,
      long sizeBytes,
      Instant createdAt,
      String contentUrl) {
    static Metadata from(MediaFile f) {
      return new Metadata(
          f.id(),
          f.originalName(),
          f.contentType(),
          f.sizeBytes(),
          f.createdAt(),
          "/api/v1/media/" + f.id() + "/content");
    }
  }

  private UUID owner(Principal principal) {
    if (principal == null)
      throw new MediaException(401, "UNAUTHORIZED", "Authentication is required");
    try {
      return UUID.fromString(principal.getName());
    } catch (IllegalArgumentException ex) {
      throw new MediaException(401, "UNAUTHORIZED", "Invalid authenticated user");
    }
  }

  @PostMapping(consumes = "multipart/form-data")
  public ResponseEntity<ApiResponse<Metadata>> upload(
      @RequestParam("file") MultipartFile file, Principal principal) throws IOException {
    UUID user = owner(principal);
    if (file.getSize() > ImageValidator.MAX_BYTES)
      throw new MediaException(413, "MEDIA_TOO_LARGE", "Maximum file size is 5 MiB");
    byte[] bytes;
    try (var input = file.getInputStream()) {
      bytes = input.readNBytes(ImageValidator.MAX_BYTES + 1);
    }
    return ApiResponse.created(
        "Media uploaded", Metadata.from(uploadMediaUseCase.upload(user, file.getOriginalFilename(), bytes)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<Metadata>> metadata(
      @PathVariable("id") UUID id, Principal principal) {
    return ApiResponse.ok("Media retrieved", Metadata.from(getMediaMetadataUseCase.metadata(owner(principal), id)));
  }

  @GetMapping("/{id}/content")
  public void content(
      @PathVariable("id") UUID id, Principal principal, HttpServletResponse response)
      throws IOException {
    UUID user = owner(principal);
    var file = getMediaMetadataUseCase.metadata(user, id);
    try (var input = getMediaContentUseCase.open(user, id)) {
      response.setContentType(file.contentType());
      response.setContentLengthLong(file.sizeBytes());
      response.setHeader("Cache-Control", "private, no-store");
      response.setHeader("X-Content-Type-Options", "nosniff");
      input.transferTo(response.getOutputStream());
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") UUID id, Principal principal) {
    deleteMediaUseCase.delete(owner(principal), id);
    return ResponseEntity.noContent().build();
  }
}
