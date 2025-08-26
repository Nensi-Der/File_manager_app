package dev.al.FileManagerApplication.repository;

import dev.al.FileManagerApplication.model.SharedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SharedItemRepository extends JpaRepository<SharedItem, Long> {
    List<SharedItem> findBySharedWithUsername(String username);
    List<SharedItem> findBySharedWithUsernameAndFileId(String username, Long fileId);
    List<SharedItem> findBySharedWithUsernameAndFolderId(String username, Long folderId);
}
