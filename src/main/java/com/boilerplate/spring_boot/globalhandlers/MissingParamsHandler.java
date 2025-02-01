package com.boilerplate.spring_boot.globalhandlers;

import com.boilerplate.spring_boot.commoncontracts.ErrorCodes;
import com.boilerplate.spring_boot.commoncontracts.ServiceResponse;
import com.boilerplate.spring_boot.responsebuilder.ErrorResponseBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class MissingParamsHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ServiceResponse<Object>> handleMissingQueryParams(MissingServletRequestParameterException ex) {
        String name = ex.getParameterName();

        return ErrorResponseBuilder.buildSingleErrorResponse(ErrorCodes.INVALID_REQUEST, name, "Required query parameter is missing", null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ServiceResponse<Object>> handleHTTPBodyNotReadable(HttpMessageNotReadableException ex) {
        log.error("caught error in handleHTTPBodyNotReadable", ex);
        return ErrorResponseBuilder.buildSingleErrorResponse(ErrorCodes.INVALID_REQUEST, "", "Something went wrong while reading request body. Please check that it's a valid JSON and try again.", null, HttpStatus.BAD_REQUEST);
    }
}
