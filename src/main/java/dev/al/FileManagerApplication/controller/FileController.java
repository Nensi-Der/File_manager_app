package dev.al.FileManagerApplication.controller;
import org.springframework.security.core.Authentication;
import dev.al.FileManagerApplication.dto.ApiResponse;
import dev.al.FileManagerApplication.model.FileEntity;
import dev.al.FileManagerApplication.model.PermissionType;
import dev.al.FileManagerApplication.service.FileService;
import dev.al.FileManagerApplication.service.SharingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final MessageSource messageSource;
    private final SharingService sharingService;

    @Autowired
    public FileController(FileService fileService, MessageSource messageSource, SharingService sharingService) {
        this.fileService = fileService;
        this.messageSource = messageSource;
        this.sharingService = sharingService;
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/upload/{folderId}")
    public ResponseEntity<ApiResponse<Void>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long folderId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) throws IOException {

        fileService.uploadFile(file, folderId);
        String message = messageSource.getMessage("file.upload", new Object[]{file.getOriginalFilename(), folderId}, locale);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileEntity>> getFile(
            @PathVariable Long id,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        return fileService.getFileById(id)
                .map(file -> {
                    String message = messageSource.getMessage("object.found", null, locale);
                    return ResponseEntity.ok(ApiResponse.success(file, message));
                })
                .orElseGet(() -> {
                    String message = messageSource.getMessage("object.notfound", null, locale);
                    return ResponseEntity.status(404).body(ApiResponse.error(message));
                });
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable Long id,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        fileService.deleteFile(id);
        String message = messageSource.getMessage("file.delete.record", new Object[]{id}, locale);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/deleteBatch")
    public ResponseEntity<ApiResponse<Void>> deleteFilesBatch(
            @RequestBody List<Long> ids,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        fileService.deleteFilesBatch(ids);
        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Resource resource = fileService.downloadFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<FileEntity>>> searchFiles(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        Pageable pageable = PageRequest.of(page, size);
        Page<FileEntity> result = fileService.searchFiles(filters, pageable);
        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/version/{originalFileId}")
    public ResponseEntity<ApiResponse<FileEntity>> createNewVersion(
            @PathVariable Long originalFileId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) throws IOException {

        FileEntity newVersion = fileService.createNewVersion(originalFileId, file);
        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(newVersion, message));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/rollback/{versionFileId}")
    public ResponseEntity<ApiResponse<FileEntity>> rollbackToVersion(
            @PathVariable Long versionFileId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        FileEntity rolledBack = fileService.rollbackToVersion(versionFileId);
        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(rolledBack, message));
    }
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/rename/{id}")
    public ResponseEntity<ApiResponse<FileEntity>> renameFile(
            @PathVariable Long id,
            @RequestParam String newName,
            Authentication auth,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        FileEntity renamed = fileService.renameFile(id, newName);  // 🔄 Fixed method call
        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(renamed, message));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/move/{id}")
    public ResponseEntity<ApiResponse<FileEntity>> moveFile(
            @PathVariable Long id,
            @RequestParam Long newFolderId,
            Authentication auth,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) throws AccessDeniedException {

        boolean hasWrite = sharingService.hasPermission(auth.getName(), id, null, PermissionType.WRITE);  // 🔐 Check write permission
        if (!hasWrite) {
            throw new AccessDeniedException(messageSource.getMessage("no.permission", null, locale));
        }

        FileEntity moved = fileService.moveFile(id, newFolderId);
        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(moved, message));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/folder/{folderId}")
    public ResponseEntity<ApiResponse<List<FileEntity>>> getFilesByFolder(
            @PathVariable Long folderId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        List<FileEntity> files = fileService.getFilesByFolderId(folderId);
        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(files, message));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/versions/{parentFileId}")
    public ResponseEntity<ApiResponse<List<FileEntity>>> getFileVersions(
            @PathVariable Long parentFileId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        List<FileEntity> versions = fileService.getFileVersions(parentFileId);
        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(versions, message));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/files/{id}/preview")
    public ResponseEntity<Resource> previewFile(@PathVariable Long id) {
        Resource fileResource = fileService.previewFile(id);
        String contentType = "application/octet-stream";

        try {
            contentType = Files.probeContentType(Paths.get(fileResource.getFile().getAbsolutePath()));
        } catch (IOException ignored) {}

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(fileResource);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/files/{id}/thumbnail")
    public ResponseEntity<Resource> getFileThumbnail(@PathVariable Long id) {
        Resource thumbnail = fileService.downloadThumbnail(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/png")
                .body(thumbnail);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{fileId}/zip")
    public ResponseEntity<ApiResponse<Void>> zipFile(
            @PathVariable Long fileId,
            @RequestParam("destinationZipPath") String destinationZipPath,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        try {
            fileService.zipFile(fileId, destinationZipPath);
            String message = messageSource.getMessage("action.success", null, locale);
            return ResponseEntity.ok(ApiResponse.success(null, message));
        } catch (IOException | IllegalArgumentException e) {
            String message = messageSource.getMessage("action.failure", null, locale);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
        }
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/unzip")
    public ResponseEntity<ApiResponse<Void>> unzipFile(
            @RequestParam("zipFilePath") String zipFilePath,
            @RequestParam("destinationFilePath") String destinationFilePath,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        try {
            fileService.unzipFile(zipFilePath, destinationFilePath);
            String message = messageSource.getMessage("action.success", null, locale);
            return ResponseEntity.ok(ApiResponse.success(null, message));
        } catch (IOException e) {
            String message = messageSource.getMessage("action.failure", null, locale);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
        }
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{fileId}/copy")
    public ResponseEntity<ApiResponse<Void>> copyFile(
            @PathVariable Long fileId,
            @RequestParam Long destinationFolderId,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        try {
            fileService.copyFile(fileId, destinationFolderId);
            String message = messageSource.getMessage("action.success", null, locale);
            return ResponseEntity.ok(ApiResponse.success(null, message));
        } catch (Exception e) {
            String message = messageSource.getMessage("action.failure", null, locale);
            return ResponseEntity.badRequest().body(ApiResponse.error(message));
        }
    }
}
