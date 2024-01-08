package com.beresik.fileStorage.web.mapper.FileNameMapper;

import com.beresik.fileStorage.model.FileModel;
import com.beresik.fileStorage.web.dto.FileNameDTO;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;


@UtilityClass
public class FileNameMapper {

    public FileNameDTO mapFileToFileNameDto(FileModel file) {
        FileNameDTO fileNameDTO = new FileNameDTO();
        fileNameDTO.setFileName(file.getFilename());
        fileNameDTO.setFileType(file.getFiletype());
        fileNameDTO.setId(file.getFileid());
        fileNameDTO.setFolderPath(file.getFolderpath());
        return fileNameDTO;
    }

    public List<FileNameDTO> mapFileNamesToDto(List<FileModel> files) {
        if(files == null) {
            return null;
        }
        List<FileNameDTO> fileNameDTOList = new ArrayList<FileNameDTO>(files.size());

        for(FileModel file : files) {
            fileNameDTOList.add(mapFileToFileNameDto(file));
        }
        return fileNameDTOList;
    }

}
