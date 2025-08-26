package dev.al.FileManagerApplication.service.impl;
import dev.al.FileManagerApplication.model.PermissionType;
import dev.al.FileManagerApplication.service.SharingService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import java.util.Locale;
import dev.al.FileManagerApplication.model.FolderEntity;
import dev.al.FileManagerApplication.dto.MetadataDto;
import dev.al.FileManagerApplication.exception.ResourceNotFoundException;
import dev.al.FileManagerApplication.model.FileEntity;
import dev.al.FileManagerApplication.model.MetadataEntry;
import dev.al.FileManagerApplication.repository.FileRepository;
import dev.al.FileManagerApplication.repository.FolderRepository;
import dev.al.FileManagerApplication.service.FileService;
import dev.al.FileManagerApplication.specifications.FileSpecifications;
import dev.al.FileManagerApplication.util.ZipUtil;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import dev.al.FileManagerApplication.service.SharingService;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);
    private static final String STORAGE_PATH = "uploads/";

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final ZipUtil zipUtil;
    private final MessageSource messageSource;
    private final SharingService sharingService;

    public FileServiceImpl(FileRepository fileRepository, FolderRepository folderRepository, ZipUtil zipUtil, MessageSource messageSource, SharingService sharingService) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.zipUtil = zipUtil;
        this.messageSource = messageSource;
        this.sharingService = sharingService;

    }

    @Override
    public Optional<FileEntity> getFileById(Long id) {
        return fileRepository.findById(id);
    }

    @Override
    public FileEntity renameFile(Long id, String newName) {
        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())
                                + ": " + id));
        file.setName(newName);
        return fileRepository.save(file);
    }

    @Override
    public FileEntity moveFile(Long id, Long newFolderId) {
        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())
                                + ": " + id));
        FolderEntity newFolder = folderRepository.findById(newFolderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())
                                + ": " + newFolderId));
        file.setParentFolder(newFolder);
        return fileRepository.save(file);
    }

    @Override
    public void uploadFile(MultipartFile multipartFile, Long folderId) throws IOException {
        FolderEntity folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())
                                + ": " + folderId));

        String originalFileName = multipartFile.getOriginalFilename();
        if (originalFileName == null) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("file.original.notfound", new Object[]{folderId}, Locale.getDefault()));
        }

        if (fileRepository.existsByNameAndParentFolderId(originalFileName, folderId)
                || folderRepository.existsByNameAndParentFolderId(originalFileName, folderId)) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("name.exists", null, Locale.getDefault()));
        }

        String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;
        String filePath = STORAGE_PATH + uniqueFileName;

        File directory = new File(STORAGE_PATH);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException(
                    messageSource.getMessage("action.failure", null, Locale.getDefault())
                            + ": Failed to create storage directory at " + STORAGE_PATH);
        }

        File file = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());
        } catch (IOException e) {
            logger.error(messageSource.getMessage("action.failure", null, Locale.getDefault()), e);
            throw e;
        }

        FileEntity fileEntity = new FileEntity();
        fileEntity.setName(originalFileName);
        fileEntity.setPath(filePath);
        fileEntity.setParentFolder(folder);
        fileEntity.setType(multipartFile.getContentType());
        fileEntity.setSize(multipartFile.getSize());

        String thumbnailPath = null;
        if (multipartFile.getContentType() != null) {
            if (multipartFile.getContentType().startsWith("image")) {
                String thumbnailName = "thumb_" + uniqueFileName;
                String thumbnailFilePath = STORAGE_PATH + thumbnailName;
                Thumbnails.of(file)
                        .size(150, 150)
                        .toFile(new File(thumbnailFilePath));
                thumbnailPath = thumbnailFilePath;
            } else if (multipartFile.getContentType().equals("application/pdf")) {
                thumbnailPath = generatePdfThumbnail(file, uniqueFileName);
            }
        }

        fileEntity.setThumbnailPath(thumbnailPath);
        fileRepository.save(fileEntity);
        logger.info(messageSource.getMessage("file.upload", new Object[]{fileEntity.getName(), folderId}, Locale.getDefault()));
    }

    private String generatePdfThumbnail(File pdfFile, String uniqueFileName) {
        // Implement PDF thumbnail generation if needed
        return null; // Placeholder
    }

    @Override
    public void deleteFile(Long id) {
        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())
                                + ": " + id));
        deleteFileVersions(file);
        deletePhysicalFile(file.getPath());
        fileRepository.delete(file);
        logger.info(messageSource.getMessage("file.delete.record", new Object[]{id}, Locale.getDefault()));
    }

    private void deleteFileVersions(FileEntity file) {
        if (file.getVersions() != null) {
            for (FileEntity version : file.getVersions()) {
                deleteFileVersions(version);
                deletePhysicalFile(version.getPath());
                fileRepository.delete(version);
                logger.info(messageSource.getMessage("file.delete.version", new Object[]{version.getId()}, Locale.getDefault()));
            }
        }
    }

    private void deletePhysicalFile(String path) {
        File localFile = new File(path);
        if (localFile.exists()) {
            if (!localFile.delete()) {
                logger.warn(messageSource.getMessage("action.failure", null, Locale.getDefault())
                        + ": Failed to delete physical file: " + path);
            } else {
                logger.info(messageSource.getMessage("action.success", null, Locale.getDefault())
                        + ": Deleted physical file: " + path);
            }
        } else {
            logger.warn(messageSource.getMessage("object.notfound", null, Locale.getDefault())
                    + ": Physical file not found on disk: " + path);
        }
    }

    @Override
    @Transactional
    public void deleteFilesBatch(List<Long> ids) {
        for (Long id : ids) {
            deleteFile(id);
        }
        logger.info(messageSource.getMessage("action.success", null, Locale.getDefault())
                + ": Batch deleted files with IDs: " + ids);
    }

    @Override
    public Resource downloadFile(Long fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())
                                + ": " + fileId));
        File file = new File(fileEntity.getPath());
        if (!file.exists()) {
            throw new ResourceNotFoundException(
                    messageSource.getMessage("file.original.notfound", new Object[]{fileEntity.getId()}, Locale.getDefault()));
        }
        logger.info(messageSource.getMessage("file.downloading", null, Locale.getDefault())
                + ": " + fileEntity.getName());
        return new FileSystemResource(file);
    }

    @Override
    public List<FileEntity> getFilesByFolderId(Long folderId) {
        return fileRepository.findByParentFolderId(folderId);
    }

    @Override
    public Page<FileEntity> searchFiles(Map<String, String> filters, Pageable pageable) {
        Specification<FileEntity> spec = FileSpecifications.withFilters(filters);
        return fileRepository.findAll(spec, pageable);
    }

    @Override
    public List<FileEntity> getFileVersions(Long parentFileId) {
        return fileRepository.findByParentFileId(parentFileId);
    }

    @Override
    @Transactional
    public FileEntity createNewVersion(Long originalFileId, MultipartFile newFile) throws IOException {
        FileEntity original = fileRepository.findById(originalFileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())
                                + ": " + originalFileId));

        if (original.isLatest()) {
            original.setLatest(false);
            fileRepository.save(original);
        }

        String uniqueFileName = UUID.randomUUID() + "_" + newFile.getOriginalFilename();
        String filePath = STORAGE_PATH + uniqueFileName;

        File directory = new File(STORAGE_PATH);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException(
                    messageSource.getMessage("action.failure", null, Locale.getDefault())
                            + ": Failed to create storage directory");
        }

        File fileOnDisk = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(fileOnDisk)) {
            fos.write(newFile.getBytes());
        }

        FileEntity newVersion = new FileEntity();
        newVersion.setName(newFile.getOriginalFilename());
        newVersion.setPath(filePath);
        newVersion.setParentFolder(original.getParentFolder());
        newVersion.setParentFile(original);
        newVersion.setLatest(true);
        newVersion.setSize(newFile.getSize());
        newVersion.setType(newFile.getContentType());

        fileRepository.save(newVersion);
        logger.info(messageSource.getMessage("action.success", null, Locale.getDefault())
                + ": Created new version for file ID " + originalFileId);
        return newVersion;
    }

    @Override
    @Transactional
    public FileEntity rollbackToVersion(Long versionFileId) {
        FileEntity version = fileRepository.findById(versionFileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())
                                + ": " + versionFileId));

        FileEntity root = version;
        while (root.getParentFile() != null) {
            root = root.getParentFile();
        }

        List<FileEntity> allVersions = fileRepository.findByParentFileId(root.getId());
        allVersions.add(root);

        for (FileEntity v : allVersions) {
            v.setLatest(false);
            fileRepository.save(v);
        }

        version.setLatest(true);
        fileRepository.save(version);

        logger.info(messageSource.getMessage("action.success", null, Locale.getDefault())
                + ": Rolled back file ID " + root.getId() + " to version ID " + versionFileId);
        return version;
    }

    @Override
    public Resource previewFile(Long fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())
                                + ": " + fileId));

        File file = new File(fileEntity.getPath());
        if (!file.exists()) {
            throw new ResourceNotFoundException(
                    messageSource.getMessage("file.original.notfound", new Object[]{fileEntity.getId()}, Locale.getDefault()));
        }

        if (fileEntity.getType() != null && fileEntity.getType().startsWith("text")) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                StringBuilder previewContent = new StringBuilder();
                String line;
                int lines = 0;
                while ((line = reader.readLine()) != null && lines < 100) {
                    previewContent.append(line).append("\n");
                    lines++;
                }
                reader.close();
                return new InputStreamResource(new ByteArrayInputStream(previewContent.toString().getBytes()));
            } catch (IOException e) {
                throw new RuntimeException(
                        messageSource.getMessage("action.failure", null, Locale.getDefault())
                                + ": Error reading file preview", e);
            }
        }

        return new FileSystemResource(file);
    }

    @Override
    public Resource downloadThumbnail(Long fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())
                                + ": " + fileId));

        if (fileEntity.getThumbnailPath() == null) {
            throw new ResourceNotFoundException(
                    messageSource.getMessage("object.notfound", null, Locale.getDefault())
                            + ": Thumbnail not available");
        }

        File thumbnailFile = new File(fileEntity.getThumbnailPath());
        if (!thumbnailFile.exists()) {
            throw new ResourceNotFoundException(
                    messageSource.getMessage("file.original.notfound", new Object[]{fileEntity.getId()}, Locale.getDefault()));
        }

        return new FileSystemResource(thumbnailFile);
    }

    @Override
    public void addMetadata(Long fileId, MetadataDto metadataDto) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())));

        MetadataEntry metadataEntry = new MetadataEntry();
        metadataEntry.setEntryKey(metadataDto.getKey());
        metadataEntry.setValue(metadataDto.getValue());
        metadataEntry.setFile(file);

        file.getMetadata().add(metadataEntry);
        fileRepository.save(file);
    }

    @Override
    public void updateMetadata(Long fileId, String key, MetadataDto metadataDto) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())));

        Optional<MetadataEntry> metadataOptional = file.getMetadata().stream()
                .filter(m -> m.getEntryKey().equals(key))
                .findFirst();

        if (metadataOptional.isPresent()) {
            MetadataEntry metadataEntry = metadataOptional.get();
            metadataEntry.setValue(metadataDto.getValue());
            fileRepository.save(file);
        } else {
            throw new ResourceNotFoundException(
                    messageSource.getMessage("object.notfound", null, Locale.getDefault())
                            + ": Metadata key not found");
        }
    }


    @Override
    public void deleteMetadata(Long fileId, String key) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())));

        file.getMetadata().removeIf(m -> m.getEntryKey().equals(key));
        fileRepository.save(file);
    }

    @Override
    public List<MetadataDto> getMetadata(Long fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())));

        List<MetadataDto> metadataDtos = new ArrayList<>();
        for (MetadataEntry metadataEntry : file.getMetadata()) {
            metadataDtos.add(new MetadataDto(
                    metadataEntry.getEntryKey(),
                    metadataEntry.getValue()
            ));
        }
        return metadataDtos;
    }
    @Override
    public void zipFile(Long fileId, String destinationZipPath) throws IOException {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())));

        zipUtil.zipFile(Path.of(file.getPath()), Path.of(destinationZipPath));
        logger.info(messageSource.getMessage("action.success", null, Locale.getDefault())
                + ": Zipped file ID " + fileId + " to " + destinationZipPath);
    }

    @Override
    public void unzipFile(String zipFilePath, String destinationFilePath) throws IOException {
        zipUtil.unzipFile(Path.of(zipFilePath), Path.of(destinationFilePath));
        logger.info(messageSource.getMessage("action.success", null, Locale.getDefault())
                + ": Unzipped " + zipFilePath + " to " + destinationFilePath);
    }

    @Override
    public void copyFile(Long fileId, Long destinationFolderId) throws IOException {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())));
        FolderEntity destinationFolder = folderRepository.findById(destinationFolderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("object.notfound", null, Locale.getDefault())));

        Path sourcePath = Paths.get(file.getPath());
        String newFileName = UUID.randomUUID() + "_" + file.getName();
        Path destinationPath = Paths.get(STORAGE_PATH, newFileName);

        Files.copy(sourcePath, destinationPath);

        FileEntity newFile = new FileEntity();
        newFile.setName(file.getName());
        newFile.setPath(destinationPath.toString());
        newFile.setParentFolder(destinationFolder);
        newFile.setType(file.getType());
        newFile.setSize(file.getSize());

        fileRepository.save(newFile);
        logger.info(messageSource.getMessage("action.success", null, Locale.getDefault())
                + ": Copied file ID " + fileId + " to folder ID " + destinationFolderId);


    }

    public FileEntity getFileForUser(Long fileId, String username) {
        boolean hasRead = sharingService.hasPermission(username, fileId, null, PermissionType.READ);
        if (!hasRead) {
            throw new AccessDeniedException(messageSource.getMessage("no.permission", null, Locale.getDefault()));
        }
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException(
                        messageSource.getMessage("file.original.notfound", new Object[]{fileId}, Locale.getDefault())
                ));
    }

    // Add this method:
    public void updateFile(Long fileId, String username, FileEntity updatedFile) {
        boolean hasWrite = sharingService.hasPermission(username, fileId, null, PermissionType.WRITE);
        if (!hasWrite) {
            throw new AccessDeniedException(messageSource.getMessage("no.permission", null, Locale.getDefault()));
        }
        FileEntity existingFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException(
                        messageSource.getMessage("file.original.notfound", new Object[]{fileId}, Locale.getDefault())
                ));
        existingFile.setName(updatedFile.getName());
        // add other fields to update if needed
        fileRepository.save(existingFile);
    }


}
