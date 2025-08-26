package dev.al.FileManagerApplication.service;

import dev.al.FileManagerApplication.dto.MetadataDto;
import dev.al.FileManagerApplication.model.FileEntity;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FileService {

    Optional<FileEntity> getFileById(Long id);

    FileEntity renameFile(Long id, String newName);

    FileEntity moveFile(Long id, Long newFolderId);

    void uploadFile(MultipartFile multipartFile, Long folderId) throws IOException;

    void deleteFile(Long id);

    void deleteFilesBatch(List<Long> ids);

    Resource downloadFile(Long fileId);

    List<FileEntity> getFilesByFolderId(Long folderId);

    Page<FileEntity> searchFiles(Map<String, String> filters, Pageable pageable);

    List<FileEntity> getFileVersions(Long parentFileId);

    FileEntity createNewVersion(Long originalFileId, MultipartFile newFile) throws IOException;

    FileEntity rollbackToVersion(Long versionFileId);

    Resource previewFile(Long fileId);

    Resource downloadThumbnail(Long fileId);

    void addMetadata(Long fileId, MetadataDto metadataDto);

    void updateMetadata(Long fileId, String key, MetadataDto metadataDto);

    void deleteMetadata(Long fileId, String key);

    List<MetadataDto> getMetadata(Long fileId);

    void zipFile(Long fileId, String destinationZipPath) throws IOException;

    void unzipFile(String zipFilePath, String destinationFilePath) throws IOException;

    void copyFile(Long fileId, Long destinationFolderId) throws IOException;

}
