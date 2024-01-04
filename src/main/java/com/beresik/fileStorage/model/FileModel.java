package com.beresik.fileStorage.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "fileuploaddownload")
public class FileModel {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")

    private UUID fileid;
    private String filename;
    private String filetype;

    private byte[] data;

    public FileModel() {}

    public FileModel(String fileName, String fileType) {
        this.filename = fileName;
        this.filetype = fileType;
    }

    public FileModel(String fileName, String fileType, byte[] data) {
        this.filename = fileName;
        this.filetype = fileType;
        this.data = data;
    }

    public InputStream getInputStreamData() {
        return new ByteArrayInputStream(data);
    }
}
