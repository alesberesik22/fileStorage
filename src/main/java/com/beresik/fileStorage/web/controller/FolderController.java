package com.beresik.fileStorage.web.controller;

import com.beresik.fileStorage.model.FileInfo;
import com.beresik.fileStorage.model.FileModel;
import com.beresik.fileStorage.model.FolderInfo;
import com.beresik.fileStorage.repository.FileRepository;
import com.beresik.fileStorage.service.FolderService;
import com.beresik.fileStorage.web.dto.FileNameDTO;
import com.beresik.fileStorage.web.mapper.FileNameMapper.FileNameMapper;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/folder")
public class FolderController {

    private final FolderService folderService;
    private final FileRepository fileRepository;

    public FolderController(FolderService folderService, FileRepository fileRepository) {
        this.folderService = folderService;
        this.fileRepository = fileRepository;
    }

    @GetMapping()
    public ResponseEntity<List<FileNameDTO>> getAllFilesInFolder(@RequestHeader String folder) {
        List<FileInfo> fileInfos = folderService.getAllFilesInFolder(folder);
        List<FileModel> fileModels = fileRepository.findAll().stream()
                .filter(fileModel -> fileInfos.stream()
                        .anyMatch(fileInfo -> fileInfo.getFilePath().contains(fileModel.getFolderpath())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(FileNameMapper.mapFileNamesToDto(fileModels));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFolder(@RequestBody String folderPath){
        folderService.deleteFolder(folderPath);
        return ResponseEntity.ok("Folder deleted successfully");
    }

    @PostMapping("/create/{folder}")
    public ResponseEntity<String> createFolder(@PathVariable String folder){
        folderService.createFolder(folder);
        return ResponseEntity.ok("Folder created successfully");
    }

    @PutMapping("/update/{newFolderName}")
    public ResponseEntity<String> updateFolder(@RequestBody String path, @PathVariable String newFolderName){
        folderService.updateFolderName(path, newFolderName);
        return ResponseEntity.ok("Folder updated successfully");
    }

    @GetMapping("/folders")
    public ResponseEntity<List<FolderInfo>> getAllFolders() {
        return ResponseEntity.ok(folderService.getAllFolders());
    }

    @PostMapping("/create/nested/")
    public ResponseEntity<String> createNestedFolder(@RequestBody String folder){
        folderService.createNestedFolders(folder);
        return ResponseEntity.ok("Nested folder created successfully");
    }

    @PostMapping("/copy")
    @SneakyThrows
    public ResponseEntity<String> copyFolder(@RequestHeader String sourceFolder, @RequestHeader String destinationFolder) {
        folderService.copyFolder(sourceFolder, destinationFolder);
        return ResponseEntity.ok("Folder copied successfully");
    }

    @PostMapping("/move")
    @SneakyThrows
    public ResponseEntity<String> moveFolder(@RequestHeader String sourceFolder, @RequestHeader String destinationFolder) {
        folderService.moveFolder(sourceFolder, destinationFolder);
        return ResponseEntity.ok("Folder moved successfully");
    }
}
