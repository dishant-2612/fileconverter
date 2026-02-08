package com.project.fileconverter.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.project.fileconverter.converter.PdfConverter;
import com.project.fileconverter.model.StoredFile;

@Service
public class ConverterService {

    private static final Logger log = LoggerFactory.getLogger(ConverterService.class);

    private final Path storageDir;
    private final Tika tika = new Tika();
    private final List<PdfConverter> converters;

    public ConverterService(@Value("${file.storage.location:storage}") String storageLocation,
                            List<PdfConverter> converters) throws IOException {
        this.storageDir = Paths.get(storageLocation).toAbsolutePath();
        Files.createDirectories(this.storageDir);
        this.converters = converters;
    }
    
    public StoredFile storeAndConvert(MultipartFile multipart) throws IOException {
        String id = UUID.randomUUID().toString();
        String originalFilename = multipart.getOriginalFilename() != null ? multipart.getOriginalFilename() : id;
        String ext = FilenameUtils.getExtension(originalFilename);

        // Store original file
        Path originalPath = storageDir.resolve(id + "-orig" + (ext.isEmpty() ? "" : "." + ext));
        try (InputStream in = multipart.getInputStream()) {
            Files.copy(in, originalPath);
        }

        // Detect MIME type
        String detected = tika.detect(originalPath.toFile());
        Path pdfPath = storageDir.resolve(id + ".pdf");

        boolean converted = false;

        // Handle PDF: just copy
        if ("application/pdf".equalsIgnoreCase(detected)) {
            Files.copy(originalPath, pdfPath);
            converted = true;
            log.info("File is already PDF, copied: {}", originalFilename);
        }

        // If still not converted, try strategy-based converters (docx/xlsx/pptx readers, etc.)
        if (!converted) {
            var converterOpt = converters.stream()
                    .filter(c -> c.supports(detected, originalFilename))
                    .findFirst();
            if (converterOpt.isPresent()) {
                PdfConverter converter = converterOpt.get();
                try {
                    log.info("Converting {} using {}", originalFilename, converter.getClass().getSimpleName());
                    converter.convert(originalPath.toFile(), pdfPath.toFile());
                    converted = true;
                    log.info("Successfully converted {} using {}", originalFilename, converter.getClass().getSimpleName());
                } catch (IOException e) {
                    log.error("Conversion failed for {}: {}", originalFilename, e.getMessage(), e);
                    throw e;
                }
            } else {
                throw new IOException("No converter available for " + detected + " (filename=" + originalFilename + ")");
            }
        }

        return new StoredFile(id, originalFilename, detected, originalPath, pdfPath);
    }

    public Path getPdfPath(String id) {
        Path p = storageDir.resolve(id + ".pdf");
        return Files.exists(p) ? p : null;
    }

    public Path getOriginalPath(String id) throws IOException {
        try (var s = Files.list(storageDir)) {
            return s.filter(p -> p.getFileName().toString().startsWith(id + "-orig")).findFirst().orElse(null);
        }
    }
}
