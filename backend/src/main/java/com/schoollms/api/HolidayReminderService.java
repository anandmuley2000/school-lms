package com.schoollms.api;

import com.schoollms.domain.Account;
import com.schoollms.domain.Notification;
import com.schoollms.domain.Role;
import com.schoollms.repository.AccountRepository;
import com.schoollms.repository.NotificationRepository;
import com.schoollms.repository.SchoolEventRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HolidayReminderService {
  private static final String TITLE = "School holiday tomorrow";
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy");

  private final SchoolEventRepository events;
  private final AccountRepository accounts;
  private final NotificationRepository notifications;
  private final ZoneId schoolZone;

  public HolidayReminderService(
      SchoolEventRepository events,
      AccountRepository accounts,
      NotificationRepository notifications,
      @Value("${app.school.time-zone:Asia/Kolkata}") String timeZone) {
    this.events = events;
    this.accounts = accounts;
    this.notifications = notifications;
    this.schoolZone = ZoneId.of(timeZone);
  }

  @Scheduled(
      cron = "${app.holidays.reminder-cron:0 0 8 * * *}",
      zone = "${app.school.time-zone:Asia/Kolkata}")
  @Transactional
  public void notifyAboutTomorrowHolidays() {
    LocalDate tomorrow = LocalDate.now(schoolZone).plusDays(1);
    List<Account> recipients =
        accounts.findByRoleInAndActiveTrue(List.of(Role.PARENT, Role.TEACHER));

    events.findAllByOrderByEventDateAsc().stream()
        .filter(event -> tomorrow.equals(event.getEventDate()))
        .filter(event -> HolidayClassifier.isHoliday(event.getTitle(), event.getDescription()))
        .forEach(
            event -> {
              String message =
                  "Tomorrow (" + DATE_FORMAT.format(tomorrow) + ") is a school holiday: "
                      + event.getTitle() + ".";
              for (Account recipient : recipients) {
                if (!notifications.existsByRecipient_IdAndMessage(recipient.getId(), message)) {
                  notifications.save(new Notification(recipient, TITLE, message));
                }
              }
            });
  }
}
