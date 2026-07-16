package com.visage.cloudstorage.Exceptions;

public class ParentFileIsAlreadyExists extends RuntimeException {
    public ParentFileIsAlreadyExists(String message) {
        super(message);
    }
}
