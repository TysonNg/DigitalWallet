package com.tyson.digitalwallet.ddd.application.usecase.media;

import com.tyson.digitalwallet.ddd.domain.model.entity.MediaFile;
import java.util.UUID;

public interface StoreGeneratedImageUseCase {
  MediaFile storeGeneratedImage(UUID owner, String name, byte[] bytes);
}
