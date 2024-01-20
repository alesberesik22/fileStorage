package com.beresik.fileStorage.web.controller;

import com.beresik.fileStorage.model.FileModel;
import com.beresik.fileStorage.response.FileResponse;
import com.beresik.fileStorage.service.FileService;
import com.beresik.fileStorage.service.FolderService;
import com.beresik.fileStorage.web.dto.FileNameDTO;
import com.beresik.fileStorage.web.mapper.FileNameMapper.FileNameMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.sql.SQLException;

@RestController
@RequestMapping("/fileStorage/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(@RequestParam String folder ,@RequestParam("file") MultipartFile file) {
        FileModel file1 = fileService.saveFile(file, folder);
        String fileUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/downloadFile/"+folder+"/").path(file1.getFileid().toString()).toUriString();
        return ResponseEntity.ok(new FileResponse(file1.getFilename(), fileUri, file.getContentType()));
    }

    @PostMapping("/upload/multiple")
    public ResponseEntity<List<FileResponse>> uploadMultipleFiles(@RequestParam("folder") String folder,@RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.ok(Arrays.asList(files).stream().map(file -> uploadFile(folder,file).getBody()).toList());
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable UUID fileId){
        FileModel fileModel = fileService.getFile(fileId);
        byte[] data = fileModel.getData();
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileModel.getFilename())
                .contentType(MediaType.parseMediaType(fileModel.getFiletype()))
                .contentLength(data.length)
                .body(resource);
    }

    @GetMapping("/files")
    public ResponseEntity<List<FileNameDTO>> getAllFiles() {
        List<FileModel> fileModels = fileService.getAllFiles();
        return ResponseEntity.ok(FileNameMapper.mapFileNamesToDto(fileModels));
    }

    @DeleteMapping("/delete/{fileId}")
    public ResponseEntity<String> deleteFile(@PathVariable UUID fileId, @RequestBody String folder){
        fileService.deleteFile(fileId, folder);
        return ResponseEntity.ok("File deleted successfully");
    }

    @PutMapping("copy/{fileId}")
    public ResponseEntity<String> copyFile(@PathVariable UUID fileId, @RequestHeader String oldFolder, @RequestHeader String newFolder) {
        fileService.copyFile(fileId, oldFolder, newFolder);
        return ResponseEntity.ok("File copied successfully");
    }

    @PutMapping("move/{fileId}")
    public ResponseEntity<String> moveFile(@PathVariable UUID fileId, @RequestHeader String oldFolder, @RequestHeader String newFolder) {
        fileService.moveFile(fileId, oldFolder, newFolder);
        return ResponseEntity.ok("File moved successfully");
    }

    @PutMapping("change-name/{fileId}/{newName}")
    public ResponseEntity<String> changeFileName(@PathVariable UUID fileId, @RequestHeader String folder, @PathVariable String newName) {
        fileService.changeName(fileId, folder, newName);
        return ResponseEntity.ok("File name changed successfully");
    }
}
