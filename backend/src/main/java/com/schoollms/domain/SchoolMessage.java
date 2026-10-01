package com.schoollms.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

@Entity
@Table(name = "school_messages")
public class SchoolMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "sender_id")
  private Account sender;

  @ManyToOne(optional = false)
  @JoinColumn(name = "recipient_id")
  private Account recipient;

  @NotBlank private String subject;

  @Column(nullable = false, length = 5000)
  @NotBlank
  private String body;

  @Column(nullable = false)
  private Instant sentAt = Instant.now();

  @Column(name = "is_read", nullable = false)
  private boolean read = false;

  protected SchoolMessage() {}

  public SchoolMessage(Account sender, Account recipient, String subject, String body) {
    this.sender = sender;
    this.recipient = recipient;
    this.subject = subject;
    this.body = body;
  }

  public Long getId() {
    return id;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public boolean isRead() {
    return read;
  }

  public String getSenderEmail() {
    return sender.getEmail();
  }

  public String getSenderName() {
    return sender.getName();
  }

  public String getRecipientEmail() {
    return recipient.getEmail();
  }

  public String getRecipientName() {
    return recipient.getName();
  }

  public void markRead() {
    read = true;
  }
}
