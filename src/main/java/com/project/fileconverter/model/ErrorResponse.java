package com.project.fileconverter.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorResponse {
    String error;
    String message;
    int status;
}
