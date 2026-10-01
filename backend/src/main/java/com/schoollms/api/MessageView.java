package com.schoollms.api;

import com.schoollms.domain.SchoolMessage;
import java.time.Instant;

public record MessageView(
    Long id,
    String senderEmail,
    String senderName,
    String recipientEmail,
    String recipientName,
    String subject,
    String body,
    Instant sentAt,
    boolean read) {
  public static MessageView from(SchoolMessage m) {
    return new MessageView(
        m.getId(),
        m.getSenderEmail(),
        m.getSenderName(),
        m.getRecipientEmail(),
        m.getRecipientName(),
        m.getSubject(),
        m.getBody(),
        m.getSentAt(),
        m.isRead());
  }
}
