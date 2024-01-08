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
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FolderService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private DefaultFolderConfig defaultFolderConfig;

    @Autowired
    private FileService fileService;

    public List<FileInfo> getAllFilesInFolder(String folderPath) {
        List<FileInfo> fileInfos = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(defaultFolderConfig.getPath() + File.separator + folderPath))) {
            for (Path path : directoryStream) {
                FileInfo fileInfo = new FileInfo();
                fileInfo.setFileName(path.getFileName().toString());
                fileInfo.setFilePath(path.toString());
                fileInfos.add(fileInfo);
            }
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
        return fileInfos;
    }

    public void deleteFolder(String folderPath) {
        try {
            Path pathToDelete = Paths.get(defaultFolderConfig.getPath() + File.separator + folderPath);
            Files.walk(pathToDelete)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            fileRepository.findAll().stream()
                    .filter(fileModel -> fileModel.getFilename().startsWith(folderPath + File.separator))
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
            Path oldFolderPath = Paths.get(defaultFolderConfig.getPath() + File.separator + folderPath);
            Path newFolderPath = oldFolderPath.getParent().resolve(newFolderName);
            Files.move(oldFolderPath, newFolderPath);
        } catch (IOException ex) {
            throw new FolderNotFoundException("Folder does not exist", ex);
        }
    }

    public List<FolderInfo> getAllFolders() {
        try {
            List<FolderInfo> folderInfos = new ArrayList<>();
            Files.walk(Paths.get(defaultFolderConfig.getPath()))
                    .filter(Files::isDirectory)
                    .forEach(path -> {
                        FolderInfo folderInfo = new FolderInfo();
                        folderInfo.setFolderPath(path.toString());
                        folderInfo.setParentFolder(path.getParent() != null ? path.getParent().toString() : null);
                        folderInfo.setChildFolders(getChildFolders(path));
                        folderInfos.add(folderInfo);
                    });
            return folderInfos;
        } catch (IOException e) {
            throw new DirectoryReadException("Error reading directory", e);
        }
    }

    private List<String> getChildFolders(Path parentPath) {
        try {
            return Files.list(parentPath)
                    .filter(Files::isDirectory)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new DirectoryReadException("Error reading child directories", e);
        }
    }

    public void createNestedFolders(String folderPath) {
        String[] folders = folderPath.split("/");
        String path = "";
        for (String folder : folders) {
            path += folder;
            createFolder(path);
            path += "/";
        }
    }

    @SneakyThrows
    public void copyFolder(String sourceFolder, String destinationFolder) {
        final String destination = Objects.equals(destinationFolder, " ") ? defaultFolderConfig.getPath() : destinationFolder;
        Path srcPath = Paths.get(defaultFolderConfig.getPath(), sourceFolder);
        Path destPath = Paths.get(defaultFolderConfig.getPath(), destination);

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

    @SneakyThrows
    public void moveFolder(String sourceFolder, String destinationFolder) {
        Path srcPath = Paths.get(defaultFolderConfig.getPath(), sourceFolder);
        Path destPath = Paths.get(defaultFolderConfig.getPath(), destinationFolder);

        Files.walk(srcPath).forEach(source -> {
            Path destination = Paths.get(destPath.toString(), source.toString()
                    .substring(srcPath.toString().length()));
            try {
                Files.move(source, destination);
            } catch (IOException e) {
                throw new DirectoryReadException("Error moving folder", e);
            }
        });

        updateFilePathsInDatabase(sourceFolder, destinationFolder);
    }

    private void updateFilePathsInDatabase(String oldPath, String newPath) {
        List<FileModel> filesToUpdate = fileRepository.findAll().stream()
                .filter(fileModel -> fileModel.getFolderpath().startsWith(oldPath))
                .collect(Collectors.toList());

        filesToUpdate.forEach(fileModel -> {
            String newFolderPath = fileModel.getFolderpath().replaceFirst(oldPath, newPath);
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
            newFileModel.setFilename(oldFileModel.getFilename());
            newFileModel.setFiletype(oldFileModel.getFiletype());
            newFileModel.setData(oldFileModel.getData());
            newFileModel.setFolderpath(newFilePathReplaced);
            fileRepository.save(newFileModel);
        }
    }
}
