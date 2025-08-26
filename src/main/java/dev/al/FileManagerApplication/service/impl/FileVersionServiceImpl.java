package dev.al.FileManagerApplication.service.impl;

import dev.al.FileManagerApplication.model.FileEntity;
import dev.al.FileManagerApplication.repository.FileRepository;
import dev.al.FileManagerApplication.service.FileVersionService;
import jakarta.transaction.Transactional;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class FileVersionServiceImpl implements FileVersionService {

    private final FileRepository fileRepository;
    private final MessageSource messageSource;

    public FileVersionServiceImpl(FileRepository fileRepository, MessageSource messageSource) {
        this.fileRepository = fileRepository;
        this.messageSource = messageSource;
    }

    @Override
    @Transactional
    public FileEntity createNewVersion(FileEntity currentFile) {
        // Mark current as not latest
        currentFile.setLatest(false);
        fileRepository.save(currentFile);

        FileEntity newVersion = new FileEntity();
        newVersion.setName(currentFile.getName());
        newVersion.setPath(currentFile.getPath());
        newVersion.setType(currentFile.getType());
        newVersion.setSize(currentFile.getSize());
        newVersion.setParentFolder(currentFile.getParentFolder());
        newVersion.setParentFile(currentFile.getParentFile() == null ? currentFile : currentFile.getParentFile());
        newVersion.setLatest(true);
        newVersion.setThumbnailPath(currentFile.getThumbnailPath());
        newVersion.setFilePath(currentFile.getFilePath());

        return fileRepository.save(newVersion);
    }

    @Override
    @Transactional
    public void rollbackToVersion(Long fileId, Long versionId) {
        FileEntity version = fileRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("version.notfound", null, Locale.getDefault())));
        FileEntity original = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("file.notfound", null, Locale.getDefault())));

        // Fetch all child versions
        List<FileEntity> allVersions = fileRepository.findByParentFileId(original.getId());
        // Include the original file as well
        allVersions.add(original);

        // Mark all as not latest
        for (FileEntity v : allVersions) {
            v.setLatest(false);
            fileRepository.save(v);
        }

        // Create a new version using rollback target
        FileEntity rollback = new FileEntity();
        rollback.setName(version.getName());
        rollback.setPath(version.getPath());
        rollback.setSize(version.getSize());
        rollback.setType(version.getType());
        rollback.setParentFolder(version.getParentFolder());
        rollback.setParentFile(original);
        rollback.setThumbnailPath(version.getThumbnailPath());
        rollback.setFilePath(version.getFilePath());
        rollback.setLatest(true);

        fileRepository.save(rollback);
    }

    @Override
    public List<FileEntity> getFileVersions(Long fileId) {
        FileEntity original = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("file.notfound", null, Locale.getDefault())));

        List<FileEntity> versions = fileRepository.findByParentFileId(fileId);
        // Include the original file in the version history
        versions.add(original);
        return versions;
    }
}
