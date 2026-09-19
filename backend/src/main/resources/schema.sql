-- ==========================================================
-- SkillCheckr Database Schema Definition
-- Database: exam_application_system
-- This file mirrors the existing database. It does not migrate data.
-- ==========================================================

USE exam_application_system;

CREATE TABLE IF NOT EXISTS `user` (
    `user_id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `user_role` VARCHAR(50) NOT NULL,
    `profile_image` LONGTEXT NULL,
    `auth_provider` VARCHAR(50) DEFAULT 'LOCAL',
    `provider_id` VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `student` (
    `student_id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NULL,
    `name` VARCHAR(100) NOT NULL,
    `contact` VARCHAR(20) NULL,
    `email` VARCHAR(100) NOT NULL,
    `profile_image` LONGTEXT NULL,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `teacher` (
    `teacher_id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NULL,
    `name` VARCHAR(100) NOT NULL,
    `contact` VARCHAR(20) NULL,
    `email` VARCHAR(100) NOT NULL,
    `profile_image` LONGTEXT NULL,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `admin` (
    `admin_id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NULL,
    `name` VARCHAR(100) NOT NULL,
    `contact` VARCHAR(20) NULL,
    `email` VARCHAR(100) NOT NULL,
    `profile_image` LONGTEXT NULL,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `subject` (
    `subject_id` INT AUTO_INCREMENT PRIMARY KEY,
    `subject_name` VARCHAR(100) NOT NULL,
    `subject_code` VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `request` (
    `request_id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `username` VARCHAR(100) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `requested_role` VARCHAR(50) NOT NULL,
    `contact` VARCHAR(20) NULL,
    `status` VARCHAR(50) DEFAULT 'Pending'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `exam` (
    `exam_id` INT AUTO_INCREMENT PRIMARY KEY,
    `subject_id` INT NOT NULL,
    `teacher_id` INT NULL,
    `exam_name` VARCHAR(150) NOT NULL,
    `exam_type` VARCHAR(50) DEFAULT 'MCQ',
    `exam_date` DATETIME NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `duration_minutes` INT NOT NULL,
    `total_marks` INT NOT NULL,
    `pass_marks` INT NOT NULL,
    `status` VARCHAR(50) DEFAULT 'Pending',
    FOREIGN KEY (`subject_id`) REFERENCES `subject`(`subject_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `question` (
    `question_id` INT AUTO_INCREMENT PRIMARY KEY,
    `subject_id` INT NOT NULL,
    `question_text` TEXT NOT NULL,
    `question_type` VARCHAR(20) NOT NULL DEFAULT 'MCQ',
    `marks` INT NOT NULL DEFAULT 1,
    `word_limit` INT NULL,
    FOREIGN KEY (`subject_id`) REFERENCES `subject`(`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `answer` (
    `answer_id` INT AUTO_INCREMENT PRIMARY KEY,
    `question_id` INT NOT NULL,
    `option_text` TEXT NOT NULL,
    `is_correct` TINYINT(1) DEFAULT 0,
    FOREIGN KEY (`question_id`) REFERENCES `question`(`question_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `exam_question` (
    `exam_question_id` INT AUTO_INCREMENT PRIMARY KEY,
    `exam_id` INT NOT NULL,
    `question_id` INT NOT NULL,
    `question_order` INT NOT NULL,
    UNIQUE KEY `unique_exam_question` (`exam_id`, `question_id`),
    FOREIGN KEY (`exam_id`) REFERENCES `exam`(`exam_id`),
    FOREIGN KEY (`question_id`) REFERENCES `question`(`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `exam_registration` (
    `registration_id` INT AUTO_INCREMENT PRIMARY KEY,
    `student_id` INT NOT NULL,
    `exam_id` INT NOT NULL,
    `registered_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `status` VARCHAR(50) DEFAULT 'Registered',
    UNIQUE KEY `unique_student_exam` (`student_id`, `exam_id`),
    FOREIGN KEY (`student_id`) REFERENCES `student`(`student_id`) ON DELETE CASCADE,
    FOREIGN KEY (`exam_id`) REFERENCES `exam`(`exam_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `exam_attempt` (
    `attempt_id` INT AUTO_INCREMENT PRIMARY KEY,
    `exam_id` INT NOT NULL,
    `student_id` INT NOT NULL,
    `started_at` DATETIME NOT NULL,
    `expires_at` DATETIME NOT NULL,
    `submitted_at` DATETIME NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    FOREIGN KEY (`exam_id`) REFERENCES `exam`(`exam_id`),
    FOREIGN KEY (`student_id`) REFERENCES `student`(`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `attempt_answer` (
    `attempt_answer_id` INT AUTO_INCREMENT PRIMARY KEY,
    `attempt_id` INT NOT NULL,
    `question_id` INT NOT NULL,
    `selected_answer_id` INT NULL,
    `text_answer` TEXT NULL,
    `marks_obtained` INT NULL,
    UNIQUE KEY `unique_attempt_question` (`attempt_id`, `question_id`),
    FOREIGN KEY (`attempt_id`) REFERENCES `exam_attempt`(`attempt_id`),
    FOREIGN KEY (`question_id`) REFERENCES `question`(`question_id`),
    FOREIGN KEY (`selected_answer_id`) REFERENCES `answer`(`answer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `result` (
    `result_id` INT AUTO_INCREMENT PRIMARY KEY,
    `exam_id` INT NOT NULL,
    `student_id` INT NOT NULL,
    `marks_obtained` INT NOT NULL,
    `total_marks` INT NOT NULL,
    `passing_marks` INT NOT NULL,
    `percentage` DOUBLE NOT NULL,
    `status` VARCHAR(50) NOT NULL,
    `submitted_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `attempt_id` INT NOT NULL,
    UNIQUE KEY `unique_result_attempt` (`attempt_id`),
    FOREIGN KEY (`attempt_id`) REFERENCES `exam_attempt`(`attempt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
