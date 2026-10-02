-- ==========================================================================
-- SkillCheckr - fresh installation schema
-- ==========================================================================
--
-- Target database: exam_application_system (MySQL 8.x)
--
-- This file describes the CURRENT structure only. It is meant to be applied
-- once, to a completely empty database, and it contains no migration,
-- upgrade, or existence-checking logic. Create the database first, then run
-- this file against it:
--
--   CREATE DATABASE exam_application_system
--       CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
--   mysql -u root -p exam_application_system < src/main/resources/schema.sql
--
-- Conventions taken from the application code, not chosen freely here:
--   user.user_role          'Admin' | 'Teacher' | 'Student'   (RoleConstants)
--   user/student/teacher    any status other than 'Inactive' means usable
--   exam.status             'Pending' | 'Upcoming' | 'Approved' | 'Rejected'
--                           | 'Cancelled' | 'Completed'      (ExamConstants)
--   question.question_type  'MCQ' | 'QUESTION_ANSWER'          (ExamConstants)
--   exam_attempt.status     'IN_PROGRESS' | 'SUBMITTED'       (ExamConstants)
--   result.status           'Pass' | 'Fail'
--                           | 'Submitted for Evaluation'       (ExamConstants)
--   request.status          'Pending' | 'Approved' | 'Rejected'
--
-- Deliberate omissions, kept this way because the repositories depend on it:
--   * exam.teacher_id, result.exam_id and result.student_id carry no foreign
--     key. AdminRepositoryImpl checks for academic history in application code
--     and deactivates instead of deleting; an FK here would turn a soft
--     deactivation into a constraint violation.
--   * exam_registration, exam_attempt, exam_question and question rows are
--     removed by the repositories in dependency order rather than by cascade,
--     because those code paths delete only when the row counts allow it.
-- ==========================================================================


-- --------------------------------------------------------------------------
-- Accounts. `user` holds credentials and the role; the three role tables hold
-- the profile rows that the role-specific endpoints read.
-- --------------------------------------------------------------------------

