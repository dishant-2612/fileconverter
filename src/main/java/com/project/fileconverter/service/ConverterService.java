package com.project.fileconverter.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    private final boolean libreEnabled;
    private final String librePath;
    private boolean libreAvailable = false;

    public ConverterService(@Value("${file.storage.location:storage}") String storageLocation,
                            @Value("${file.converter.libreoffice.enabled:false}") boolean libreEnabled,
                            @Value("${file.converter.libreoffice.path:soffice}") String librePath,
                            List<PdfConverter> converters) throws IOException {
        this.storageDir = Paths.get(storageLocation).toAbsolutePath();
        Files.createDirectories(this.storageDir);
        this.libreEnabled = libreEnabled;
        this.librePath = librePath;
        this.converters = converters;
        
        // Validate LibreOffice availability if enabled
        if (libreEnabled) {
            this.libreAvailable = checkLibreOfficeAvailable();
            if (libreAvailable) {
                log.info("LibreOffice found at: {}", librePath);
            } else {
                log.warn("LibreOffice enabled but not found at: {}. Using fallback converters.", librePath);
            }
        }
    }

    private boolean checkLibreOfficeAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(librePath, "--version");
            pb.environment().put("SAL_HEADLESS", "1");
            Process p = pb.start();
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
            p.destroy();
            return finished && p.exitValue() == 0;
        } catch (Exception e) {
            log.debug("LibreOffice check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Clean up stale LibreOffice lock files that may prevent conversion
     * Important for Docker environments where locks may persist
     */
    private void cleanupLibreOfficeLockFiles() {
        try {
            // Look for .~lock* files in storage directory
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDir, ".~lock*")) {
                for (Path entry : stream) {
                    try {
                        Files.deleteIfExists(entry);
                        log.debug("Cleaned up stale LibreOffice lock file: {}", entry.getFileName());
                    } catch (IOException e) {
                        log.debug("Could not delete lock file {}: {}", entry.getFileName(), e.getMessage());
                    }
                }
            }
            
            // Also clean up LibreOffice temp directories
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDir, "libreconv-*")) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        // Only delete if directory is old (more than 5 minutes)
                        long ageMinutes = (System.currentTimeMillis() - Files.getLastModifiedTime(entry).toMillis()) / (1000 * 60);
                        if (ageMinutes > 5) {
                            try {
                                Files.walk(entry)
                                    .sorted((p1, p2) -> p2.compareTo(p1))  // reverse order to delete files before dirs
                                    .forEach(p -> {
                                        try {
                                            Files.deleteIfExists(p);
                                        } catch (IOException ignored) {}
                                    });
                                log.debug("Cleaned up old LibreOffice temp directory: {}", entry.getFileName());
                            } catch (IOException e) {
                                log.debug("Could not fully clean temp directory {}: {}", entry.getFileName(), e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.debug("Error during LibreOffice lock cleanup: {}", e.getMessage());
        }
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

        // If it's an office file (including legacy .xls/.doc/.ppt) try LibreOffice first when enabled
        // if (!converted && isOfficeFile(originalFilename) && libreEnabled) {
        //     if (libreAvailable) {
        //         log.info("Attempting LibreOffice conversion for: {}", originalFilename);
        //         converted = tryConvertWithLibreOffice(originalPath, pdfPath);
        //         if (converted) {
        //             log.info("Successfully converted {} using LibreOffice", originalFilename);
        //         } else {
        //             log.warn("LibreOffice conversion failed for {}", originalFilename);
        //         }
        //     } else {
        //         log.warn("LibreOffice enabled but not available. Skipping LibreOffice conversion for {}", originalFilename);
        //     }
        // }

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

    private boolean isOfficeFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".doc") || lower.endsWith(".docx")
                || lower.endsWith(".ppt") || lower.endsWith(".pptx")
                || lower.endsWith(".xls") || lower.endsWith(".xlsx")
                || lower.endsWith(".xlsm") || lower.endsWith(".xlsb");
    }

    private boolean tryConvertWithLibreOffice(Path src, Path dest) {
        try {
            // Clean up any stale LibreOffice lock files before conversion
            cleanupLibreOfficeLockFiles();
            
            Path outDir = Files.createTempDirectory(storageDir, "libreconv-");
            ProcessBuilder pb = new ProcessBuilder(librePath, 
                                                   "--headless", 
                                                   "--norestore",
                                                   "--convert-to", "pdf:writer_pdf_Export", 
                                                   "--outdir", outDir.toString(), 
                                                   src.toString());
            pb.redirectErrorStream(true);
            pb.environment().put("SAL_NO_DIALOGS", "1");
            pb.environment().put("SAL_HEADLESS", "1");
            
            log.debug("Running LibreOffice command: {} with outDir: {}", pb.command(), outDir);
            
            Process p = pb.start();

            // capture output
            String output;
            try (InputStream is = p.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                is.transferTo(baos);
                output = baos.toString();
            }

            boolean finished = p.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                log.warn("LibreOffice conversion timed out for {}", src.getFileName());
                log.warn("soffice output: {}", output);
                return false;
            }

            int code = p.exitValue();
            if (code != 0) {
                log.warn("LibreOffice conversion failed (exit {}) for {}", code, src.getFileName());
                log.warn("soffice output: {}", output);
                return false;
            }

            log.debug("soffice output: {}", output.replaceAll("\n", " "));
            
            // List what files were actually created in outDir
            try (var s = Files.list(outDir)) {
                var filesInDir = s.map(p2 -> p2.getFileName().toString()).toList();
                log.debug("Files in outDir {}: {}", outDir.getFileName(), filesInDir);
            }

            // Move result PDF to final location
            String base = src.getFileName().toString();
            int idx = base.lastIndexOf('.');
            if (idx > 0) base = base.substring(0, idx);
            Path produced = outDir.resolve(base + ".pdf");

            if (!Files.exists(produced)) {
                log.warn("LibreOffice did not produce PDF for {}. Expected at: {}", src.getFileName(), produced);
                // List files again for debugging
                try (var s = Files.list(outDir)) {
                    var files = s.map(Path::toString).toList();
                    log.warn("Actual files in outDir: {}", files);
                }
                return false;
            }

            Files.move(produced, dest);
            log.info("Successfully moved LibreOffice converted PDF to {}", dest.getFileName());

            // Cleanup temp directory
            try (var s = Files.list(outDir)) {
                s.forEach(pth -> {
                    try {
                        Files.deleteIfExists(pth);
                    } catch (IOException ignored) {
                    }
                });
            }
            Files.deleteIfExists(outDir);
            return true;
        } catch (Exception e) {
            log.warn("LibreOffice conversion failed with exception: {}", e.getMessage(), e);
            return false;
        }
    }
}
