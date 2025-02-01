package com.boilerplate.spring_boot.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@SuppressWarnings({"PMD.MissingSerialVersionUID", "PMD.UnusedPrivateField"})
@EqualsAndHashCode
public class UserServiceException extends Exception {
    private String code;
    private String entity;
    private String message;

    public UserServiceException(String code, String entity, String message) {
        super(message);
        this.code = code;
        this.entity = entity;
        this.message = message;
    }
}
