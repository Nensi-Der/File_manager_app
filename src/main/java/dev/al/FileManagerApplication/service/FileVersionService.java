package dev.al.FileManagerApplication.service;

import dev.al.FileManagerApplication.model.FileEntity;
import java.util.List;

public interface FileVersionService {
    FileEntity createNewVersion(FileEntity currentFile);
    void rollbackToVersion(Long fileId, Long versionId);
    List<FileEntity> getFileVersions(Long fileId);
}