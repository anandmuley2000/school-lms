-- Create-only baseline for an empty MySQL 8 database.
-- Schema-only export; contains no application rows and no DROP statements.
-- Apply manually before starting the production profile; Hibernate uses ddl-auto=validate.

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `email` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `role` enum('ADMIN','PARENT','STUDENT','TEACHER') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKn7ihswpy07ci568w34q0oi8he` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assessment_results` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exam_date` date DEFAULT NULL,
  `exam_name` varchar(255) NOT NULL,
  `marks` double NOT NULL,
  `maximum_marks` double NOT NULL,
  `subject` varchar(255) NOT NULL,
  `entered_by_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKlppmp6slbsvr6nab1nh2i7xq2` (`entered_by_id`),
  KEY `FK8odlxtqrb7krn83rmy2opkeuy` (`student_id`),
  CONSTRAINT `FK8odlxtqrb7krn83rmy2opkeuy` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKlppmp6slbsvr6nab1nh2i7xq2` FOREIGN KEY (`entered_by_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attendance_date` date NOT NULL,
  `status` enum('ABSENT','LATE','LEAVE','PRESENT') NOT NULL,
  `marked_by_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtc2hy77ldgu1s2a7r0ws0h0gi` (`student_id`,`attendance_date`),
  KEY `FKav79turvkk6vhgsb8plgqgsmb` (`marked_by_id`),
  CONSTRAINT `FK7121lveuhtmu9wa6m90ayd5yg` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKav79turvkk6vhgsb8plgqgsmb` FOREIGN KEY (`marked_by_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bus_routes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bus_number` varchar(255) NOT NULL,
  `capacity` int DEFAULT NULL,
  `current_stop` varchar(255) DEFAULT NULL,
  `driver_name` varchar(255) NOT NULL,
  `driver_phone` varchar(255) NOT NULL,
  `last_updated` datetime(6) NOT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `route_name` varchar(255) NOT NULL,
  `speed_kmvh` double DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `stops` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` double NOT NULL,
  `description` varchar(255) NOT NULL,
  `due_date` date DEFAULT NULL,
  `paid_date` date DEFAULT NULL,
  `status` enum('OVERDUE','PAID','PENDING') NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpa7aak9gybhyk0gr2vldtvrrb` (`student_id`),
  CONSTRAINT `FKpa7aak9gybhyk0gr2vldtvrrb` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `homework` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_name` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(4000) DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `subject` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `teacher_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfvy0gxptd3lb3bjeq5sx4gfgv` (`teacher_id`),
  CONSTRAINT `FKfvy0gxptd3lb3bjeq5sx4gfgv` FOREIGN KEY (`teacher_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `leave_applications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `end_date` date NOT NULL,
  `reason` varchar(2000) NOT NULL,
  `start_date` date NOT NULL,
  `status` enum('APPROVED','PENDING','REJECTED') NOT NULL,
  `requester_id` bigint NOT NULL,
  `student_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKla3lrsj221yst9gpuiu9pi1rd` (`requester_id`),
  KEY `FKl7o9l67k2mwalit8mywjxhq9r` (`student_id`),
  CONSTRAINT `FKl7o9l67k2mwalit8mywjxhq9r` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKla3lrsj221yst9gpuiu9pi1rd` FOREIGN KEY (`requester_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `message` varchar(2000) NOT NULL,
  `is_read` bit(1) NOT NULL,
  `title` varchar(255) NOT NULL,
  `recipient_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKrphdmgvl924ajapmo7l8hyx3i` (`recipient_id`),
  CONSTRAINT `FKrphdmgvl924ajapmo7l8hyx3i` FOREIGN KEY (`recipient_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `school_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(4000) DEFAULT NULL,
  `event_date` date NOT NULL,
  `location` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `created_by_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKl49ck51jrsaq46ck9symn58kf` (`created_by_id`),
  CONSTRAINT `FKl49ck51jrsaq46ck9symn58kf` FOREIGN KEY (`created_by_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `school_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `body` varchar(5000) NOT NULL,
  `is_read` bit(1) NOT NULL,
  `sent_at` datetime(6) NOT NULL,
  `subject` varchar(255) NOT NULL,
  `recipient_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfmdh9t7yd0eg37gf3nqw0y72i` (`recipient_id`),
  KEY `FK9vuuihljq023fc20boi0cw7cb` (`sender_id`),
  CONSTRAINT `FK9vuuihljq023fc20boi0cw7cb` FOREIGN KEY (`sender_id`) REFERENCES `accounts` (`id`),
  CONSTRAINT `FKfmdh9t7yd0eg37gf3nqw0y72i` FOREIGN KEY (`recipient_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `class_name` varchar(255) NOT NULL,
  `date_of_birth` date DEFAULT NULL,
  `division` varchar(255) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `gender` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `roll_number` int NOT NULL,
  `status` varchar(255) DEFAULT NULL,
  `student_code` varchar(255) NOT NULL,
  `parent_account_id` bigint DEFAULT NULL,
  `student_account_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKcgcf3r5xk73o0etbduc1qxnol` (`student_code`),
  KEY `FKouwblni3ektbyy32e6l32oakk` (`parent_account_id`),
  KEY `FKoek08t7kvq4epsjdlf6w49kko` (`student_account_id`),
  CONSTRAINT `FKoek08t7kvq4epsjdlf6w49kko` FOREIGN KEY (`student_account_id`) REFERENCES `accounts` (`id`),
  CONSTRAINT `FKouwblni3ektbyy32e6l32oakk` FOREIGN KEY (`parent_account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_classes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_name` varchar(255) NOT NULL,
  `division` varchar(255) NOT NULL,
  `teacher_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK7kxgv0gdqd3kh7hq3kcs71ec7` (`teacher_id`,`class_name`,`division`),
  CONSTRAINT `FKmaip6aw3jttktgdix1awjwc9r` FOREIGN KEY (`teacher_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;



