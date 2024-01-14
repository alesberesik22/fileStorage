package com.beresik.fileStorage.service;

import com.beresik.fileStorage.config.DefaultFolderConfig;
import com.beresik.fileStorage.exception.DirectoryReadException;
import com.beresik.fileStorage.exception.FolderNotFoundException;
import com.beresik.fileStorage.model.FileInfo;
import com.beresik.fileStorage.model.FileModel;
import com.beresik.fileStorage.model.FolderInfo;
import com.beresik.fileStorage.repository.FileRepository;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FolderService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private DefaultFolderConfig defaultFolderConfig;

    @Autowired
    private FileService fileService;

    @Autowired
    private EncryptionService encryptionService;

    public List<FileInfo> getAllFilesInFolder(String folderPath) {
        List<FileInfo> fileInfos = new ArrayList<>();
        final String encryptedFolder = encryptionService.encryptPath(folderPath);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(defaultFolderConfig.getPath() + File.separator + encryptedFolder))) {
            for (Path path : directoryStream) {
                if (!Files.isDirectory(path)) {
                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setFileName(path.getFileName().toString());
                    fileInfo.setFilePath(path.toString());
                    fileInfos.add(fileInfo);
                }
            }
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
        return fileInfos;
    }

    @SneakyThrows
    public void deleteFolder(String folderPath) {
        try {
            final String encryptedFolder = encryptionService.encryptPath(folderPath);
            Path pathToDelete = Paths.get(defaultFolderConfig.getPath() + File.separator + encryptedFolder);
            Files.walk(pathToDelete)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            fileRepository.findAll().stream()
                    .filter(fileModel -> fileModel.getFilename().startsWith(encryptedFolder + File.separator))
                    .forEach(fileModel -> fileRepository.delete(fileModel));
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
    }

    public void createFolder(String folder) {
        try {
            Files.createDirectories(Paths.get(defaultFolderConfig.getPath() + File.separator + folder));
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
    }

    public void updateFolderName(String folderPath, String newFolderName) {
        try {
            final String encryptedFolderPath = encryptionService.encryptPath(folderPath);
            final String encryptedNewFolderName = encryptionService.encrypt(newFolderName);
            Path oldFolderPath = Paths.get(defaultFolderConfig.getPath() + File.separator + encryptedFolderPath);
            Path newFolderPath = oldFolderPath.getParent().resolve(encryptedNewFolderName);
            Files.move(oldFolderPath, newFolderPath);
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
    }

    public List<FolderInfo> getAllFolders() {
        List<FolderInfo> folderInfos = new ArrayList<>();
        try {
            Files.walk(Paths.get(defaultFolderConfig.getPath()))
                    .filter(Files::isDirectory)
                    .forEach(path -> {
                        FolderInfo folderInfo = new FolderInfo();
                        folderInfo.setFolderPath(encryptionService.decryptPath(path.toString()));
                        folderInfo.setParentFolder(path.getParent() != null ? encryptionService.decryptPath(path.getParent().toString()) : null);
                        folderInfo.setChildFolders(getChildFolders(path));
                        folderInfos.add(folderInfo);
                    });
        } catch (IOException e) {
            // Log the exception and return an empty list
            System.err.println("Error reading directory: " + e.getMessage());
        }
        return folderInfos;
    }

    private List<String> getChildFolders(Path parentPath) {
        try {
            return Files.list(parentPath)
                    .filter(Files::isDirectory)
                    .map(path -> encryptionService.decryptPath(path.toString()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new DirectoryReadException("Error reading child directories", e);
        }
    }

    public void createNestedFolders(String folderPath) {
        String[] folders = folderPath.split("/");
        Arrays.stream(folders).forEach(System.out::println);
        String path = "";
        for (String folder : folders) {
            if (!path.isEmpty()) {
                path += "/";
            }
            String encryptedFolder = encryptionService.encrypt(folder);
            System.out.println("encrypted: " + encryptedFolder);
            path += encryptedFolder;
            createFolder(path);
        }
    }

    @SneakyThrows
    public void copyFolder(String sourceFolder, String destinationFolder) {
        final String destination = Objects.equals(destinationFolder, " ") ? defaultFolderConfig.getPath() : encryptionService.encryptPath(destinationFolder);
        Path srcPath = Paths.get(defaultFolderConfig.getPath(), encryptionService.encryptPath(sourceFolder));
        Path destPath = destinationFolder.trim().isEmpty()
                ? Paths.get(defaultFolderConfig.getPath())
                : Paths.get(defaultFolderConfig.getPath(), destination);
        System.out.println("srcPath: " + srcPath.toString());
        System.out.println("destPath: " + destPath.toString());

        Path srcParentPath = srcPath.getParent();
        Files.walkFileTree(srcPath, new SimpleFileVisitor<Path>() {
            @SneakyThrows
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                Path targetPath = destPath.resolve(srcParentPath.relativize(dir));
                if (!Files.exists(targetPath)) {
                    Files.createDirectories(targetPath);
                }
                return FileVisitResult.CONTINUE;
            }

            @SneakyThrows
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                Path targetPath = destPath.resolve(srcParentPath.relativize(file));
                Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                createNewFileModelInDatabase(file, targetPath);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    //TODO: refactor this method
    @SneakyThrows
    public void moveFolder(String sourceFolder, String destinationFolder) {
        final String encryptedSourceFolder = encryptionService.encryptPath(sourceFolder);
        final String encryptedDestinationFolder = destinationFolder.trim().isEmpty()
                ? ""
                : encryptionService.encryptPath(destinationFolder);
        Path srcPath = Paths.get(defaultFolderConfig.getPath(), encryptedSourceFolder);
        Path destPath = Paths.get(defaultFolderConfig.getPath(), encryptedDestinationFolder);

        // Check if source folder exists
        if (!Files.exists(srcPath)) {
            throw new IllegalArgumentException("Source folder does not exist: " + sourceFolder);
        }

        // Move the contents from the source folder to the destination folder
        Files.walk(srcPath).forEach(source -> {
            Path destination = Paths.get(destPath.toString(), source.toString()
                    .substring(srcPath.toString().length()));
            try {
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new DirectoryReadException("Error moving folder", e);
            }
        });

        updateFilePathsInDatabase(sourceFolder, destinationFolder);
    }

    private void updateFilePathsInDatabase(String oldPath, String newPath) {
        final String encryptedOldPath = encryptionService.encryptPath(oldPath);
        final String encryptedNewPath = encryptionService.encryptPath(newPath);
        List<FileModel> filesToUpdate = fileRepository.findAll().stream()
                .filter(fileModel -> fileModel.getFolderpath().startsWith(encryptedOldPath))
                .collect(Collectors.toList());

        filesToUpdate.forEach(fileModel -> {
            String newFolderPath = fileModel.getFolderpath().replaceFirst(encryptedOldPath, encryptedNewPath);
            fileModel.setFolderpath(newFolderPath);
            fileRepository.save(fileModel);
        });
    }

    private void createNewFileModelInDatabase(Path oldFilePath, Path newFilePath) {
        String parentFolderPath = oldFilePath.getParent().toString().replaceFirst(defaultFolderConfig.getPath(), "");
        FileModel oldFileModel = fileRepository.findByFilenameAndFolderpath(oldFilePath.getFileName().toString(), parentFolderPath);
        if (oldFileModel != null) {
            String newFilePathReplaced = newFilePath.getParent().toString().replaceFirst(defaultFolderConfig.getPath(), "");
            FileModel newFileModel = new FileModel();
            newFileModel.setFilename(encryptionService.encrypt(oldFileModel.getFilename()));
            newFileModel.setFiletype(encryptionService.encrypt(oldFileModel.getFiletype()));
            newFileModel.setData(encryptionService.encrypt(oldFileModel.getData()));
            newFileModel.setFolderpath(encryptionService.encryptPath(newFilePathReplaced));
            fileRepository.save(newFileModel);
        }
    }
}
