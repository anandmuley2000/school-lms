package com.schoollms.repository;

import com.schoollms.domain.TeacherClass;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherClassRepository extends JpaRepository<TeacherClass, Long> {
  List<TeacherClass> findByTeacher_Email(String email);

  boolean existsByTeacher_EmailAndClassNameAndDivision(
      String email, String className, String division);

  boolean existsByTeacher_EmailAndClassName(String email, String className);

  boolean existsByTeacher_Id(Long id);
}
