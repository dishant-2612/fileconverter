package com.project.fileconverter.model;

import java.nio.file.Path;
import lombok.Value;

@Value
public class StoredFile {
    String id;
    String originalFilename;
    String contentType;
    Path originalPath;
    Path pdfPath;
}
