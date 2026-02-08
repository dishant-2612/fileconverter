package com.project.fileconverter.controller;

import com.project.fileconverter.dto.ConvertResponse;
import com.project.fileconverter.model.StoredFile;
import com.project.fileconverter.service.ConverterService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/api")
public class FileController {

    private final ConverterService converter;

    public FileController(ConverterService converter) {
        this.converter = converter;
    }

    @PostMapping("/convert")
    public ResponseEntity<ConvertResponse> convert(@RequestParam("file") MultipartFile file) throws IOException {
        StoredFile stored = converter.storeAndConvert(file);
        ConvertResponse resp = ConvertResponse.builder()
                .id(stored.getId())
                .originalFilename(stored.getOriginalFilename())
                .contentType(stored.getContentType())
                .viewUrl("/api/view/" + stored.getId())
                .downloadUrl("/api/download/" + stored.getId())
                .build();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/view/{id}")
    public ResponseEntity<byte[]> view(@PathVariable String id) throws IOException {
        var pdfPath = converter.getPdfPath(id);
        if (pdfPath == null) return ResponseEntity.notFound().build();
        byte[] data = Files.readAllBytes(pdfPath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable String id) throws IOException {
        var path = converter.getOriginalPath(id);
        if (path == null) return ResponseEntity.notFound().build();
        byte[] data = Files.readAllBytes(path);
        String fname = path.getFileName().toString();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fname + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(data);
    }
}
