-- Disable only the unchanged administrator seeded with the repository-known
-- password. Administrators who have already replaced that password are left
-- untouched.
UPDATE `user`
SET `status` = 'Inactive'
WHERE `username` = 'admin'
  AND `user_role` = 'Admin'
  AND `auth_provider` = 'LOCAL'
  AND `password` = '$2a$10$j2C435J408CE8xngWpT0eeKGLATzFE56FZVwgaYuW3B1cRQtTo2Vq';
