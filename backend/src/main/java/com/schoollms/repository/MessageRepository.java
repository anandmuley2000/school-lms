package com.schoollms.repository;

import com.schoollms.domain.SchoolMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<SchoolMessage, Long> {
  List<SchoolMessage> findBySender_EmailOrRecipient_EmailOrderBySentAtDesc(
      String sender, String recipient);

  boolean existsBySender_IdAndRecipient_IdAndSubjectAndBody(
      Long senderId, Long recipientId, String subject, String body);
}
