package com.project.fileconverter.model;

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
