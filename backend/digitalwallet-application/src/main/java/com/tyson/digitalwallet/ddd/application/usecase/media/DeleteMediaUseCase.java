package com.tyson.digitalwallet.ddd.application.usecase.media;

import java.util.UUID;

public interface DeleteMediaUseCase {
  void delete(UUID owner, UUID id);
}
