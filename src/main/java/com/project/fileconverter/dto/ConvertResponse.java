package com.project.fileconverter.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConvertResponse {
    String id;
    String viewUrl;
    String downloadUrl;
    String originalFilename;
    String contentType;
}
