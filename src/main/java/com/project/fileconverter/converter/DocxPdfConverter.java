package com.project.fileconverter.converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * DOCX to PDF converter using docx4j library.
 * Preserves formatting, styles, fonts, tables, and structure from the DOCX document.
 * Uses FOP (Formatting Objects Processor) for high-fidelity PDF conversion.
 */
@Component
public class DocxPdfConverter implements PdfConverter {
    
    private static final Logger log = LoggerFactory.getLogger(DocxPdfConverter.class);

    @Override
    public void convert(File source, File dest) throws IOException {
        try {
            InputStream templateInputStream;
            WordprocessingMLPackage wordMLPackage;
            FileOutputStream os;
			templateInputStream = new FileInputStream(source);
            wordMLPackage = WordprocessingMLPackage.load(templateInputStream);
            String outputfilepath = dest.getAbsolutePath();
            File outputFile = new File(outputfilepath);
            outputFile.getParentFile().mkdirs();
            os = new FileOutputStream(outputfilepath);
            Docx4J.toPDF(wordMLPackage,os);
            os.flush();
            os.close();
        } catch (Exception e) {
            log.error("Failed to convert DOCX to PDF: {}", e.getMessage(), e);
            throw new IOException("DOCX to PDF conversion failed: " + e.getMessage(), e);
        }
    }
    @Override
    public boolean supports(String mimeType, String filename) {
        if (mimeType != null && mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) 
            return true;
        return filename != null && filename.toLowerCase().endsWith(".docx");
    }
}