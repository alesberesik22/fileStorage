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
import java.util.Arrays;
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
        System.out.println("File name: " + StringUtils.cleanPath(file.getOriginalFilename()));
        String fileName = StringUtils.cleanPath(encryptionService.encrypt(file.getOriginalFilename()));
        String encryptedFolder = encryptionService.encryptPath(folder);
        try {
            if(fileName.contains("..")) {
                throw new FileSaveException("Sorry! Filename contains invalid path sequence " + fileName);
            }
            byte[] encryptedData = encryptionService.encrypt(file.getBytes());
            FileModel dbFile = new FileModel(fileName, file.getContentType(), encryptedData,encryptedFolder);

            // Save the file in the file system
            Path copyLocation = Paths.get(defaultFolderConfig.getPath() + File.separator + encryptedFolder + File.separator + encryptionService.encrypt(StringUtils.cleanPath(file.getOriginalFilename())));
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
            fileModel.setFilename(encryptionService.decrypt(fileModel.getFilename()));
            fileModel.setFolderpath(encryptionService.decryptPath(fileModel.getFolderpath()));
        } catch (DecryptFileException e) {
            throw new DecryptFileException(DECRYPT_ERROR_WITH_MESSAGE_AND_CAUSE, e);
        }
        return fileModel;
    }

    public List<FileModel> getAllFiles() {

        List<FileModel> fileModelList = fileRepository.findAll();
        Arrays.asList(fileModelList).forEach(System.out::println);
        for (FileModel fileModel : fileModelList) {
            try {
                System.out.println("pred");
                byte[] decryptedData = encryptionService.decrypt(fileModel.getData());
                System.out.println("prve");
                fileModel.setData(decryptedData);
                fileModel.setFilename(encryptionService.decrypt(fileModel.getFilename()));
                System.out.println("druhe");
                fileModel.setFolderpath(encryptionService.decryptPath(fileModel.getFolderpath()));
                System.out.println("tretie");
            } catch (DecryptFileException e) {
                throw new DecryptFileException(DECRYPT_ERROR_WITH_MESSAGE_AND_CAUSE, e);
            }
        }
        return fileModelList;
    }

    public void deleteFile(UUID fileId, String folder) {
        final String encryptedFolder = encryptionService.encryptPath(folder);
        FileModel fileModel = fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException("File not found with id " + fileId));
        try {
            Files.deleteIfExists(Paths.get(defaultFolderConfig.getPath() + File.separator + encryptedFolder + File.separator + fileModel.getFilename()));
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
        fileRepository.deleteById(fileId);
    }

    public void deleteFileByName(String fileName, String folder) {
        try {
            final String encryptedFolder = encryptionService.encryptPath(folder);
            final String encryptedFile = encryptionService.encrypt(fileName);
            Files.deleteIfExists(Paths.get(defaultFolderConfig.getPath() + File.separator + encryptedFolder + File.separator + encryptedFile));
            FileModel fileModel = fileRepository.findByFilename(encryptedFile);
            fileRepository.deleteById(fileModel.getFileid());
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
    }

    public void copyFile(UUID fileId, String oldFolder, String newFolder) {
        final String encryptedOldFolder = encryptionService.encryptPath(oldFolder);
        final String encryptedNewFolder = encryptionService.encryptPath(newFolder);
        FileModel fileModel = fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException("File not found with id " + fileId));
        System.out.println(fileModel.getFileid());
        try {
            Files.copy(Paths.get(defaultFolderConfig.getPath() + File.separator + encryptedOldFolder + File.separator + fileModel.getFilename()), Paths.get(defaultFolderConfig.getPath() + File.separator + encryptedNewFolder + File.separator + fileModel.getFilename()), StandardCopyOption.REPLACE_EXISTING);
            FileModel newFileModel = new FileModel(fileModel.getFilename(), fileModel.getFiletype(), fileModel.getData(), encryptedNewFolder);
            fileRepository.save(newFileModel);
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
    }
    public void moveFile(UUID fileId, String oldFolder, String newFolder) {
        final String encryptedOldFolder = encryptionService.encryptPath(oldFolder);
        final String encryptedNewFolder = encryptionService.encryptPath(newFolder);
        FileModel fileModel = fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException("File not found with id " + fileId));
        try {
            Files.move(Paths.get(defaultFolderConfig.getPath() + File.separator + encryptedOldFolder + File.separator + fileModel.getFilename()), Paths.get(defaultFolderConfig.getPath() + File.separator + encryptedNewFolder + File.separator + fileModel.getFilename()), StandardCopyOption.REPLACE_EXISTING);
            fileModel.setFolderpath(encryptedNewFolder);
            fileRepository.save(fileModel);
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
    }

    public void changeName(UUID fileId, String folder, String newName) {
        final String encryptedFolder = encryptionService.encryptPath(folder);
        final String encryptedNewName = encryptionService.encrypt(newName);
        FileModel fileModel = fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException("File not found with id " + fileId));
        try {
            Files.move(Paths.get(defaultFolderConfig.getPath() + File.separator + encryptedFolder + File.separator + fileModel.getFilename()), Paths.get(defaultFolderConfig.getPath() + File.separator + encryptedFolder + File.separator + encryptedNewName), StandardCopyOption.REPLACE_EXISTING);
            fileModel.setFilename(encryptedNewName);
            fileRepository.save(fileModel);
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
    }

}
