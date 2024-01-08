package com.beresik.fileStorage.service;

import com.beresik.fileStorage.config.FileEncryptConfig;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Arrays;

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

    @SneakyThrows
    public String encrypt(String data) {
        Key key = new SecretKeySpec(fileEncryptConfig.getKEY().getBytes(), fileEncryptConfig.getALGORITHM());
        Cipher c = Cipher.getInstance(fileEncryptConfig.getALGORITHM());
        c.init(Cipher.ENCRYPT_MODE,key);
        return Arrays.toString(c.doFinal(data.getBytes()));
    }

    @SneakyThrows
    public String decrypt(String encryptedData) {
        Key key = new SecretKeySpec(fileEncryptConfig.getKEY().getBytes(), fileEncryptConfig.getALGORITHM());
        Cipher c = Cipher.getInstance(fileEncryptConfig.getALGORITHM());
        c.init(Cipher.DECRYPT_MODE,key);
        return Arrays.toString(c.doFinal(encryptedData.getBytes()));
    }
}
