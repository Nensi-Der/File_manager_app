package dev.al.FileManagerApplication.service.impl;
import dev.al.FileManagerApplication.model.PermissionType;
import dev.al.FileManagerApplication.service.SharingService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import java.util.Locale;
import dev.al.FileManagerApplication.dto.MetadataDto;
import dev.al.FileManagerApplication.exception.ResourceNotFoundException;
import dev.al.FileManagerApplication.model.FileEntity;
import dev.al.FileManagerApplication.model.FolderEntity;
import dev.al.FileManagerApplication.model.MetadataEntry;
import dev.al.FileManagerApplication.repository.FileRepository;
import dev.al.FileManagerApplication.repository.FolderRepository;
import dev.al.FileManagerApplication.service.FileService;
import dev.al.FileManagerApplication.service.FolderService;
import dev.al.FileManagerApplication.util.ZipUtil;
import dev.al.FileManagerApplication.specifications.FolderSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FolderServiceImpl implements FolderService {

    private static final Logger logger = LoggerFactory.getLogger(FolderServiceImpl.class);

    private final FolderRepository folderRepository;
    private final FileService fileService;
    private final FileRepository fileRepository;
    private final ZipUtil zipUtil;
    private final MessageSource messageSource;
    private final SharingService sharingService;

    public FolderServiceImpl(FolderRepository folderRepository, FileService fileService,
                             FileRepository fileRepository, ZipUtil zipUtil, MessageSource messageSource ,SharingService sharingService) {
        this.folderRepository = folderRepository;
        this.fileService = fileService;
        this.fileRepository = fileRepository;
        this.zipUtil = zipUtil;
        this.messageSource = messageSource;
        this.sharingService = sharingService;
    }

    @Override
    public FolderEntity saveFolder(FolderEntity folder) {
        Long parentId = folder.getParentFolder() != null ? folder.getParentFolder().getId() : null;
        String folderName = folder.getName();

        // Check if a folder with the same name exists in the same parent
        if (folderRepository.existsByNameAndParentFolderId(folderName, parentId)) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("folder.name.exists", null, Locale.getDefault())
            );
        }

        // Check if a file with the same name exists in the same parent
        if (fileRepository.existsByNameAndParentFolderId(folderName, parentId)) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("file.name.exists", null, Locale.getDefault())
            );
        }

        return folderRepository.save(folder);
    }

    @Override
    public java.util.Optional<FolderEntity> getFolderById(Long id) {
        return folderRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteFolder(Long id) {
        FolderEntity folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("folder.notfound.id", new Object[]{id}, Locale.getDefault())
                ));

        // Recursively delete child folders
        List<FolderEntity> childFolders = folderRepository.findByParentFolderId(id);
        for (FolderEntity child : childFolders) {
            deleteFolder(child.getId());
        }

        // Delete files inside this folder
        List<FileEntity> files = fileRepository.findByParentFolderId(id);
        for (FileEntity file : files) {
            fileService.deleteFile(file.getId());
        }

        folderRepository.delete(folder);
        logger.info(messageSource.getMessage("folder.deleted.id", new Object[]{id}, Locale.getDefault()));
    }

    @Override
    @Transactional
    public void deleteFoldersBatch(List<Long> ids) {
        // Instead of deleting directly, recursively delete each folder to handle nested files/folders
        for (Long id : ids) {
            deleteFolder(id);
        }
        logger.info(messageSource.getMessage("folders.batch.deleted.ids", new Object[]{ids}, Locale.getDefault()));
    }

    @Override
    public void renameFolder(Long id, String newName) {
        FolderEntity folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("folder.notfound.id", new Object[]{id}, Locale.getDefault())
                ));
        folder.setName(newName);
    }

    @Override
    public FolderEntity moveFolder(Long id, Long newParentFolderId) {
        FolderEntity folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("folder.notfound.id", new Object[]{id}, Locale.getDefault())
                ));
        FolderEntity newParent = folderRepository.findById(newParentFolderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("folder.notfound.id", new Object[]{newParentFolderId}, Locale.getDefault())
                ));
        folder.setParentFolder(newParent);
        return folderRepository.save(folder);
    }

    @Override
    public List<FolderEntity> getFoldersByParentId(Long parentId) {
        return folderRepository.findByParentFolderId(parentId);
    }

    @Override
    public Page<FolderEntity> searchFolders(Map<String, String> filters, Pageable pageable) {
        Specification<FolderEntity> spec = FolderSpecifications.withFilters(filters);
        return folderRepository.findAll(spec, pageable);
    }

    @Override
    public void addMetadata(Long folderId, MetadataDto metadataDto) {
        FolderEntity folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("folder.notfound", null, Locale.getDefault())
                ));

        MetadataEntry metadataEntry = new MetadataEntry();
        metadataEntry.setEntryKey(metadataDto.getKey());
        metadataEntry.setValue(metadataDto.getValue());
        metadataEntry.setFolder(folder);

        folder.getMetadata().add(metadataEntry);
        folderRepository.save(folder);
    }

    @Override
    public void updateMetadata(Long folderId, String key, MetadataDto metadataDto) {
        FolderEntity folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("folder.notfound", null, Locale.getDefault())
                ));

        MetadataEntry metadataEntry = folder.getMetadata().stream()
                .filter(m -> m.getEntryKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("metadata.key.notfound", null, Locale.getDefault())
                ));

        metadataEntry.setValue(metadataDto.getValue());
        folderRepository.save(folder);
    }

    @Override
    public void deleteMetadata(Long folderId, String key) {
        FolderEntity folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("folder.notfound", null, Locale.getDefault())
                ));

        folder.getMetadata().removeIf(m -> m.getEntryKey().equals(key));
        folderRepository.save(folder);
    }

    @Override
    public List<MetadataDto> getMetadata(Long folderId) {
        FolderEntity folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("folder.notfound", null, Locale.getDefault())
                ));

        return folder.getMetadata().stream()
                .map(m -> new MetadataDto(m.getEntryKey(), m.getValue()))
                .toList();
    }

    @Override
    public void zipFolder(Long folderId, String destinationZipPath) throws IOException {
        FolderEntity folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("folder.notfound.id", new Object[]{folderId}, Locale.getDefault())
                ));

        Path sourceDir = Paths.get(folder.getFolderPath()); // Make sure FolderEntity has getFolderPath()
        Path destinationZip = Paths.get(destinationZipPath);

        if (Files.exists(destinationZip)) {
            throw new IOException(
                    messageSource.getMessage("zip.destination.exists", new Object[]{destinationZipPath}, Locale.getDefault())
            );
        }

        zipUtil.zipFolder(sourceDir, destinationZip);
    }

    @Override
    public void unzipFolder(String zipFilePath, String destinationFolderPath) throws IOException {
        Path destinationDir = Paths.get(destinationFolderPath);

        if (Files.exists(destinationDir)) {
            throw new IOException(
                    messageSource.getMessage("unzip.destination.exists", new Object[]{destinationFolderPath}, Locale.getDefault())
            );
        }

        zipUtil.unzipFolder(Paths.get(zipFilePath), destinationDir);
    }

    @Override
    @Transactional
    public void copyFolder(Long folderId, Long destinationFolderId) throws IOException {
        FolderEntity sourceFolder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("folder.notfound", null, Locale.getDefault())
                ));

        FolderEntity destinationFolder = folderRepository.findById(destinationFolderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("destination.folder.notfound", null, Locale.getDefault())
                ));

        // Check name conflicts
        if (folderRepository.existsByNameAndParentFolderId(sourceFolder.getName(), destinationFolderId) ||
                fileRepository.existsByNameAndParentFolderId(sourceFolder.getName(), destinationFolderId)) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("file.folder.name.exists.destination", null, Locale.getDefault())
            );
        }

        // Recursive copy helper
        copyFolderRecursive(sourceFolder, destinationFolder);
    }

    private void copyFolderRecursive(FolderEntity source, FolderEntity destinationParent) throws IOException {
        // Create new folder in destination
        FolderEntity newFolder = new FolderEntity();
        newFolder.setName(source.getName());
        newFolder.setParentFolder(destinationParent);
        // You might want to generate new folder path accordingly here
        folderRepository.save(newFolder);

        // Copy all files in source folder
        List<FileEntity> files = fileRepository.findByParentFolderId(source.getId());
        for (FileEntity file : files) {
            Path sourcePath = Paths.get(file.getFilePath());
            Path destinationPath = Paths.get(newFolder.getFolderPath(), file.getName());
            Files.copy(sourcePath, destinationPath);

            FileEntity copiedFile = new FileEntity();
            copiedFile.setName(file.getName());
            copiedFile.setParentFolder(newFolder);
            copiedFile.setFilePath(destinationPath.toString());
            fileRepository.save(copiedFile);
        }

        // Recursively copy subfolders
        List<FolderEntity> subFolders = folderRepository.findByParentFolderId(source.getId());
        for (FolderEntity subFolder : subFolders) {
            copyFolderRecursive(subFolder, newFolder);
        }
    }

    public FolderEntity getFolderForUser(Long folderId, String username) {
        boolean hasRead = sharingService.hasPermission(username, null, folderId, PermissionType.READ);
        if (!hasRead) {
            throw new AccessDeniedException(messageSource.getMessage("no.permission", null, Locale.getDefault()));
        }
        return folderRepository.findById(folderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())
                ));
    }

    public void updateFolder(Long folderId, String username, FolderEntity updatedFolder) {
        boolean hasWrite = sharingService.hasPermission(username, null, folderId, PermissionType.WRITE);
        if (!hasWrite) {
            throw new AccessDeniedException(messageSource.getMessage("no.permission", null, Locale.getDefault()));
        }
        FolderEntity existingFolder = folderRepository.findById(folderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())
                ));
        existingFolder.setName(updatedFolder.getName());
        // add other fields to update if needed
        folderRepository.save(existingFolder);
    }
}
