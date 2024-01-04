package com.beresik.fileStorage.service;

import com.beresik.fileStorage.config.DefaultFolderConfig;
import com.beresik.fileStorage.exception.FileNotFoundException;
import com.beresik.fileStorage.exception.FileSaveException;
import com.beresik.fileStorage.exception.FolderNotFoundException;
import com.beresik.fileStorage.model.FileModel;
import com.beresik.fileStorage.repository.FileRepository;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {
    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private DefaultFolderConfig defaultFolderConfig;

    @SneakyThrows
    public FileModel saveFile(MultipartFile file, String folder) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if(fileName.contains("..")) {
                throw new FileSaveException("Sorry! Filename contains invalid path sequence " + fileName);
            }
            FileModel dbFile = new FileModel(fileName, file.getContentType(), file.getBytes());

            // Save the file in the file system
            Path copyLocation = Paths.get(defaultFolderConfig.getPath() + File.separator + folder + File.separator + StringUtils.cleanPath(file.getOriginalFilename()));
            Files.createDirectories(copyLocation.getParent()); // Ensure the directory exists
            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileRepository.save(dbFile);
        } catch (Exception e) {
            throw new FileSaveException("Could not store file " + fileName + ". Please try again!", e);
        }
    }

    public FileModel getFile(UUID fileId) {
        return fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException("File not found with id " + fileId));
    }

    public List<FileModel> getAllFiles() {
        return fileRepository.findAll();
    }

    public List<String> getAllFilesInFolder(String folder) {
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(defaultFolderConfig.getPath() + File.separator + folder))) {
            for (Path path : directoryStream) {
                fileNames.add(path.getFileName().toString());
            }
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
        return fileNames;
    }
}
