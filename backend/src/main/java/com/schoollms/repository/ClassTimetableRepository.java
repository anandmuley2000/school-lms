package com.schoollms.repository;

import com.schoollms.domain.ClassTimetable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassTimetableRepository extends JpaRepository<ClassTimetable, Long> {
  List<ClassTimetable> findAllByOrderByDayOfWeekAscStartTimeAsc();
  List<ClassTimetable> findByTeacher_IdOrderByDayOfWeekAscStartTimeAsc(Long teacherId);
  boolean existsByTeacher_Id(Long teacherId);
}
