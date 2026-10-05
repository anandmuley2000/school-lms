-- Upgrade existing installations without dropping or rewriting student records.
ALTER TABLE homework ADD COLUMN division varchar(255) NULL;
ALTER TABLE leave_applications ADD COLUMN reviewer_remark varchar(2000) NULL;
ALTER TABLE leave_applications ADD COLUMN reviewed_at datetime(6) NULL;
ALTER TABLE students ADD COLUMN bus_route_id bigint NULL;
ALTER TABLE students ADD CONSTRAINT fk_students_bus_route FOREIGN KEY (bus_route_id) REFERENCES bus_routes (id);
CREATE TABLE class_timetable (
  id bigint NOT NULL AUTO_INCREMENT,
  academic_year varchar(20) NOT NULL,
  class_name varchar(40) NOT NULL,
  division varchar(20) NOT NULL,
  day_of_week varchar(12) NOT NULL,
  subject varchar(100) NOT NULL,
  teacher_id bigint NOT NULL,
  room varchar(80) NOT NULL,
  start_time time NOT NULL,
  end_time time NOT NULL,
  PRIMARY KEY (id),
  KEY idx_timetable_class_day (class_name, division, day_of_week, start_time),
  KEY idx_timetable_teacher_day (teacher_id, day_of_week, start_time),
  CONSTRAINT fk_timetable_teacher FOREIGN KEY (teacher_id) REFERENCES accounts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
