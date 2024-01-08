package com.beresik.fileStorage.service;

import com.beresik.fileStorage.config.DefaultFolderConfig;
import com.beresik.fileStorage.exception.DecryptFileException;
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
import java.util.List;
import java.util.UUID;

import static com.beresik.fileStorage.errors.DecryptErrors.DECRYPT_ERROR_WITH_MESSAGE_AND_CAUSE;

@Service
public class FileService {
    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private DefaultFolderConfig defaultFolderConfig;

    @Autowired
    private EncryptionService encryptionService;

    @SneakyThrows
    public FileModel saveFile(MultipartFile file, String folder) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if(fileName.contains("..")) {
                throw new FileSaveException("Sorry! Filename contains invalid path sequence " + fileName);
            }
            byte[] encryptedData = encryptionService.encrypt(file.getBytes());
            FileModel dbFile = new FileModel(fileName, file.getContentType(), encryptedData,folder);

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
        FileModel fileModel = fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException("File not found with id " + fileId));
        try {
            byte[] decryptedData = encryptionService.decrypt(fileModel.getData());
            fileModel.setData(decryptedData);
        } catch (DecryptFileException e) {
            throw new DecryptFileException(DECRYPT_ERROR_WITH_MESSAGE_AND_CAUSE, e);
        }
        return fileModel;
    }

    public List<FileModel> getAllFiles() {
        return fileRepository.findAll();
    }

    public void deleteFile(UUID fileId, String folder) {
        FileModel fileModel = fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException("File not found with id " + fileId));
        try {
            Files.deleteIfExists(Paths.get(defaultFolderConfig.getPath() + File.separator + folder + File.separator + fileModel.getFilename()));
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
        fileRepository.deleteById(fileId);
    }

    public void deleteFileByName(String fileName, String folder) {
        try {
            Files.deleteIfExists(Paths.get(defaultFolderConfig.getPath() + File.separator + folder + File.separator + fileName));
            FileModel fileModel = fileRepository.findByFilename(fileName);
            fileRepository.deleteById(fileModel.getFileid());
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
    }

    public void copyFile(UUID fileId, String oldFolder, String newFolder) {
        FileModel fileModel = fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException("File not found with id " + fileId));
        try {
            Files.copy(Paths.get(defaultFolderConfig.getPath() + File.separator + oldFolder + File.separator + fileModel.getFilename()), Paths.get(defaultFolderConfig.getPath() + File.separator + newFolder + File.separator + fileModel.getFilename()), StandardCopyOption.REPLACE_EXISTING);
            FileModel newFileModel = new FileModel(fileModel.getFilename(), fileModel.getFiletype(), fileModel.getData(), newFolder);
            fileRepository.save(newFileModel);
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
    }
    public void moveFile(UUID fileId, String oldFolder, String newFolder) {
        FileModel fileModel = fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException("File not found with id " + fileId));
        try {
            Files.move(Paths.get(defaultFolderConfig.getPath() + File.separator + oldFolder + File.separator + fileModel.getFilename()), Paths.get(defaultFolderConfig.getPath() + File.separator + newFolder + File.separator + fileModel.getFilename()), StandardCopyOption.REPLACE_EXISTING);
            fileModel.setFolderpath(newFolder);
            fileRepository.save(fileModel);
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
    }

    public void changeName(UUID fileId, String folder, String newName) {
        FileModel fileModel = fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException("File not found with id " + fileId));
        try {
            Files.move(Paths.get(defaultFolderConfig.getPath() + File.separator + folder + File.separator + fileModel.getFilename()), Paths.get(defaultFolderConfig.getPath() + File.separator + folder + File.separator + newName), StandardCopyOption.REPLACE_EXISTING);
            fileModel.setFilename(newName);
            fileRepository.save(fileModel);
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
    }

}
