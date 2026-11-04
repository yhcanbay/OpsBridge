package com.yhcanbay.opsbridge.entity;

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

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "project_profiles")
public class ProjectProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public ProjectProfile() {}

    @PrePersist
    private void beforeInsert() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    private void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getJenkinsBaseUrlRef() {
        return jenkinsBaseUrlRef;
    }

    public void setJenkinsBaseUrlRef(String jenkinsBaseUrlRef) {
        this.jenkinsBaseUrlRef = jenkinsBaseUrlRef;
    }

    public String getJenkinsJobPath() {
        return jenkinsJobPath;
    }

    public void setJenkinsJobPath(String jenkinsJobPath) {
        this.jenkinsJobPath = jenkinsJobPath;
    }

    public String getContainerLabelSelector() {
        return containerLabelSelector;
    }

    public void setContainerLabelSelector(String containerLabelSelector) {
        this.containerLabelSelector = containerLabelSelector;
    }

    public String getHealthUrl() {
        return healthUrl;
    }

    public void setHealthUrl(String healthUrl) {
        this.healthUrl = healthUrl;
    }

    public Set<Capability> getCapabilities() {
        return Set.copyOf(capabilities);
    }

    public void setCapabilities(Set<Capability> capabilities) {
        this.capabilities.clear();
        this.capabilities.addAll(capabilities);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
