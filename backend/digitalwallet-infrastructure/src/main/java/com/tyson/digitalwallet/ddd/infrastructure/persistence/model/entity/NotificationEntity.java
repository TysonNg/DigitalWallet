package com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

    private UUID id;


    private UserEntity user;
}
