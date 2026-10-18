-- Support the expired-attempt keyset sweep and exam question listing.
-- This is forward-only; roll back these indexes with a separate migration if required.
CREATE INDEX `idx_attempt_status_id` ON `exam_attempt` (`status`, `attempt_id`);

-- Keep exam question retrieval in display order without an extra sort.
CREATE INDEX `idx_exam_question_order` ON `exam_question` (`exam_id`, `question_order`);
