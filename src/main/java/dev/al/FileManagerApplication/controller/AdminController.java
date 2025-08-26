package dev.al.FileManagerApplication.controller;

import dev.al.FileManagerApplication.model.FolderEntity;
import dev.al.FileManagerApplication.model.UserActivityLog;
import dev.al.FileManagerApplication.repository.UserActivityLogRepository;
import dev.al.FileManagerApplication.service.FolderService;
import dev.al.FileManagerApplication.dto.ApiResponse;

import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;


import java.util.Locale;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final MessageSource messageSource;
    private final UserActivityLogRepository userActivityLogRepository;
    private final FolderService folderService;

    public AdminController(UserActivityLogRepository userActivityLogRepository, FolderService folderService, MessageSource messageSource) {
        this.userActivityLogRepository = userActivityLogRepository;
        this.folderService = folderService;
        this.messageSource = messageSource;
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> searchFolders(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FolderEntity> folderPage = folderService.searchFolders(filters, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(folderPage, "Action committed successfully")
        );
    }

    @GetMapping("/activity-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> getActivityLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UserActivityLog> logs;

        if (userId != null && start != null && end != null) {
            logs = userActivityLogRepository.findByUserIdAndEventTimeBetween(userId, start, end, pageable);
        } else if (start != null && end != null) {
            logs = userActivityLogRepository.findByEventTimeBetween(start, end, pageable);
        } else if (eventType != null) {
            logs = userActivityLogRepository.findByEventType(eventType, pageable);
        } else {
            logs = userActivityLogRepository.findAll(pageable);
        }

        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(logs, message));
    }
}