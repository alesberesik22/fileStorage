package com.beresik.fileStorage.exception;

public class DirectoryReadException extends RuntimeException {
    public DirectoryReadException(String message) {
        super(message);
    }
    public DirectoryReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
