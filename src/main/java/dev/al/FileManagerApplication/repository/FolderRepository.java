package dev.al.FileManagerApplication.repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import dev.al.FileManagerApplication.model.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<FolderEntity, Long>, JpaSpecificationExecutor<FolderEntity> {

    List<FolderEntity> findByParentFolderId(Long parentFolderId);

    @Transactional
    @Modifying
    @Query("DELETE FROM FolderEntity f WHERE f.id IN ?1")
    void deleteByIds(List<Long> ids);

    boolean existsByNameAndParentFolderId(String name, Long parentFolderId);

    long count();

    Page<FolderEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<FolderEntity> findByParentFolderId(Long parentFolderId, Pageable pageable);
}