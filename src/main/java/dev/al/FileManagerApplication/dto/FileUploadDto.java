package dev.al.FileManagerApplication.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadDto {
    @NotNull(message = "File must be provided")
    private MultipartFile file;

    @NotNull(message = "Target folder ID must be provided")
    private Long folderId;
}