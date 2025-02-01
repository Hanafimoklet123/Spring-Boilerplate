package com.boilerplate.spring_boot.responsebuilder;

import com.boilerplate.spring_boot.commoncontracts.ServiceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class SuccessResponseBuilder {
    public static <T> ResponseEntity<ServiceResponse<T>> buildSuccessResponse(T data, HttpStatus httpStatus) {
        ServiceResponse<T> response = ServiceResponse.<T>builder()
                .success(true)
                .data(data)
                .build();

        return new ResponseEntity<>(response, httpStatus);
    }
}
