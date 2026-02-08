package com.project.fileconverter.converter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import org.jsoup.nodes.Document;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

@Component
public class HtmlPdfConverter implements PdfConverter {

    @Override
    public void convert(File source, File dest) throws IOException {
        try {
            // 1. Read the raw HTML string
            String rawHtml = Files.readString(source.toPath());

            // 2. Use Jsoup to convert messy HTML -> Valid XHTML
            Document doc = Jsoup.parse(rawHtml);
            doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml); // CRITICAL: forces self-closing tags (e.g. <br/>)
            
            // 3. Render the clean XHTML to PDF
            try (OutputStream os = Files.newOutputStream(dest.toPath())) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                
                // Use doc.html() to get the sanitized content
                builder.withHtmlContent(doc.html(), source.toURI().toString()); 
                builder.toStream(os);
                builder.run();
            }
        } catch (Exception e) {
            throw new IOException("HTML to PDF conversion failed", e);
        }
    }

    @Override
    public boolean supports(String mimeType, String filename) {
        if (mimeType != null && mimeType.equals("text/html")) return true;
        return filename != null && (filename.toLowerCase().endsWith(".html") || filename.toLowerCase().endsWith(".htm"));
    }
}
