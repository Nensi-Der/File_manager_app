package dev.al.FileManagerApplication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FolderRequestDto {

    @NotBlank(message = "Folder name cannot be blank")
    private String name;
}