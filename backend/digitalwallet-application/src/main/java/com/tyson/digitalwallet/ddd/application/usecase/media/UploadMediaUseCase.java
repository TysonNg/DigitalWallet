package com.tyson.digitalwallet.ddd.application.usecase.media;

import com.tyson.digitalwallet.ddd.domain.model.entity.MediaFile;
import java.util.UUID;

public interface UploadMediaUseCase {
  MediaFile upload(UUID owner, String originalName, byte[] bytes);
}
