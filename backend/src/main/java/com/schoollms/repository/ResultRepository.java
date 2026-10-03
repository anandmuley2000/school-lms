package com.schoollms.repository;

import com.schoollms.domain.AssessmentResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultRepository extends JpaRepository<AssessmentResult, Long> {
  List<AssessmentResult> findByStudent_IdOrderByExamDateDesc(Long studentId);

  boolean existsByStudent_IdAndExamNameAndSubject(Long studentId, String examName, String subject);

  void deleteByStudent_Id(Long studentId);
}
