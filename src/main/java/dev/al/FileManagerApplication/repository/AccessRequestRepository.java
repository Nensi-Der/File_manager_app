package dev.al.FileManagerApplication.repository;

import dev.al.FileManagerApplication.model.AccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


import dev.al.FileManagerApplication.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {

    List<AccessRequest> findByFileAndStatus(FileEntity file, AccessStatus status);

    List<AccessRequest> findByFolderAndStatus(FolderEntity folder, AccessStatus status);

    List<AccessRequest> findByRequester(UserEntity requester);

    List<AccessRequest> findByFile(FileEntity file);

    List<AccessRequest> findByFolder(FolderEntity folder);
}