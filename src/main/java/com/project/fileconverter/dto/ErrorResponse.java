package com.project.fileconverter.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorResponse {
    String error;
    String message;
    int status;
}
