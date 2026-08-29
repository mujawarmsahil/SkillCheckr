-- ==========================================================
-- SkillCheckr Database Schema Definition
-- Database: exam_application_system
-- ==========================================================

CREATE DATABASE IF NOT EXISTS exam_application_system;
USE exam_application_system;

-- 1. Core Users Table
CREATE TABLE IF NOT EXISTS `user` (
    `user_id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `user_role` VARCHAR(50) NOT NULL,
    `profile_image` LONGTEXT NULL,
    `auth_provider` VARCHAR(50) DEFAULT 'LOCAL',
    `provider_id` VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Admin Table
CREATE TABLE IF NOT EXISTS `admin` (
    `admin_id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT,
    `name` VARCHAR(100) NOT NULL,
    `contact` VARCHAR(20),
    `email` VARCHAR(100) NOT NULL,
    `profile_image` LONGTEXT NULL,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Teacher Table
CREATE TABLE IF NOT EXISTS `teacher` (
    `teacher_id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT,
    `name` VARCHAR(100) NOT NULL,
    `contact` VARCHAR(20),
    `email` VARCHAR(100) NOT NULL,
    `profile_image` LONGTEXT NULL,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Student Table
CREATE TABLE IF NOT EXISTS `student` (
    `student_id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT,
    `name` VARCHAR(100) NOT NULL,
    `contact` VARCHAR(20),
    `email` VARCHAR(100) NOT NULL,
    `profile_image` LONGTEXT NULL,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Registration Requests Table
CREATE TABLE IF NOT EXISTS `request` (
    `request_id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `username` VARCHAR(100) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `requested_role` VARCHAR(50) NOT NULL,
    `contact` VARCHAR(20),
    `status` VARCHAR(50) DEFAULT 'Pending'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Subjects Table
CREATE TABLE IF NOT EXISTS `subject` (
    `subject_id` INT AUTO_INCREMENT PRIMARY KEY,
    `subject_name` VARCHAR(100) NOT NULL,
    `subject_code` VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Exams Table
CREATE TABLE IF NOT EXISTS `exam` (
    `exam_id` INT AUTO_INCREMENT PRIMARY KEY,
    `subject_id` INT NOT NULL,
    `teacher_id` INT,
    `exam_name` VARCHAR(150) NOT NULL,
    `exam_type` VARCHAR(50) DEFAULT 'MCQ', -- 'MCQ' or 'QUESTION_ANSWER'
    `exam_date` DATETIME NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `duration_minutes` INT NOT NULL,
    `total_marks` INT NOT NULL,
    `pass_marks` INT NOT NULL,
    `status` VARCHAR(50) DEFAULT 'Pending',
    FOREIGN KEY (`subject_id`) REFERENCES `subject`(`subject_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Questions Table
CREATE TABLE IF NOT EXISTS `question` (
    `question_id` INT AUTO_INCREMENT PRIMARY KEY,
    `subject_id` INT NOT NULL,
    `question_text` TEXT NOT NULL,
    FOREIGN KEY (`subject_id`) REFERENCES `subject`(`subject_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. Answers / Options Table
CREATE TABLE IF NOT EXISTS `answer` (
    `answer_id` INT AUTO_INCREMENT PRIMARY KEY,
    `question_id` INT NOT NULL,
    `option_text` TEXT NOT NULL,
    `is_correct` BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (`question_id`) REFERENCES `question`(`question_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. Student Results Table
CREATE TABLE IF NOT EXISTS `result` (
    `result_id` INT AUTO_INCREMENT PRIMARY KEY,
    `exam_id` INT NOT NULL,
    `student_id` INT NOT NULL,
    `marks_obtained` INT NOT NULL,
    `total_marks` INT NOT NULL,
    `passing_marks` INT NOT NULL,
    `percentage` DOUBLE NOT NULL,
    `status` VARCHAR(50) NOT NULL,
    `submitted_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Initial Seed Data: Default Admin User
-- Password: Admin@1234
INSERT IGNORE INTO `user` (`user_id`, `username`, `password`, `user_role`) 
VALUES (101, 'Admin1', 'Admin@1234', 'Admin');

INSERT IGNORE INTO `admin` (`admin_id`, `user_id`, `name`, `contact`, `email`) 
VALUES (1, 101, 'System Administrator', '9834303107', 'admin@skillcheckr.edu');
