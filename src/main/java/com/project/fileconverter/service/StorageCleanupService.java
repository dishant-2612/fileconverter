package com.project.fileconverter.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StorageCleanupService {

    private static final Logger log = LoggerFactory.getLogger(StorageCleanupService.class);
    private final Path storageDir;
    private final long retentionDays;

    public StorageCleanupService(@Value("${file.storage.location:storage}") String storageLocation,
                                 @Value("${file.storage.retention-days:30}") long retentionDays) {
        this.storageDir = Paths.get(storageLocation).toAbsolutePath();
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedDelayString = "PT6H") // every 6 hours
    public void pruneOldFiles() {
        try {
            if (!Files.exists(storageDir)) return;
            Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(storageDir)) {
                for (Path p : ds) {
                    try {
                        Instant m = Files.getLastModifiedTime(p).toInstant();
                        if (m.isBefore(cutoff)) {
                            Files.deleteIfExists(p);
                            log.info("Deleted old storage file {}", p.getFileName());
                        }
                    } catch (IOException ex) {
                        log.warn("Failed to check/delete {}: {}", p, ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Storage cleanup failed: {}", e.getMessage());
        }
    }
}
