package com.convo.file_sharing.repository;

import com.convo.file_sharing.entity.FileDownload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FileDownloadRepository extends JpaRepository<FileDownload, UUID> {

    List<FileDownload> findByContentHashOrderByDownloadedAtAsc(String contentHash);
}
