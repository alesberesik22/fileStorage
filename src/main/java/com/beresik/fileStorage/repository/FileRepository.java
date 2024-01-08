package com.beresik.fileStorage.repository;

import com.beresik.fileStorage.model.FileModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileModel, UUID> {

    FileModel findByFilename(String fileName);
    FileModel findByFolderpath(String path);
    FileModel findByFilenameAndFolderpath(String fileName, String folderPath);
}
