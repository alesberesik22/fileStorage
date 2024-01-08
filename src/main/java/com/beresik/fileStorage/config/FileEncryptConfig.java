package com.beresik.fileStorage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file-encrypt")
@Getter
@Setter
public class FileEncryptConfig {
    private String ALGORITHM;
    private String KEY;
}
