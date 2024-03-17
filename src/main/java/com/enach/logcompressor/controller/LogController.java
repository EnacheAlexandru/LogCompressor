package com.enach.logcompressor.controller;

import com.enach.logcompressor.service.LogService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequiredArgsConstructor
public class LogController {

    private static final Log logger = LogFactory.getLog(LogController.class);

    @Value("${logcompressor.compressed.log.filename}")
    private String COMPRESSED_LOG_FILENAME;

    private final LogService logService;

    @PostMapping(value="/compress", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> compress(@RequestParam(value="file") MultipartFile logFile) {
        try {
            if (logFile == null || logFile.isEmpty()) {
                throw new Exception();
            }
            logger.info("Received '" + logFile.getOriginalFilename() + "' for compression.");
            logService.compress(logFile.getInputStream());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error while trying to compress file.");
            return ResponseEntity.badRequest().build();
        } finally {
            logService.clearFormatType();
        }
    }

    @GetMapping(value="/compress/download", produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> compressDownload() {
        try {
            Path logFormatPath = Paths.get("src/main/resources/" + COMPRESSED_LOG_FILENAME);
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(logFormatPath));
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + COMPRESSED_LOG_FILENAME + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error while trying to download compressed file!");
            return ResponseEntity.badRequest().build();
        }
    }
}
