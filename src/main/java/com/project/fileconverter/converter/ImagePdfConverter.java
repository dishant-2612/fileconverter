package com.project.fileconverter.converter;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

@Component
public class ImagePdfConverter implements PdfConverter {

    @Override
    public void convert(File source, File dest) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDImageXObject pdImage = PDImageXObject.createFromFileByContent(source, doc);
            PDPage page = new PDPage(new PDRectangle(pdImage.getWidth(), pdImage.getHeight()));
            doc.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.drawImage(pdImage, 0, 0, pdImage.getWidth(), pdImage.getHeight());
            }
            doc.save(dest);
        }
    }

    @Override
    public boolean supports(String mimeType, String filename) {
        return mimeType != null && mimeType.startsWith("image/");
    }
}
