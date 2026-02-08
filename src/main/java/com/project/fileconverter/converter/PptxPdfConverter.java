package com.project.fileconverter.converter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import org.springframework.stereotype.Component;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

@Component
public class PptxPdfConverter implements PdfConverter {

    @Override
    public void convert(File source, File dest) throws IOException {
        try (InputStream inputStream = Files.newInputStream(source.toPath());
            //  XMLSlideShow ppt = new XMLSlideShow(inputStream);
            SlideShow<?, ?> ppt = SlideShowFactory.create(inputStream);
             FileOutputStream outputStream = new FileOutputStream(dest)) {

            // Get slide dimensions
            Dimension pageSize = ppt.getPageSize();
            
            // Initialize iText Document with PPT's slide size
            Document pdfDocument = new Document(new Rectangle((float) pageSize.getWidth(), (float) pageSize.getHeight()), 0, 0, 0, 0);
            PdfWriter.getInstance(pdfDocument, outputStream);
            pdfDocument.open();

            int slideNumber = 1;

            for (Slide<?, ?> slide : ppt.getSlides()) {
                // 1. Create an image buffer for the slide
                BufferedImage img = new BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = img.createGraphics();

                // 2. Clear the drawing area (white background)
                graphics.setPaint(Color.white);
                graphics.fill(new Rectangle2D.Float(0, 0, pageSize.width, pageSize.height));

                // 3. Set Rendering Hints (Quality)
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                try {
                    slide.draw(graphics); 
                } catch (Exception e) {
                    // 4. Handle the specific EMF/Rendering error
                    System.err.println("Failed to render Slide " + slideNumber + ": " + e.getMessage());
                    
                    // Optional: Draw an error message on the PDF page so you know which one failed
                    graphics.setPaint(Color.RED);
                    graphics.drawRect(0, 0, pageSize.width - 1, pageSize.height - 1);
                    graphics.drawString("Error rendering Slide " + slideNumber, 50, 50);
                    graphics.drawString("Cause: " + e.getClass().getSimpleName(), 50, 80);
                }

                // 5. Add the resulting image (either the slide or the error placeholder) to the PDF
                try {
                    Image rendererMetafile = Image.getInstance(img, null);
                    pdfDocument.add(rendererMetafile);
                } catch (Exception pdfEx) {
                    pdfEx.printStackTrace();
                }        
                graphics.dispose();
                slideNumber++;
            }
            pdfDocument.close();
            System.out.println("PDF created successfully at: " + dest.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Error during conversion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean supports(String mimeType, String filename) {
        if (mimeType != null && mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")) 
            return true;
        return filename != null && (filename.toLowerCase().endsWith(".pptx") || filename.toLowerCase().endsWith(".ppt"));
    }
}
