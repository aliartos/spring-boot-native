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
        return Mono.fromCallable(() -> Files.createTempFile("upload_", ".zip"))
                .flatMap(tempZip -> filePart.transferTo(tempZip.toFile())
                        .thenReturn(tempZip))
                .flatMap(tempZip -> Mono.fromCallable(() -> {
                    Path extractDir = Files.createTempDirectory("extracted_");
                    boolean useZip4j = random.nextBoolean();
                    logger.info("Using {}", useZip4j ? "Zip4j" : "java.util.zip");

                    long startTime = System.nanoTime();
                    if (useZip4j) {
                        extractWithZip4j(tempZip.toFile(), extractDir.toFile());
                    } else {
                        extractWithJavaUtilZip(tempZip.toFile(), extractDir.toFile());
                    }
                    long duration = System.nanoTime() - startTime;

                    logger.info("Extraction completed in {} ms using {}", duration / 1_000_000, useZip4j ? "Zip4j" : "java.util.zip");
                    return "Extraction successful! Method used: " + (useZip4j ? "Zip4j" : "java.util.zip") + "  Duration: " + duration / 1_000_000 + " ms!";
                }))
                .subscribeOn(Schedulers.boundedElastic()); // Run in a separate thread pool
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
}
