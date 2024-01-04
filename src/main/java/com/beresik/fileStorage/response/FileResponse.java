package com.beresik.fileStorage.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileResponse {

    String fileName;
    String fileType;
    String fileUri;

    public FileResponse(String fileName, String fileType, String fileUri) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileUri = fileUri;
    }
}
