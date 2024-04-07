package com.enach.logcompressor.controller;

import com.enach.logcompressor.repository.LogRepository;
import com.enach.logcompressor.service.LogDecompressService;
import com.enach.logcompressor.service.LogService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

    @Value("${logcompressor.decompressed.log.filename}")
    private String DECOMPRESSED_LOG_FILENAME;

    private final LogRepository logRepository;

    private final LogService logService;

    private final LogDecompressService logDecompressService;

    @PostMapping(value="/compress", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> compress(@RequestParam(value="file") MultipartFile logFile) {
        if (logRepository.isProcessing()) {
            logger.error("Already compressing a file!");
            return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
        }
        try {
            logRepository.setProcessing(true);
            if (logFile == null || logFile.isEmpty()) {
                throw new Exception();
            }
            logger.info("Received '" + logFile.getOriginalFilename() + "' for compression.");
            logService.compress(logFile.getInputStream());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error while trying to compress file!");
            return ResponseEntity.badRequest().build();
        } finally {
            logService.clearFormatType();
            logRepository.setProcessing(false);
        }
    }

    @GetMapping(value="/compress/download", produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> compressDownload() {
        if (logRepository.isProcessing()) {
            logger.error("Already downloading a compressed file!");
            return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
        }
        try {
            logRepository.setProcessing(true);
            Path logFormatPath = Paths.get("src/main/resources/" + COMPRESSED_LOG_FILENAME);
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(logFormatPath));
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + COMPRESSED_LOG_FILENAME + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error while trying to download compressed file!");
            return ResponseEntity.badRequest().build();
        } finally {
            logRepository.setProcessing(false);
        }
    }

    @PostMapping(value="/decompress", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> decompress(@RequestParam(value="file") MultipartFile logFile) {
        if (logRepository.isProcessing()) {
            logger.error("Already decompressing a file!");
            return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
        }
        try {
            logRepository.setProcessing(true);
            if (logFile == null || logFile.isEmpty()) {
                throw new Exception();
            }
            logger.info("Received '" + logFile.getOriginalFilename() + "' for decompression.");
            logDecompressService.decompress(logFile.getInputStream());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error while trying to decompress file!");
            return ResponseEntity.badRequest().build();
        } finally {
            logService.clearFormatType();
            logRepository.setProcessing(false);
        }
    }

    @GetMapping(value="/decompress/download", produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> decompressDownload() {
        if (logRepository.isProcessing()) {
            logger.error("Already downloading a decompressed file!");
            return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
        }
        try {
            logRepository.setProcessing(true);
            Path logFormatPath = Paths.get("src/main/resources/" + DECOMPRESSED_LOG_FILENAME);
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(logFormatPath));
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + DECOMPRESSED_LOG_FILENAME + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error while trying to download decompressed file!");
            return ResponseEntity.badRequest().build();
        } finally {
            logRepository.setProcessing(false);
        }
    }
}
