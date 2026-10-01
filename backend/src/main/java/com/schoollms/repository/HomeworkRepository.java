package com.schoollms.repository;

import com.schoollms.domain.Homework;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeworkRepository extends JpaRepository<Homework, Long> {
  List<Homework> findByClassNameOrderByDueDateAsc(String className);
}
