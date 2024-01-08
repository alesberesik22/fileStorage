package com.beresik.fileStorage.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class FileNameDTO {

    private String fileName;
    private String fileType;
    private UUID id;
    private String folderPath;
}
