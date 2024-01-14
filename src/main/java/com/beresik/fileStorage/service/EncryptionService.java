package com.beresik.fileStorage.service;

import com.beresik.fileStorage.config.FileEncryptConfig;
import com.beresik.fileStorage.exception.DecryptFileException;
import lombok.SneakyThrows;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@Service
public class EncryptionService {

    @Autowired
    private FileEncryptConfig fileEncryptConfig;

    @SneakyThrows
    public byte[] encrypt(byte[] data) {
        Key key = new SecretKeySpec(fileEncryptConfig.getKEY().getBytes(), fileEncryptConfig.getALGORITHM());
        Cipher c = Cipher.getInstance(fileEncryptConfig.getALGORITHM());
        c.init(Cipher.ENCRYPT_MODE,key);
        return c.doFinal(data);
    }

    @SneakyThrows
    public byte[] decrypt(byte[] encryptedData) {
        Key key = new SecretKeySpec(fileEncryptConfig.getKEY().getBytes(), fileEncryptConfig.getALGORITHM());
        Cipher c = Cipher.getInstance(fileEncryptConfig.getALGORITHM());
        c.init(Cipher.DECRYPT_MODE,key);
        return c.doFinal(encryptedData);
    }

    public String encrypt(String data) {
        if (data == null) {
            throw new IllegalArgumentException("Data to encrypt cannot be null");
        }
        try {
            Cipher cipher = Cipher.getInstance(fileEncryptConfig.getALGORITHM());
            SecretKeySpec key = new SecretKeySpec(fileEncryptConfig.getKEY().getBytes(), fileEncryptConfig.getALGORITHM());
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.encodeBase64String(encrypted).replace("/", "_");
        } catch (Exception e) {
            throw new DecryptFileException("Error encrypting data", e);
        }
    }

    public String decrypt(String encryptedData) {
        if (encryptedData == null) {
            throw new IllegalArgumentException("Data to decrypt cannot be null");
        }
        try {
            Cipher cipher = Cipher.getInstance(fileEncryptConfig.getALGORITHM());
            SecretKeySpec key = new SecretKeySpec(fileEncryptConfig.getKEY().getBytes(), fileEncryptConfig.getALGORITHM());
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] original = cipher.doFinal(Base64.decodeBase64(encryptedData.replace("_", "/")));
            return new String(original);
        } catch (Exception e) {
            return encryptedData;
        }
    }

    public String decryptPath(String encryptedPath) {
        return Arrays.stream(encryptedPath.split("/"))
                .map(this::decrypt)
                .collect(Collectors.joining("/"));
    }

    public String encryptPath(String path) {
        return Arrays.stream(path.split("/"))
                .map(this::encrypt)
                .collect(Collectors.joining("/"));
    }
}