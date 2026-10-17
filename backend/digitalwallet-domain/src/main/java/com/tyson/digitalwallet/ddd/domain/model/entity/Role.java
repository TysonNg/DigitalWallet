package com.tyson.digitalwallet.ddd.domain.model.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Role {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private Set<Authority> authorities = new HashSet<>();

    public Role() {
    }

    public Role(Long id, String name, String description, LocalDateTime createdAt) {
        this(id, name, description, createdAt, new HashSet<>());
    }

    public Role(Long id, String name, String description, LocalDateTime createdAt, Set<Authority> authorities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.authorities = authorities != null ? new HashSet<>(authorities) : new HashSet<>();
    }

    public static Role of(String name, String description) {
        return new Role(null, name, description, LocalDateTime.now(), new HashSet<>());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<Authority> getAuthorities() {
        return authorities != null ? authorities : new HashSet<>();
    }

    public void setAuthorities(Set<Authority> authorities) {
        this.authorities = authorities != null ? new HashSet<>(authorities) : new HashSet<>();
    }

    public void addAuthority(Authority authority) {
        if (this.authorities == null) {
            this.authorities = new HashSet<>();
        }
        if (authority != null) {
            this.authorities.add(authority);
        }
    }

    public void removeAuthority(String authorityName) {
        if (this.authorities != null && authorityName != null) {
            this.authorities.removeIf(a -> authorityName.equalsIgnoreCase(a.getName()));
        }
    }

    public boolean hasAuthority(String authorityName) {
        if (this.authorities == null || authorityName == null) {
            return false;
        }
        return this.authorities.stream().anyMatch(a -> authorityName.equalsIgnoreCase(a.getName()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(name, role.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
