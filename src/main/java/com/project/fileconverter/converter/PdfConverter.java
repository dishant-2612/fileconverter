package com.project.fileconverter.converter;

import java.io.File;
import java.io.IOException;

public interface PdfConverter {
    /**
     * Convert source file to PDF.
     * @param source the source file
     * @param dest the destination PDF file
     * @throws IOException if conversion fails
     */
    void convert(File source, File dest) throws IOException;

    /**
     * Check if this converter handles the given MIME type.
     */
    boolean supports(String mimeType, String filename);
}
