package com.schoollms.config;

import com.schoollms.domain.*;
import com.schoollms.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("development")
@ConditionalOnProperty(
    prefix = "app.demo", name = {"data-enabled", "legacy-data-enabled"}, havingValue = "true")
public class DemoData {
  @Bean
  CommandLineRunner demoDataRunner(
      AccountRepository accounts,
      StudentRepository students,
      HomeworkRepository homework,
      TeacherClassRepository teacherClasses,
      ResultRepository results,
      FeeRepository fees,
      SchoolEventRepository events,
      LeaveApplicationRepository leaveApplications,
      PasswordEncoder encoder) {
    return args -> {
      Account admin =
          account(
              accounts,
              encoder,
              "admin@schooldemo.com",
              "Admin@123",
              Role.ADMIN,
              "School Administrator",
              "+91 9876500000");
      Account teacher =
          account(
              accounts,
              encoder,
              "teacher@schooldemo.com",
              "Teacher@123",
              Role.TEACHER,
              "Neha Kulkarni",
              "+91 9876500001");
      Account aaravParent =
          account(
              accounts,
              encoder,
              "parent@schooldemo.com",
              "Parent@123",
              Role.PARENT,
              "Rajesh Sharma",
              "9876543210");
      Account aaravStudent =
          account(
              accounts,
              encoder,
              "student@schooldemo.com",
              "Student@123",
              Role.STUDENT,
              "Aarav Sharma",
              "9876543210");
      if (!teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
          teacher.getEmail(), "6", "A")) teacherClasses.save(new TeacherClass(teacher, "6", "A"));
      if (!teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
          teacher.getEmail(), "7", "A")) teacherClasses.save(new TeacherClass(teacher, "7", "A"));
      if (!teacherClasses.existsByTeacher_EmailAndClassNameAndDivision(
          teacher.getEmail(), "7", "B")) teacherClasses.save(new TeacherClass(teacher, "7", "B"));
      String[][] data = {
        {"STU001", "Aarav", "Sharma", "6", "A", "1", "Rajesh Sharma", "9876543210"},
        {"STU002", "Ananya", "Patil", "6", "A", "2", "Mahesh Patil", "9876543211"},
        {"STU003", "Vivaan", "Joshi", "6", "A", "3", "Suresh Joshi", "9876543212"},
        {"STU004", "Siya", "Kulkarni", "6", "A", "4", "Amit Kulkarni", "9876543213"},
        {"STU005", "Aditya", "Deshmukh", "7", "B", "1", "Nitin Deshmukh", "9876543214"},
        {"STU006", "Isha", "More", "7", "B", "2", "Prakash More", "9876543215"},
        {"STU007", "Reyansh", "Pawar", "7", "B", "3", "Sunil Pawar", "9876543216"},
        {"STU008", "Myra", "Jadhav", "8", "A", "1", "Rohan Jadhav", "9876543217"},
        {"STU009", "Arjun", "Shinde", "8", "A", "2", "Vijay Shinde", "9876543218"},
        {"STU010", "Riya", "Chavan", "8", "A", "3", "Santosh Chavan", "9876543219"},
        {"STU011", "Atharv", "Bhosale", "9", "B", "1", "Kiran Bhosale", "9876543220"},
        {"STU012", "Anvi", "Pawar", "9", "B", "2", "Rahul Pawar", "9876543221"},
        {"STU013", "Vedant", "Wagh", "10", "A", "1", "Pravin Wagh", "9876543222"},
        {"STU014", "Kavya", "Thakur", "10", "A", "2", "Sanjay Thakur", "9876543223"},
        {"STU015", "Omkar", "Gaikwad", "10", "A", "3", "Dinesh Gaikwad", "9876543224"}
      };
      if (students.count() == 0) {
        for (String[] d : data) {
          String email =
              d[0].equals("STU001")
                  ? "parent@schooldemo.com"
                  : "parent." + d[0].toLowerCase() + "@schooldemo.com";
          Account parent =
              accounts
                  .findByEmail(email)
                  .orElseGet(
                      () ->
                          accounts.save(
                              new Account(
                                  email, encoder.encode("Parent@123"), Role.PARENT, d[6], d[7])));
          Student s = new Student(d[0], d[1], d[2], d[3], d[4], Integer.parseInt(d[5]), parent);
          if (d[0].equals("STU001")) s.setStudentAccount(aaravStudent);
          students.save(s);
        }
      }
      if (homework.count() == 0) {
        homework.save(
            new Homework(
                "Fractions practice",
                "Complete exercises 1–12 in your maths workbook.",
                "6",
                "Mathematics",
                java.time.LocalDate.now().plusDays(3),
                teacher));
        homework.save(
            new Homework(
                "The water cycle",
                "Draw and label the water cycle in your science notebook.",
                "6",
                "Science",
                java.time.LocalDate.now().plusDays(5),
                teacher));
      }
      Student aarav = students.findByStudentCode("STU001").orElseThrow();
      if (results.count() == 0) {
        results.save(
            new AssessmentResult(
                aarav,
                "Term 1",
                "Mathematics",
                82,
                100,
                java.time.LocalDate.now().minusDays(12),
                teacher));
        results.save(
            new AssessmentResult(
                aarav,
                "Term 1",
                "Science",
                88,
                100,
                java.time.LocalDate.now().minusDays(9),
                teacher));
      }
      if (fees.count() == 0) {
        FeeRecord paid =
            new FeeRecord(aarav, "Term 1 tuition", 12500, java.time.LocalDate.now().minusDays(20));
        paid.setStatus(FeeStatus.PAID);
        fees.save(paid);
        fees.save(
            new FeeRecord(aarav, "Term 2 tuition", 12500, java.time.LocalDate.now().plusDays(21)));
      }
      if (events.count() == 0) {
        events.save(
            new SchoolEvent(
                "Annual Science Fair",
                "Student science projects and demonstrations.",
                java.time.LocalDate.now().plusDays(14),
                "School Auditorium",
                admin));
        events.save(
            new SchoolEvent(
                "Parent-Teacher Meeting",
                "Term progress discussions with class teachers.",
                java.time.LocalDate.now().plusDays(21),
                "Classrooms",
                admin));
      }
      if (leaveApplications.count() == 0) {
        leaveApplications.save(
            new LeaveApplication(
                aaravParent,
                aarav,
                java.time.LocalDate.now().plusDays(7),
                java.time.LocalDate.now().plusDays(8),
                "Family appointment"));
      }
    };
  }

  private Account account(
      AccountRepository repo,
      PasswordEncoder encoder,
      String email,
      String password,
      Role role,
      String name,
      String phone) {
    return repo.findByEmail(email)
        .orElseGet(
            () -> repo.save(new Account(email, encoder.encode(password), role, name, phone)));
  }
}
