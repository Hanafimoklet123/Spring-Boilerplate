package com.boilerplate.spring_boot.commoncontracts;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode
public class ServiceResponse<T> {
    private boolean success;
    private T data;
    private List<ServiceError>errors;
}
