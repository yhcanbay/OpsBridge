package com.yhcanbay.opsbridge.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.yhcanbay.opsbridge.enums.Capability;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_profiles")
@Getter
@Setter
@NoArgsConstructor
public class ProjectProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 500)
    private String repositoryUrl;

    @Column(nullable = false, length = 30)
    private String environment;

    @Column(length = 100)
    private String jenkinsBaseUrlRef;

    @Column(length = 200)
    private String jenkinsJobPath;

    @Column(length = 250)
    private String containerLabelSelector;

    @Column(length = 500)
    private String healthUrl;

    @ElementCollection
    @CollectionTable(name = "project_capabilities", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "capability", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Capability> capabilities = new HashSet<>();

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    @PrePersist
    private void beforeInsert() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    private void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public Set<Capability> getCapabilities() {
        return Set.copyOf(capabilities);
    }

    public void setCapabilities(Set<Capability> capabilities) {
        this.capabilities.clear();
        this.capabilities.addAll(capabilities);
    }
}
