package com.boilerplate.spring_boot.globalhandlers;

import com.boilerplate.spring_boot.commoncontracts.ServiceError;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GenericErrorResponse {
    private boolean success;
    private List<ServiceError> errors;
}
