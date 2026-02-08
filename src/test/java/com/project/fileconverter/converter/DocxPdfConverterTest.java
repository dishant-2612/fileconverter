package com.project.fileconverter.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DocxPdfConverterTest {

    @Autowired
    private DocxPdfConverter docxPdfConverter;
    
    private File testDocxFile;
    private File outputPdfFile;

    @BeforeEach
    public void setUp() throws Exception {
        // Create a temporary test DOCX file
        testDocxFile = Files.createTempFile("test", ".docx").toFile();
        outputPdfFile = Files.createTempFile("output", ".pdf").toFile();
        
        // Create a sample DOCX document using docx4j
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
        MainDocumentPart mainPart = wordMLPackage.getMainDocumentPart();
        
        // Add some sample text
        P paragraph = new P();
        R run = new R();
        Text text = new Text();
        text.setValue("Hello World! This is a test DOCX document.");
        run.getContent().add(text);
        paragraph.getContent().add(run);
        mainPart.getContent().add(paragraph);
        
        // Add another paragraph
        paragraph = new P();
        run = new R();
        text = new Text();
        text.setValue("Testing docx4j DOCX to PDF conversion with formatting preservation.");
        run.getContent().add(text);
        paragraph.getContent().add(run);
        mainPart.getContent().add(paragraph);
        
        // Save the DOCX
        wordMLPackage.save(testDocxFile);
    }

    @Test
    public void testDocxToPdfConversion() throws Exception {
        assertNotNull(testDocxFile);
        assertTrue(testDocxFile.exists());
        
        // Convert DOCX to PDF
        docxPdfConverter.convert(testDocxFile, outputPdfFile);
        
        // Verify PDF was created
        assertTrue(outputPdfFile.exists(), "PDF file was not created");
        assertTrue(outputPdfFile.length() > 0, "PDF file is empty");
        
        System.out.println("✓ DOCX to PDF conversion successful!");
        System.out.println("  Input DOCX: " + testDocxFile.getAbsolutePath() + " (" + testDocxFile.length() + " bytes)");
        System.out.println("  Output PDF: " + outputPdfFile.getAbsolutePath() + " (" + outputPdfFile.length() + " bytes)");
    }

    @Test
    public void testSupportsDocx() {
        assertTrue(docxPdfConverter.supports(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "test.docx"
        ));
    }

    @Test
    public void testSupportsDocxByFilename() {
        assertTrue(docxPdfConverter.supports(null, "document.docx"));
    }

    @Test
    public void testDoesNotSupportPdf() {
        assertFalse(docxPdfConverter.supports("application/pdf", "document.pdf"));
    }
}