CREATE TABLE `user` (
    `user_id`       INT AUTO_INCREMENT PRIMARY KEY,
    `username`      VARCHAR(100) NOT NULL,
    `password`      VARCHAR(255) NOT NULL,
    `user_role`     VARCHAR(50)  NOT NULL,
    `profile_image` LONGTEXT     NULL,
    `auth_provider` VARCHAR(50)  NOT NULL DEFAULT 'LOCAL',
    `provider_id`   VARCHAR(255) NULL,
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'Active',
    CONSTRAINT `uq_user_username` UNIQUE (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE `student` (
    `student_id`    INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`       INT          NULL,
    `name`          VARCHAR(100) NOT NULL,
    `contact`       VARCHAR(20)  NULL,
    `email`         VARCHAR(100) NOT NULL,
    `profile_image` LONGTEXT     NULL,
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'Active',
    CONSTRAINT `fk_student_user` FOREIGN KEY (`user_id`)
        REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE `teacher` (
    `teacher_id`    INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`       INT          NULL,
    `name`          VARCHAR(100) NOT NULL,
    `contact`       VARCHAR(20)  NULL,
    `email`         VARCHAR(100) NOT NULL,
    `profile_image` LONGTEXT     NULL,
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'Active',
    CONSTRAINT `fk_teacher_user` FOREIGN KEY (`user_id`)
        REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- `admin` intentionally has no `status` column: no admin status endpoint
-- exists and login reads the status from the `user` row.
CREATE TABLE `admin` (
    `admin_id`      INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`       INT          NULL,
    `name`          VARCHAR(100) NOT NULL,
    `contact`       VARCHAR(20)  NULL,
    `email`         VARCHAR(100) NOT NULL,
    `profile_image` LONGTEXT     NULL,
    CONSTRAINT `fk_admin_user` FOREIGN KEY (`user_id`)
        REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE `subject` (
    `subject_id`   INT AUTO_INCREMENT PRIMARY KEY,
    `subject_name` VARCHAR(100) NOT NULL,
    `subject_code` VARCHAR(50)  NOT NULL,
    CONSTRAINT `uq_subject_code` UNIQUE (`subject_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Self-service registration requests awaiting an administrator decision.
CREATE TABLE `request` (
    `request_id`     INT AUTO_INCREMENT PRIMARY KEY,
    `name`           VARCHAR(100) NOT NULL,
    `email`          VARCHAR(100) NOT NULL,
    `username`       VARCHAR(100) NOT NULL,
    `password`       VARCHAR(255) NOT NULL,
    `requested_role` VARCHAR(50)  NOT NULL,
    `contact`        VARCHAR(20)  NULL,
    `status`         VARCHAR(50)  NOT NULL DEFAULT 'Pending'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- --------------------------------------------------------------------------
-- Exams and questions.
-- --------------------------------------------------------------------------

CREATE TABLE `exam` (
    `exam_id`          INT AUTO_INCREMENT PRIMARY KEY,
    `subject_id`       INT           NOT NULL,
    `teacher_id`       INT           NULL,
    `exam_name`        VARCHAR(150)  NOT NULL,
    `exam_type`        VARCHAR(50)   NOT NULL DEFAULT 'MCQ',
    -- DATETIME, not DATE: ExamRepositoryImpl binds this column with
    -- PreparedStatement.setTimestamp, and AdminRepositoryImpl compares it
    -- with DATE(exam_date).
    `exam_date`        DATETIME      NOT NULL,
    `start_time`       TIME          NOT NULL,
    `end_time`         TIME          NOT NULL,
    `duration_minutes` INT           NOT NULL,
    `total_marks`      INT           NOT NULL,
    `pass_marks`       INT           NOT NULL,
    `status`           VARCHAR(50)   NOT NULL DEFAULT 'Pending',
    -- teacher_id is intentionally not a foreign key; see the header note.
    KEY `idx_exam_teacher` (`teacher_id`),
    CONSTRAINT `fk_exam_subject` FOREIGN KEY (`subject_id`)
        REFERENCES `subject` (`subject_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE `question` (
    `question_id`   INT AUTO_INCREMENT PRIMARY KEY,
    `subject_id`    INT          NOT NULL,
    `question_text` TEXT         NOT NULL,
    `question_type` VARCHAR(20)  NOT NULL DEFAULT 'MCQ',
    `marks`         INT          NOT NULL DEFAULT 1,
    -- Only meaningful for QUESTION_ANSWER questions.
    `word_limit`    INT          NULL,
    CONSTRAINT `fk_question_subject` FOREIGN KEY (`subject_id`)
        REFERENCES `subject` (`subject_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE `answer` (
    `answer_id`   INT AUTO_INCREMENT PRIMARY KEY,
    `question_id` INT          NOT NULL,
    `option_text` TEXT         NOT NULL,
    `is_correct`  TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT `fk_answer_question` FOREIGN KEY (`question_id`)
        REFERENCES `question` (`question_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Ordered question list for one exam. The pair is unique, which is what makes
-- QuestionRepositoryImpl reject a question that is already in the exam.
CREATE TABLE `exam_question` (
    `exam_question_id` INT AUTO_INCREMENT PRIMARY KEY,
    `exam_id`          INT NOT NULL,
    `question_id`      INT NOT NULL,
    `question_order`   INT NOT NULL,
    CONSTRAINT `uq_exam_question` UNIQUE (`exam_id`, `question_id`),
    CONSTRAINT `fk_exam_question_exam` FOREIGN KEY (`exam_id`)
        REFERENCES `exam` (`exam_id`),
    CONSTRAINT `fk_exam_question_question` FOREIGN KEY (`question_id`)
        REFERENCES `question` (`question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- --------------------------------------------------------------------------
-- Student-facing exam state.
-- --------------------------------------------------------------------------

CREATE TABLE `exam_registration` (
    `registration_id` INT AUTO_INCREMENT PRIMARY KEY,
    `student_id`      INT          NOT NULL,
    `exam_id`         INT          NOT NULL,
    `registered_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status`          VARCHAR(50)  NOT NULL DEFAULT 'Registered',
    -- ExamRepositoryImpl relies on this key for
    -- "ON DUPLICATE KEY UPDATE status = 'Registered'" when re-registering.
    CONSTRAINT `uq_exam_registration` UNIQUE (`student_id`, `exam_id`),
    CONSTRAINT `fk_registration_student` FOREIGN KEY (`student_id`)
        REFERENCES `student` (`student_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_registration_exam` FOREIGN KEY (`exam_id`)
        REFERENCES `exam` (`exam_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- A student holds at most one attempt per exam. The constraint is declared
-- here, so a fresh installation already has it and needs no upgrade step.
CREATE TABLE `exam_attempt` (
    `attempt_id`   INT AUTO_INCREMENT PRIMARY KEY,
    `exam_id`      INT          NOT NULL,
    `student_id`   INT          NOT NULL,
    `started_at`   DATETIME     NOT NULL,
    `expires_at`   DATETIME     NOT NULL,
    `submitted_at` DATETIME     NULL,
    `status`       VARCHAR(30)  NOT NULL DEFAULT 'IN_PROGRESS',
    CONSTRAINT `uq_exam_attempt` UNIQUE (`exam_id`, `student_id`),
    -- No ON DELETE rule on either key: AdminRepositoryImpl checks for
    -- academic history before deleting a profile and deactivates instead.
    CONSTRAINT `fk_attempt_exam` FOREIGN KEY (`exam_id`)
        REFERENCES `exam` (`exam_id`),
    CONSTRAINT `fk_attempt_student` FOREIGN KEY (`student_id`)
        REFERENCES `student` (`student_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One row per answered question. MCQ rows fill selected_answer_id, descriptive
-- rows fill text_answer, and marks_obtained stays NULL until grading.
-- The (attempt_id, question_id) key is what the autosave upsert relies on.
CREATE TABLE `attempt_answer` (
    `attempt_answer_id` INT AUTO_INCREMENT PRIMARY KEY,
    `attempt_id`        INT      NOT NULL,
    `question_id`       INT      NOT NULL,
    `selected_answer_id` INT     NULL,
    `text_answer`       TEXT     NULL,
    `marks_obtained`    INT      NULL,
    CONSTRAINT `uq_attempt_answer` UNIQUE (`attempt_id`, `question_id`),
    CONSTRAINT `fk_attempt_answer_attempt` FOREIGN KEY (`attempt_id`)
        REFERENCES `exam_attempt` (`attempt_id`),
    CONSTRAINT `fk_attempt_answer_question` FOREIGN KEY (`question_id`)
        REFERENCES `question` (`question_id`),
    CONSTRAINT `fk_attempt_answer_selected` FOREIGN KEY (`selected_answer_id`)
        REFERENCES `answer` (`answer_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One row per submitted attempt. exam_id and student_id are stored alongside
-- the attempt so the result listings need no join back through exam_attempt.
CREATE TABLE `result` (
    `result_id`      INT AUTO_INCREMENT PRIMARY KEY,
    `exam_id`        INT          NOT NULL,
    `student_id`     INT          NOT NULL,
    `marks_obtained` INT          NOT NULL,
    `total_marks`    INT          NOT NULL,
    `passing_marks`  INT          NOT NULL,
    `percentage`     DOUBLE       NOT NULL,
    `status`         VARCHAR(50)  NOT NULL,
    `submitted_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `attempt_id`     INT          NOT NULL,
    -- exam_id / student_id intentionally carry no foreign key; see the header note.
    KEY `idx_result_exam` (`exam_id`),
    KEY `idx_result_student` (`student_id`),
    -- Makes submission idempotent: the unique attempt_id is what
    -- ExamSubmissionServiceImpl relies on when it re-reads the stored result.
    CONSTRAINT `uq_result_attempt` UNIQUE (`attempt_id`),
    CONSTRAINT `fk_result_attempt` FOREIGN KEY (`attempt_id`)
        REFERENCES `exam_attempt` (`attempt_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- --------------------------------------------------------------------------
-- Development seed.
--
-- One disabled administrator profile so a freshly initialised database can be
-- provisioned without exposing a working, repository-known password.
-- The password is BCrypt, produced with the application's own
-- BCryptPasswordEncoder (strength 10); the plaintext never appears here.
--
--   username: admin
--
-- Before enabling this account, replace its password with a newly generated
-- BCrypt hash through a trusted provisioning process, then set status to Active.
-- --------------------------------------------------------------------------

INSERT INTO `user` (`username`, `password`, `user_role`, `auth_provider`, `status`)
VALUES ('admin',
        '$2a$10$j2C435J408CE8xngWpT0eeKGLATzFE56FZVwgaYuW3B1cRQtTo2Vq',
        'Admin',
        'LOCAL',
        'Inactive');

INSERT INTO `admin` (`user_id`, `name`, `contact`, `email`)
VALUES ((SELECT `user_id` FROM `user` WHERE `username` = 'admin'),
        'Administrator',
        NULL,
        'admin@skillcheckr.local');
