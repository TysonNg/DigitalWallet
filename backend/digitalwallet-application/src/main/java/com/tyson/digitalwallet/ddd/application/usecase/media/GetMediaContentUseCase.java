package com.tyson.digitalwallet.ddd.application.usecase.media;

import java.io.InputStream;
import java.util.UUID;

public interface GetMediaContentUseCase {
  InputStream open(UUID owner, UUID id);
}
