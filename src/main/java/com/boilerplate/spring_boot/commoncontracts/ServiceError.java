package com.boilerplate.spring_boot.commoncontracts;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode
public class ServiceError {
    private String code;
    private String entity;
    private String message;
    private String details;
}
