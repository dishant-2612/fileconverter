package com.project.fileconverter.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

@Component
public class TextPdfConverter implements PdfConverter {

    @Override
    public void convert(File source, File dest) throws IOException {
        String text = Files.readString(source.toPath());
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(50, 700);
                BufferedReader reader = new BufferedReader(new StringReader(text));
                String line;
                float leading = 14f;
                while ((line = reader.readLine()) != null) {
                    if (line.length() == 0) {
                        content.newLineAtOffset(0, -leading);
                        continue;
                    }
                    // naive wrap at 90 chars
                    int max = 90;
                    for (int start = 0; start < line.length(); start += max) {
                        int end = Math.min(line.length(), start + max);
                        content.showText(line.substring(start, end));
                        content.newLineAtOffset(0, -leading);
                    }
                }
                content.endText();
            }
            doc.save(dest);
        }
    }

    @Override
    public boolean supports(String mimeType, String filename) {
        // Only handle plain text-like files (don't treat binary office files as text)
        if (mimeType != null && mimeType.startsWith("text/")) return true;
        if (filename != null) {
            String lower = filename.toLowerCase();
            return lower.endsWith(".txt") || lower.endsWith(".csv") || lower.endsWith(".log");
        }
        return false;
    }
}
