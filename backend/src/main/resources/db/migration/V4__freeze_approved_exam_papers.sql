ALTER TABLE `exam`
    ADD COLUMN `paper_frozen` BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE `exam`
SET `paper_frozen` = TRUE
WHERE LOWER(`status`) IN ('upcoming', 'approved', 'completed', 'cancelled');
