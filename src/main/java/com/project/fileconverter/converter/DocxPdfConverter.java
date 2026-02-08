package com.project.fileconverter.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.spire.doc.Document; // Dependency for .doc conversion
import com.spire.doc.FileFormat;

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
            InputStream docxInputStream;
            
            // 1. Check if the file is a legacy .doc file
            if (isLegacyDoc(source)) {
                // Convert .doc to .docx stream dynamically
                docxInputStream = convertDocToDocxStream(source);
            } else {
                // It is likely already .docx, load directly
                docxInputStream = new FileInputStream(source);
            }
            // InputStream templateInputStream;
            WordprocessingMLPackage wordMLPackage;
            FileOutputStream os;
			// templateInputStream = new FileInputStream(source);
            wordMLPackage = WordprocessingMLPackage.load(docxInputStream);
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

    private boolean isLegacyDoc(File file) {
        return file.getName().toLowerCase().endsWith(".doc");
    }

    private InputStream convertDocToDocxStream(File source) {
        // Load the legacy .doc file
        Document document = new Document();
        document.loadFromFile(source.getAbsolutePath());

        // Save it to a Byte Array Output Stream as .docx
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.saveToStream(baos, FileFormat.Docx);
        document.close();

        // Return the byte array as an Input Stream for Docx4j to read
        return new ByteArrayInputStream(baos.toByteArray());
    }

    @Override
    public boolean supports(String mimeType, String filename) {
        if (mimeType != null && (mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || mimeType.equals("application/msword"))) 
            return true;
        return filename != null && (filename.toLowerCase().endsWith(".docx") || filename.toLowerCase().endsWith(".doc"));
    }
}