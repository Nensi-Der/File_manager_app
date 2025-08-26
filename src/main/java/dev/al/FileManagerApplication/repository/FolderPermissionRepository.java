package dev.al.FileManagerApplication.repository;

import dev.al.FileManagerApplication.model.FolderEntity;
import dev.al.FileManagerApplication.model.FolderPermission;
import dev.al.FileManagerApplication.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FolderPermissionRepository extends JpaRepository<FolderPermission, Long> {
    Optional<FolderPermission> findByFolderAndUser(FolderEntity folder, UserEntity user);
}