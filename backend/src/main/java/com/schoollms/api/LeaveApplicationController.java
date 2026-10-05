package com.schoollms.api;

import com.schoollms.domain.*;
import com.schoollms.repository.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/leave-requests")
@Transactional
public class LeaveApplicationController {
  private final LeaveApplicationRepository applications;
  private final StudentRepository students;
  private final NotificationRepository notifications;
  private final TeacherClassRepository teacherClasses;

  public LeaveApplicationController(
      LeaveApplicationRepository applications,
      StudentRepository students,
      NotificationRepository notifications,
      TeacherClassRepository teacherClasses) {
    this.applications = applications;
    this.students = students;
    this.notifications = notifications;
    this.teacherClasses = teacherClasses;
  }

  @GetMapping
  public List<LeaveApplicationView> list(@AuthenticationPrincipal Account user) {
    List<LeaveApplication> rows = applications.findAllByOrderByCreatedAtDesc();
    if (user.getRole() == Role.TEACHER) {
      rows = rows.stream()
          .filter(application -> application.getRequester().getId().equals(user.getId())
              || isAssignedStudentLeave(application, user))
          .toList();
    } else if (user.getRole() != Role.ADMIN) {
      rows = applications.findByRequester_EmailOrderByCreatedAtDesc(user.getEmail());
    }
    return rows.stream().map(LeaveApplicationView::from).toList();
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER','PARENT','STUDENT')")
  @ResponseStatus(HttpStatus.CREATED)
  public LeaveApplicationView create(
      @Valid @RequestBody LeaveApplicationRequest request,
      @AuthenticationPrincipal Account user) {
    validateDateRange(request);
    Student student = resolveStudent(request.studentId(), user);
    return LeaveApplicationView.from(
        applications.save(
            new LeaveApplication(
                user, student, request.startDate(), request.endDate(), request.reason().trim())));
  }

  @PutMapping("/{id}")
  public LeaveApplicationView update(
      @PathVariable Long id,
      @Valid @RequestBody LeaveApplicationRequest request,
      @AuthenticationPrincipal Account user) {
    validateDateRange(request);
    LeaveApplication application = findApplication(id);
    boolean isAdmin = user.getRole() == Role.ADMIN;
    if (!isAdmin
        && (!application.getRequester().getId().equals(user.getId())
            || application.getStatus() != LeaveStatus.PENDING)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Only your pending leave requests can be edited");
    }
    application.update(
        resolveStudent(request.studentId(), user),
        request.startDate(),
        request.endDate(),
        request.reason().trim());
    return LeaveApplicationView.from(application);
  }

  @PatchMapping("/{id}/status")
  public LeaveApplicationView updateStatus(
      @PathVariable Long id,
      @Valid @RequestBody LeaveStatusRequest request,
      @AuthenticationPrincipal Account user) {
    LeaveApplication application = findApplication(id);
    boolean isAdmin = user.getRole() == Role.ADMIN;
    boolean isTeacherReviewingStudentLeave = user.getRole() == Role.TEACHER
        && isAssignedStudentLeave(application, user);
    if (!isAdmin && !isTeacherReviewingStudentLeave) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Only admins can review staff leave; teachers may review student leave");
    }
    application.setReviewerRemark(request.remark());
    application.setStatus(request.status());
    notifications.save(
        new Notification(
            application.getRequester(),
            "Leave request " + request.status().name().toLowerCase(),
            "Your leave request for "
                + application.getStartDate()
                + " to "
                + application.getEndDate()
                + " was "
                + request.status().name().toLowerCase()
                + "."));
    return LeaveApplicationView.from(application);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id, @AuthenticationPrincipal Account user) {
    LeaveApplication application = findApplication(id);
    boolean isAdmin = user.getRole() == Role.ADMIN;
    boolean isOwner = application.getRequester().getId().equals(user.getId());
    if (!isAdmin && (!isOwner || application.getStatus() != LeaveStatus.PENDING)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Only your pending leave requests can be deleted");
    }
    applications.delete(application);
  }

  private Student resolveStudent(Long studentId, Account user) {
    if (user.getRole() == Role.TEACHER) {
      if (studentId != null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Teacher leave requests cannot be linked to a student");
      }
      return null;
    }
    if (studentId == null) {
      if (user.getRole() == Role.ADMIN) return null;
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a student for this request");
    }
    Student student =
        students
            .findById(studentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    boolean allowed =
        switch (user.getRole()) {
          case ADMIN -> true;
          case PARENT -> student.getParent() != null && student.getParent().getId().equals(user.getId());
          case STUDENT ->
              student.getStudentAccount() != null
                  && student.getStudentAccount().getId().equals(user.getId());
          case TEACHER -> false;
        };
    if (!allowed) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You can only request leave for your linked student account");
    }
    return student;
  }

  private boolean isAssignedStudentLeave(LeaveApplication application, Account teacher) {
    Student student = application.getStudent();
    return student != null
        && teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
            teacher.getEmail(), student.getClassName(), student.getDivision());
  }

  private void validateDateRange(LeaveApplicationRequest request) {
    if (request.endDate().isBefore(request.startDate())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
    }
  }

  private LeaveApplication findApplication(Long id) {
    return applications
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
  }
}
