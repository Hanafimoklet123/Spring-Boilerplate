package com.boilerplate.spring_boot.responsebuilder;

import com.boilerplate.spring_boot.commoncontracts.ServiceError;
import com.boilerplate.spring_boot.commoncontracts.ServiceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

public class ErrorResponseBuilder {
    public static <T> ResponseEntity<ServiceResponse<T>> buildSingleErrorResponse(String code, String entity, String message, T details, HttpStatus httpStatus) {
        ServiceError error = ServiceError.builder().code(code).entity(entity).message(message).details(details).build();

        List<ServiceError> errors = new ArrayList<>();
        errors.add(error);

        ServiceResponse<T> response = ServiceResponse.<T>builder()
                .success(false)
                .errors(errors)
                .build();

        return new ResponseEntity<>(response, httpStatus);
    }

    public static <T> ResponseEntity<ServiceResponse<T>> buildMultipleErrorsResponse(List<ServiceError> errors, HttpStatus httpStatus) {
        ServiceResponse<T> response = ServiceResponse.<T>builder()
                .success(false)
                .errors(errors)
                .build();

        return new ResponseEntity<>(response, httpStatus);
    }
}
