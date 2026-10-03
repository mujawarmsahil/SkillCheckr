CREATE TABLE `exam_question_bank` (
    `exam_id`     INT          NOT NULL PRIMARY KEY,
    `file_name`   VARCHAR(255) NOT NULL,
    `pdf_content` LONGBLOB     NOT NULL,
    `uploaded_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_question_bank_exam` FOREIGN KEY (`exam_id`)
        REFERENCES `exam` (`exam_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
