package com.beresik.fileStorage.web.controller;

import com.beresik.fileStorage.model.FileModel;
import com.beresik.fileStorage.response.FileResponse;
import com.beresik.fileStorage.service.FileService;
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
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload/{folder}")
    public ResponseEntity<FileResponse> uploadFile(@PathVariable String folder ,@RequestParam("file") MultipartFile file) {
        FileModel file1 = fileService.saveFile(file, folder);
        String fileUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/downloadFile/"+folder+"/").path(file1.getFileid().toString()).toUriString();
        return ResponseEntity.ok(new FileResponse(file1.getFilename(), fileUri, file.getContentType()));
    }

    @PostMapping("/upload/multiple/{folder}")
    public ResponseEntity<List<FileResponse>> uploadMultipleFiles(@PathVariable String folder,@RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.ok(Arrays.asList(files).stream().map(file -> uploadFile(folder,file).getBody()).toList());
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable UUID fileId) throws IOException {
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
    public ResponseEntity<List<FileModel>> getAllFiles() {
        List<FileModel> fileModels = fileService.getAllFiles();
        return ResponseEntity.ok(fileModels);
    }

    @GetMapping("/files/{folder}")
    public ResponseEntity<List<String>> getAllFilesInFolder(@PathVariable String folder) {
        List<String> fileModels = fileService.getAllFilesInFolder(folder);
        return ResponseEntity.ok(fileModels);
    }
}
