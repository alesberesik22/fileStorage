package com.beresik.fileStorage.exception;

public class DecryptFileException extends RuntimeException{
    public DecryptFileException(String message) {
        super(message);
    }
    public DecryptFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
