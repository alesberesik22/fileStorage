package com.beresik.fileStorage.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FolderInfo {

    private String folderPath;
    private String parentFolder;
    private List<String> childFolders;
}
