package com.schoollms.repository;

import com.schoollms.domain.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByRecipient_EmailOrderByCreatedAtDesc(String email);
}
