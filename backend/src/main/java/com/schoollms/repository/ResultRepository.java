package com.schoollms.repository;

import com.schoollms.domain.AssessmentResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultRepository extends JpaRepository<AssessmentResult, Long> {
  List<AssessmentResult> findByStudent_IdOrderByExamDateDesc(Long studentId);

  void deleteByStudent_Id(Long studentId);
}
