-- Safe upgrade for existing MySQL 8 installations.
-- Adds fields/tables only; it never deletes or rewrites student records.
-- Each ALTER checks metadata first so re-running after a partial apply is safe.

SET @migration_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'homework' AND column_name = 'division'),
  'SELECT 1',
  'ALTER TABLE `homework` ADD COLUMN `division` varchar(255) NULL'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'leave_applications' AND column_name = 'reviewer_remark'),
  'SELECT 1',
  'ALTER TABLE `leave_applications` ADD COLUMN `reviewer_remark` varchar(2000) NULL'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'leave_applications' AND column_name = 'reviewed_at'),
  'SELECT 1',
  'ALTER TABLE `leave_applications` ADD COLUMN `reviewed_at` datetime(6) NULL'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'students' AND column_name = 'bus_route_id'),
  'SELECT 1',
  'ALTER TABLE `students` ADD COLUMN `bus_route_id` bigint NULL'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 'students'
      AND column_name = 'bus_route_id' AND referenced_table_name = 'bus_routes'
      AND referenced_column_name = 'id'),
  'SELECT 1',
  'ALTER TABLE `students` ADD CONSTRAINT `fk_students_bus_route` FOREIGN KEY (`bus_route_id`) REFERENCES `bus_routes` (`id`)'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

CREATE TABLE IF NOT EXISTS `class_timetable` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `academic_year` varchar(20) NOT NULL,
  `class_name` varchar(40) NOT NULL,
  `division` varchar(20) NOT NULL,
  `day_of_week` varchar(12) NOT NULL,
  `subject` varchar(100) NOT NULL,
  `teacher_id` bigint NOT NULL,
  `room` varchar(80) NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_timetable_class_day` (`class_name`, `division`, `day_of_week`, `start_time`),
  KEY `idx_timetable_teacher_day` (`teacher_id`, `day_of_week`, `start_time`),
  CONSTRAINT `fk_timetable_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
