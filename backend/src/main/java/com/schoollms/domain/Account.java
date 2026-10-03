package com.schoollms.domain;

import jakarta.persistence.*;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "accounts")
public class Account implements UserDetails {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(nullable = false)
  private String name;

  private String phone;
  private boolean active = true;

  protected Account() {}

  public Account(String email, String passwordHash, Role role, String name, String phone) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
    this.name = name;
    this.phone = phone;
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }

  public String getPhone() {
    return phone;
  }

  public Role getRole() {
    return role;
  }

  public void updateProfile(String name, String phone, Role role) {
    this.name = name;
    this.phone = phone;
    this.role = role;
  }

  public void changePassword(String encodedPassword) {
    this.passwordHash = encodedPassword;
  }

  public void synchronizeDemoCredentials(String encodedPassword, Role expectedRole) {
    this.passwordHash = encodedPassword;
    this.role = expectedRole;
    this.active = true;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return active;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }
}
