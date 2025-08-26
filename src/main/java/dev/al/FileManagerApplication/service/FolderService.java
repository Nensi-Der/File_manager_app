package dev.al.FileManagerApplication.service;

import dev.al.FileManagerApplication.dto.MetadataDto;
import dev.al.FileManagerApplication.model.FolderEntity;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;


public interface FolderService {

    FolderEntity saveFolder(FolderEntity folder);

    Optional<FolderEntity> getFolderById(Long id);

    void deleteFolder(Long id);

    void deleteFoldersBatch(List<Long> ids);

    void renameFolder(Long id, String newName);

    FolderEntity moveFolder(Long id, Long newParentFolderId);

    List<FolderEntity> getFoldersByParentId(Long parentId);

    Page<FolderEntity> searchFolders(Map<String, String> filters, Pageable pageable);

    void addMetadata(Long folderId, MetadataDto metadataDto);

    void updateMetadata(Long folderId, String key, MetadataDto metadataDto);

    void deleteMetadata(Long folderId, String key);

    List<MetadataDto> getMetadata(Long folderId);

    void zipFolder(Long folderId, String destinationZipPath) throws IOException;

    void unzipFolder(String zipFilePath, String destinationFolderPath) throws IOException;

    void copyFolder(Long folderId, Long destinationFolderId) throws IOException;
}