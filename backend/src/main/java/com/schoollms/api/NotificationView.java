package com.schoollms.api;

import com.schoollms.domain.Notification;
import java.time.Instant;

public record NotificationView(
    Long id, String title, String message, Instant createdAt, boolean read) {
  public static NotificationView from(Notification n) {
    return new NotificationView(
        n.getId(), n.getTitle(), n.getMessage(), n.getCreatedAt(), n.isRead());
  }
}
