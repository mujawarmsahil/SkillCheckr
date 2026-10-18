package com.skillcheckr.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;
import com.skillcheckr.persistence.entity.AdminProfileEntity;
import com.skillcheckr.persistence.entity.StudentProfileEntity;
import com.skillcheckr.persistence.entity.TeacherProfileEntity;
import com.skillcheckr.persistence.entity.UserAccountEntity;
import com.skillcheckr.persistence.repository.AdminProfileJpaRepository;
import com.skillcheckr.persistence.repository.StudentProfileJpaRepository;
import com.skillcheckr.persistence.repository.TeacherProfileJpaRepository;
import com.skillcheckr.persistence.repository.UserAccountJpaRepository;

@Repository
public class AuthRepositoryImpl implements AuthRepository {

    private final UserAccountJpaRepository accountRepository;
    private final StudentProfileJpaRepository studentRepository;
    private final TeacherProfileJpaRepository teacherRepository;
    private final AdminProfileJpaRepository adminRepository;

    public AuthRepositoryImpl(UserAccountJpaRepository accountRepository,
            StudentProfileJpaRepository studentRepository,
            TeacherProfileJpaRepository teacherRepository,
            AdminProfileJpaRepository adminRepository) {
        this.accountRepository = accountRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return accountRepository.findByUsernameIgnoreCase(username.trim()).map(this::toUser);
    }

