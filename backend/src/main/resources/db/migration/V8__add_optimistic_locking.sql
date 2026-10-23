-- Forward-only: optimistic locking version columns for mutable aggregates.
ALTER TABLE `exam`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0;

ALTER TABLE `result`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0;
