package com.devtracker.entities;

import com.devtracker.support.EmailNormalizer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Column(nullable = false)
    private String name;

    @Id
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)
    private String phoneNumber;

    private boolean emailVerified;

    @Column(nullable = true)
    private String password;

    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Providers provider = Providers.LOCAL;

    private String providerUserId;

    private String profilePic;

    @ElementCollection(fetch = FetchType.EAGER)
    @Builder.Default
    private List<String> roleList = new ArrayList<>(List.of("ROLE_USER"));

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Problem> problems = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void normalizeEmailAndDefaults() {
        if (this.email != null) {
            this.email = EmailNormalizer.normalize(this.email);
        }
        if (this.roleList == null || this.roleList.isEmpty()) {
            this.roleList = new ArrayList<>(List.of("ROLE_USER"));
        }
        if (this.provider == null) {
            this.provider = Providers.LOCAL;
        }
    }

    public String getId() {
        return this.email;
    }

    public void setId(String id) {
        this.email = id;
    }

    public String getProviderId() {
        return this.providerUserId;
    }

    public void setProviderId(String providerId) {
        this.providerUserId = providerId;
    }

    public List<String> getRoles() {
        return getRoleList();
    }

    public void setRoles(List<String> roles) {
        this.roleList = roles != null ? new ArrayList<>(roles) : new ArrayList<>(List.of("ROLE_USER"));
    }

    public List<String> getRoleList() {
        if (this.roleList == null) {
            this.roleList = new ArrayList<>(List.of("ROLE_USER"));
        } else if (this.roleList.isEmpty()) {
            this.roleList.add("ROLE_USER");
        }
        return this.roleList;
    }

    public String getPicture() {
        return this.profilePic;
    }

    public void setPicture(String picture) {
        this.profilePic = picture;
    }
}
