package com.schoollms.api;

import com.schoollms.domain.*;
import com.schoollms.repository.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/messages")
@Transactional
public class MessageController {
  private final MessageRepository messages;
  private final AccountRepository accounts;
  private final StudentRepository students;
  private final TeacherClassRepository teacherClasses;
  private final NotificationRepository notifications;

  public MessageController(
      MessageRepository m,
      AccountRepository a,
      StudentRepository s,
      TeacherClassRepository t,
      NotificationRepository n) {
    messages = m;
    accounts = a;
    students = s;
    teacherClasses = t;
    notifications = n;
  }

  @GetMapping
  public List<MessageView> list(@AuthenticationPrincipal Account user) {
    return messages
        .findBySender_EmailOrRecipient_EmailOrderBySentAtDesc(user.getEmail(), user.getEmail())
        .stream()
        .map(MessageView::from)
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MessageView send(
      @Valid @RequestBody MessageRequest r, @AuthenticationPrincipal Account user) {
    Account recipient =
        accounts
            .findByEmail(r.recipientEmail())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Recipient account not found"));
    if (!canMessage(user, recipient))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Messaging is limited to your school relationships");
    SchoolMessage message =
        messages.save(new SchoolMessage(user, recipient, r.subject(), r.body()));
    notifications.save(
        new Notification(recipient, "New message from " + user.getName(), r.subject()));
    return MessageView.from(message);
  }

  @PatchMapping("/{id}/read")
  public MessageView read(@PathVariable Long id, @AuthenticationPrincipal Account user) {
    SchoolMessage m =
        messages
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
    if (!m.getRecipientEmail().equals(user.getEmail()))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Only the recipient can mark a message read");
    m.markRead();
    return MessageView.from(messages.save(m));
  }

  private boolean canMessage(Account from, Account to) {
    if (from.getRole() == Role.ADMIN) return true;
    if (from.getRole() == Role.PARENT && to.getRole() == Role.TEACHER)
      return students.findByParent_EmailOrderByClassNameAscRollNumberAsc(from.getEmail()).stream()
          .anyMatch(
              s ->
                  teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
                      to.getEmail(), s.getClassName(), s.getDivision()));
    if (from.getRole() == Role.STUDENT && to.getRole() == Role.TEACHER)
      return students.findByStudentAccount_Email(from.getEmail()).stream()
          .anyMatch(
              s ->
                  teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
                      to.getEmail(), s.getClassName(), s.getDivision()));
    if (from.getRole() == Role.TEACHER && to.getRole() == Role.PARENT)
      return students.findByParent_EmailOrderByClassNameAscRollNumberAsc(to.getEmail()).stream()
          .anyMatch(
              s ->
                  teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
                      from.getEmail(), s.getClassName(), s.getDivision()));
    return false;
  }
}
