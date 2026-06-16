package dev.sey.streamfolio.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false, length = 128)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_account_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Set<UserRole> roles = new LinkedHashSet<>();

    protected UserAccount() {
    }

    public UserAccount(String email, String displayName, String passwordHash) {
        this(email, displayName, passwordHash, Set.of(UserRole.USER));
    }

    public UserAccount(String email, String displayName, String passwordHash, Set<UserRole> roles) {
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        replaceRoles(roles);
    }

    public UserAccount addRole(UserRole role) {
        if (role != null) {
            this.roles.add(role);
        }
        if (this.roles.isEmpty()) {
            this.roles.add(UserRole.USER);
        }
        return this;
    }

    public boolean hasRole(UserRole role) {
        return this.roles.contains(role);
    }

    private void replaceRoles(Set<UserRole> roles) {
        this.roles.clear();
        if (roles != null) {
            roles.stream()
                .filter(Objects::nonNull)
                .forEach(this.roles::add);
        }
        if (this.roles.isEmpty()) {
            this.roles.add(UserRole.USER);
        }
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<UserRole> getRoles() {
        return Collections.unmodifiableSet(roles);
    }
}
