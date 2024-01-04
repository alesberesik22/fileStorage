package com.beresik.fileStorage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "default-folder")
@Getter
@Setter
public class DefaultFolderConfig {
    private String path;
}
