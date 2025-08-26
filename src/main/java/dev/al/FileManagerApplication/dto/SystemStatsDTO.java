package dev.al.FileManagerApplication.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsDTO {
    private long totalFiles;
    private long totalFolders;
    private long totalUsers;
    private long totalFileVersions;

    // Constructors, Getters, Setters
}
