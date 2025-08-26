package dev.al.FileManagerApplication.repository;

import dev.al.FileManagerApplication.model.FileEntity;
import dev.al.FileManagerApplication.model.FilePermission;
import dev.al.FileManagerApplication.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FilePermissionRepository extends JpaRepository<FilePermission, Long> {
    Optional<FilePermission> findByFileAndUser(FileEntity file, UserEntity user);
}