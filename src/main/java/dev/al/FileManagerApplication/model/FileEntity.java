package dev.al.FileManagerApplication.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.util.List;
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "files")
public class FileEntity extends Auditable {

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    private String name;

    @Setter
    @Getter
    private String path;

    @Setter
    @Getter
    private Long size;

    @Setter
    @Getter
    private String type;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private FolderEntity parentFolder;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_file_id")
    private FileEntity parentFile;

    @Setter
    @Getter
    @OneToMany(mappedBy = "parentFile")
    private List<FileEntity> versions;

    @Setter
    @Getter
    private String thumbnailPath;

    @Setter
    @Getter
    private boolean latest;

    @Getter
    @Setter
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MetadataEntry> metadata;

    @Getter
    @Setter
    private String filePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @Setter
    @Getter
    @JoinColumn(name = "owner_id")
    private UserEntity owner;
}