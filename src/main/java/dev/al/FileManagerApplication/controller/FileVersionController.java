package dev.al.FileManagerApplication.controller;

import dev.al.FileManagerApplication.dto.ApiResponse;
import dev.al.FileManagerApplication.model.FileEntity;
import dev.al.FileManagerApplication.service.FileVersionService;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/files")
public class FileVersionController {

    private final FileVersionService fileVersionService;
    private final MessageSource messageSource;

    public FileVersionController(FileVersionService fileVersionService, MessageSource messageSource) {
        this.fileVersionService = fileVersionService;
        this.messageSource = messageSource;
    }

    // Create a new version of the latest file
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @PostMapping("/{id}/versions")
    public ApiResponse<FileEntity> createVersion(@PathVariable Long id,
                                                 @RequestHeader(name = "Accept-Language", required = false) Locale locale) {
        FileEntity file = fileVersionService.getFileVersions(id).stream()
                .filter(FileEntity::isLatest)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("file.original.notfound", new Object[]{id}, locale)));

        FileEntity newVersion = fileVersionService.createNewVersion(file);
        String message = messageSource.getMessage("action.success", null, locale);
        return ApiResponse.success(newVersion, message);
    }

    // Rollback to a previous version of the file
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @PostMapping("/{fileId}/rollback/{versionId}")
    public ApiResponse<String> rollback(@PathVariable Long fileId,
                                        @PathVariable Long versionId,
                                        @RequestHeader(name = "Accept-Language", required = false) Locale locale) {
        fileVersionService.rollbackToVersion(fileId, versionId);
        String message = messageSource.getMessage("action.success", null, locale);
        return ApiResponse.success(null, message);
    }

    // Get all versions of a file
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @GetMapping("/{id}/versions")
    public ApiResponse<List<FileEntity>> getVersions(@PathVariable Long id,
                                                     @RequestHeader(name = "Accept-Language", required = false) Locale locale) {
        List<FileEntity> versions = fileVersionService.getFileVersions(id);
        String message = messageSource.getMessage("object.found", null, locale);
        return ApiResponse.success(versions, message);
    }
}
