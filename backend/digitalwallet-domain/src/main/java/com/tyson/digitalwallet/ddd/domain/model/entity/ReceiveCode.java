package com.tyson.digitalwallet.ddd.domain.model.entity;

import java.util.UUID;

/**
 * The token identifies a destination; it grants no access to its wallet or private media.
 */
public record ReceiveCode(UUID walletId, String token, UUID mediaId) {}