    @Override
    @Transactional
    public boolean updatePassword(int userId, String passwordHash) {
        return accountRepository.findById(userId).map(account -> {
            account.setPassword(passwordHash);
            accountRepository.save(account);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findPasswordByUserId(int userId) {
        if (userId <= 0) {
            return Optional.empty();
        }
        return accountRepository.findById(userId).map(UserAccountEntity::getPassword);
    }

    @Override
    @Transactional(readOnly = true)
    public int getStudentIdByUserId(int userId) {
        return studentRepository.findByUser_UserId(userId).map(StudentProfileEntity::getStudentId).orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public int getTeacherIdByUserId(int userId) {
        return teacherRepository.findByUser_UserId(userId).map(TeacherProfileEntity::getTeacherId).orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public int getAdminIdByUserId(int userId) {
        return adminRepository.findByUser_UserId(userId).map(AdminProfileEntity::getAdminId).orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileDTO> getUserProfile(int userId) {
        return accountRepository.findById(userId).map(this::toProfile);
    }

    @Override
    @Transactional
    public Optional<UserProfileDTO> updateUserProfile(UserProfileDTO profile) {
        if (profile == null || profile.getUserId() <= 0) {
            return Optional.empty();
        }

        Optional<UserAccountEntity> accountResult = accountRepository.findById(profile.getUserId());
        if (accountResult.isEmpty()) {
            return Optional.empty();
        }

        UserAccountEntity account = accountResult.get();
        boolean profileUpdated = switch (normalizedRole(account.getRole())) {
            case "student" -> updateStudentProfile(profile);
            case "teacher" -> updateTeacherProfile(profile);
            case "admin" -> updateAdminProfile(profile);
            default -> false;
        };
        if (!profileUpdated) {
            return Optional.empty();
        }

        account.setUsername(profile.getUsername());
        account.setProfileImage(profile.getProfileImage());
        if (profile.getPassword() != null && !profile.getPassword().isBlank()) {
            account.setPassword(profile.getPassword());
        }
        accountRepository.save(account);

        return Optional.of(toProfile(account));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameInUse(String username, int excludeUserId) {
        return username != null
                && accountRepository.existsByUsernameIgnoreCaseAndUserIdNot(username.trim(), excludeUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return username != null && accountRepository.existsByUsernameIgnoreCase(username.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailInUse(String email, int excludeUserId) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalizedEmail = email.trim();
        return studentRepository.existsByEmailIgnoreCaseAndUser_UserIdNot(normalizedEmail, excludeUserId)
                || teacherRepository.existsByEmailIgnoreCaseAndUser_UserIdNot(normalizedEmail, excludeUserId)
                || adminRepository.existsByEmailIgnoreCaseAndUser_UserIdNot(normalizedEmail, excludeUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserActive(int userId) {
        if (userId <= 0) {
            return false;
        }
        return accountRepository.findById(userId)
                .map(UserAccountEntity::getStatus)
                .map(status -> status == null || status.isBlank() || !"Inactive".equalsIgnoreCase(status.trim()))
                .orElse(false);
    }

    private boolean updateStudentProfile(UserProfileDTO profile) {
        return studentRepository.findByUser_UserId(profile.getUserId()).map(entity -> {
            copyProfile(profile, entity.getName(), entity::setName, entity::setEmail, entity::setContact,
                    entity::setProfileImage);
            studentRepository.save(entity);
            return true;
        }).orElse(false);
    }

    private boolean updateTeacherProfile(UserProfileDTO profile) {
        return teacherRepository.findByUser_UserId(profile.getUserId()).map(entity -> {
            copyProfile(profile, entity.getName(), entity::setName, entity::setEmail, entity::setContact,
                    entity::setProfileImage);
            teacherRepository.save(entity);
            return true;
        }).orElse(false);
    }

    private boolean updateAdminProfile(UserProfileDTO profile) {
        return adminRepository.findByUser_UserId(profile.getUserId()).map(entity -> {
            copyProfile(profile, entity.getName(), entity::setName, entity::setEmail, entity::setContact,
                    entity::setProfileImage);
            adminRepository.save(entity);
            return true;
        }).orElse(false);
    }

    private void copyProfile(UserProfileDTO source, String currentName,
            java.util.function.Consumer<String> setName,
            java.util.function.Consumer<String> setEmail,
            java.util.function.Consumer<String> setContact,
            java.util.function.Consumer<String> setProfileImage) {
        setName.accept(source.getName() == null ? currentName : source.getName());
        setEmail.accept(source.getEmail());
        setContact.accept(source.getContact());
        setProfileImage.accept(source.getProfileImage());
    }

    private UserProfileDTO toProfile(UserAccountEntity account) {
        UserProfileDTO profile = new UserProfileDTO();
        profile.setUserId(account.getUserId());
        profile.setUsername(account.getUsername());
        profile.setRole(account.getRole());
        profile.setProfileImage(account.getProfileImage());

        switch (normalizedRole(account.getRole())) {
            case "student" -> studentRepository.findByUser_UserId(account.getUserId()).ifPresent(entity -> {
                profile.setRoleId(entity.getStudentId());
                profile.setName(entity.getName());
                profile.setEmail(entity.getEmail());
                profile.setContact(entity.getContact());
                useProfileImage(profile, entity.getProfileImage());
            });
            case "teacher" -> teacherRepository.findByUser_UserId(account.getUserId()).ifPresent(entity -> {
                profile.setRoleId(entity.getTeacherId());
                profile.setName(entity.getName());
                profile.setEmail(entity.getEmail());
                profile.setContact(entity.getContact());
                useProfileImage(profile, entity.getProfileImage());
            });
            case "admin" -> adminRepository.findByUser_UserId(account.getUserId()).ifPresent(entity -> {
                profile.setRoleId(entity.getAdminId());
                profile.setName(entity.getName());
                profile.setEmail(entity.getEmail());
                profile.setContact(entity.getContact());
                useProfileImage(profile, entity.getProfileImage());
            });
            default -> {
            }
        }

        if (profile.getName() == null || profile.getName().isBlank()) {
            profile.setName(account.getUsername());
        }
        if (profile.getEmail() == null) {
            profile.setEmail("");
        }
        if (profile.getContact() == null) {
            profile.setContact("");
        }
        return profile;
    }

    private User toUser(UserAccountEntity account) {
        User user = new User();
        user.setUserId(account.getUserId());
        user.setUsername(account.getUsername());
        user.setPassword(account.getPassword());
        user.setRole(account.getRole());
        user.setProfileImage(account.getProfileImage());
        user.setAuthProvider(account.getAuthProvider());
        user.setProviderId(account.getProviderId());
        user.setStatus(account.getStatus());
        return user;
    }

    private static void useProfileImage(UserProfileDTO profile, String roleProfileImage) {
        if (roleProfileImage != null && !roleProfileImage.isBlank()) {
            profile.setProfileImage(roleProfileImage);
        }
    }

    private static String normalizedRole(String role) {
        return role == null ? "" : role.trim().toLowerCase();
    }
}
