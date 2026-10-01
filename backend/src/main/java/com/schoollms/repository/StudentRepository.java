package com.schoollms.repository;

import com.schoollms.domain.Student;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
  Optional<Student> findByStudentCode(String code);

  List<Student> findByParent_EmailOrderByClassNameAscRollNumberAsc(String email);

  List<Student> findByStudentAccount_Email(String email);

  List<Student> findByClassNameAndDivisionOrderByRollNumber(String className, String division);

  boolean existsByParent_Id(Long id);

  boolean existsByStudentAccount_Id(Long id);
}
