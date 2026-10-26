package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.InboxEventEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataInboxEventRepository
        extends JpaRepository<InboxEventEntity, InboxEventEntity.Key> {}
