package dev.al.FileManagerApplication.repository;

import dev.al.FileManagerApplication.model.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long>, JpaSpecificationExecutor<FileEntity> {

    List<FileEntity> findByParentFolderId(Long parentFolderId);

    List<FileEntity> findByParentFileId(Long parentFileId);

    // Find the latest version of a file by parentFileId = null (root file) and latest=true
    @Query("SELECT f FROM FileEntity f WHERE f.parentFile IS NULL AND f.latest = true AND f.name = ?1")
    List<FileEntity> findLatestRootFilesByName(String name);

    // Batch delete files by ids
    @Transactional
    @Modifying
    @Query("DELETE FROM FileEntity f WHERE f.id IN ?1")
    void deleteByIds(List<Long> ids);

    boolean existsByNameAndParentFolderId(String name, Long parentFolderId);

    long count();

    List<FileEntity> findByParentFileOrId(FileEntity parentFile, Long id);

    Page<FileEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<FileEntity> findByParentFolderId(Long parentFolderId, Pageable pageable);
}