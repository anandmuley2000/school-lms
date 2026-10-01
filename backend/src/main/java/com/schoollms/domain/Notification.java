package com.schoollms.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Account recipient;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, length = 2000)
  private String message;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "is_read", nullable = false)
  private boolean read = false;

  protected Notification() {}

  public Notification(Account recipient, String title, String message) {
    this.recipient = recipient;
    this.title = title;
    this.message = message;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getMessage() {
    return message;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public boolean isRead() {
    return read;
  }

  public void markRead() {
    read = true;
  }

  public String getRecipientEmail() {
    return recipient.getEmail();
  }
}
