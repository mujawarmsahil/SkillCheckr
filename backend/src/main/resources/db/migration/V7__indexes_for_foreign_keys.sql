-- Forward-only index additions for foreign-key and common lookup columns.
-- Complements V5 (attempt-status keyset sweep, exam-question ordering).

CREATE INDEX `idx_question_subject` ON `question` (`subject_id`);

CREATE INDEX `idx_exam_subject` ON `exam` (`subject_id`);

CREATE INDEX `idx_exam_question_exam` ON `exam_question` (`exam_id`);
CREATE INDEX `idx_exam_question_question` ON `exam_question` (`question_id`);

CREATE INDEX `idx_exam_registration_student` ON `exam_registration` (`student_id`);
CREATE INDEX `idx_exam_registration_exam` ON `exam_registration` (`exam_id`);

CREATE INDEX `idx_exam_attempt_exam` ON `exam_attempt` (`exam_id`);
CREATE INDEX `idx_exam_attempt_student` ON `exam_attempt` (`student_id`);

CREATE INDEX `idx_attempt_answer_attempt` ON `attempt_answer` (`attempt_id`);
CREATE INDEX `idx_attempt_answer_question` ON `attempt_answer` (`question_id`);

CREATE INDEX `idx_result_exam` ON `result` (`exam_id`);
CREATE INDEX `idx_result_student` ON `result` (`student_id`);
CREATE INDEX `idx_result_attempt` ON `result` (`attempt_id`);

CREATE INDEX `idx_request_status` ON `request` (`status`);
