package com.project.fileconverter.converter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.Phrase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class XlsxPdfConverter implements PdfConverter {
    @Override
    public void convert(File source, File dest) throws IOException {
        try (InputStream input_document = Files.newInputStream(source.toPath())) {
            Workbook workbook = WorkbookFactory.create(input_document);
            Sheet sheet = workbook.getSheetAt(0);
            // Determine max columns
            int maxCols = 0;
            for (Row row : sheet) {
                if (row.getLastCellNum() > maxCols) maxCols = row.getLastCellNum();
            }
            Document pdfDoc = new Document();
            PdfWriter.getInstance(pdfDoc, new java.io.FileOutputStream(dest));
            pdfDoc.open();
            PdfPTable table = new PdfPTable(maxCols > 0 ? maxCols : 1);
            for (Row row : sheet) {
                for (int cn = 0; cn < maxCols; cn++) {
                    Cell cell = row.getCell(cn);
                    String value = (cell == null) ? "" : cell.toString();
                    table.addCell(new PdfPCell(new Phrase(value)));
                }
            }
            pdfDoc.add(table);
            pdfDoc.close();
        } catch (IOException | DocumentException e) {
            throw new RuntimeException("Error converting Excel to PDF", e);
        }
    }

    @Override
    public boolean supports(String mimeType, String filename) {
        if (mimeType != null) {
            if (mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    || mimeType.equals("application/vnd.ms-excel")) return true;
        }
        if (filename != null) {
            String lower = filename.toLowerCase();
            return lower.endsWith(".xlsx") || lower.endsWith(".xls");
        }
        return false;
    }
}
