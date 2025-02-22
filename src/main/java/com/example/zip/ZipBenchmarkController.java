package com.example.zip;

import net.lingala.zip4j.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/api/zip")
public class ZipBenchmarkController {
    private static final Logger logger = LoggerFactory.getLogger(ZipBenchmarkController.class);
    private static final Random random = new Random();

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Mono<String> uploadAndExtract(@RequestPart("file") FilePart filePart) {

        logger.info("Received file: {}", filePart.filename());
        return Mono.fromCallable(() -> Files.createTempFile("upload_", ".zip"))
                .flatMap(tempZip -> filePart.transferTo(tempZip.toFile()).thenReturn(tempZip))
                .flatMap(tempZip -> Mono.fromCallable(() -> {
                    Path extractDir = Files.createTempDirectory("extracted_");
                    boolean useZip4j = random.nextBoolean();
                    logger.info("Using {}", useZip4j ? "Zip4j" : "java.util.zip");

                    long startTime = System.nanoTime();
                    try {
                        if (useZip4j) {
                            extractWithZip4j(tempZip.toFile(), extractDir.toFile());
                        } else {
                            extractWithJavaUtilZip(tempZip.toFile(), extractDir.toFile());
                        }
                    } finally {
                        long duration = System.nanoTime() - startTime;
                        long timeMs = duration / 1_000_000;
                        logger.info("Extraction completed in {} ms using {}", timeMs, useZip4j ? "Zip4j" : "java.util.zip");

                        return new ExtractionResult(useZip4j ? "Zip4j" : "java.util.zip", timeMs, tempZip, extractDir);
                    }
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(result -> cleanUpTempFiles(result.zipFile, result.extractDir))
                .map(result -> String.format("Extraction successful! Library: %s, Time Taken: %d ms",
                        result.library, result.timeMs));
    }

    private void extractWithJavaUtilZip(File zipFile, File destDir) throws IOException {
        logger.info("Extracting using java.util.zip...");
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File extractedFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    extractedFile.mkdirs();
                } else {
                    try (FileOutputStream fos = new FileOutputStream(extractedFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
        logger.info("java.util.zip extraction completed.");
    }

    private void extractWithZip4j(File zipFile, File destDir) throws IOException {
        logger.info("Extracting using Zip4j...");
        try (ZipFile zip4j = new ZipFile(zipFile)) {
            zip4j.extractAll(destDir.getAbsolutePath());
        }
        logger.info("Zip4j extraction completed.");
    }

    private void cleanUpTempFiles(Path zipFile, Path extractDir) {
        try {
            // Delete extracted directory and contents
            Files.walk(extractDir)
                    .sorted(Comparator.reverseOrder()) // Delete files before the directory itself
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            logger.info("Deleted: {}", path);
                        } catch (IOException e) {
                            logger.error("Failed to delete: {}", path, e);
                        }
                    });

            // Delete ZIP file
            Files.deleteIfExists(zipFile);
            logger.info("Deleted uploaded ZIP: {}", zipFile);
        } catch (IOException e) {
            logger.error("Cleanup failed", e);
        }
    }

    // Utility class to track extraction results
    private static class ExtractionResult {
        String library;
        long timeMs;
        Path zipFile;
        Path extractDir;

        ExtractionResult(String library, long timeMs, Path zipFile, Path extractDir) {
            this.library = library;
            this.timeMs = timeMs;
            this.zipFile = zipFile;
            this.extractDir = extractDir;
        }
    }
}
