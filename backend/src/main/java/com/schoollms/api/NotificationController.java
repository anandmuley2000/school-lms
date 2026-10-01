package com.schoollms.api;

import com.schoollms.domain.*;
import com.schoollms.repository.NotificationRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
@Transactional
public class NotificationController {
  private final NotificationRepository notifications;

  public NotificationController(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  @GetMapping
  public List<NotificationView> list(@AuthenticationPrincipal Account user) {
    return notifications.findByRecipient_EmailOrderByCreatedAtDesc(user.getEmail()).stream()
        .map(NotificationView::from)
        .toList();
  }

  @PatchMapping("/{id}/read")
  public NotificationView read(@PathVariable Long id, @AuthenticationPrincipal Account user) {
    Notification n =
        notifications
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    if (!n.getRecipientEmail().equals(user.getEmail()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
    n.markRead();
    return NotificationView.from(notifications.save(n));
  }
}
