ALTER TABLE `student`
    MODIFY COLUMN `user_id` INT NOT NULL,
    ADD CONSTRAINT `uq_student_user` UNIQUE (`user_id`);

ALTER TABLE `teacher`
    MODIFY COLUMN `user_id` INT NOT NULL,
    ADD CONSTRAINT `uq_teacher_user` UNIQUE (`user_id`);

ALTER TABLE `admin`
    MODIFY COLUMN `user_id` INT NOT NULL,
    ADD CONSTRAINT `uq_admin_user` UNIQUE (`user_id`);
