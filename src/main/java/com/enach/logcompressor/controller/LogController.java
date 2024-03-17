package com.enach.logcompressor.controller;

import com.enach.logcompressor.service.LogService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LogController {

    private static final Log logger = LogFactory.getLog(LogController.class);

    private final LogService logService;

    @PostMapping(value="/generate", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> login(@RequestParam(value="file") MultipartFile logFile) {
        try {
            if (logFile == null || logFile.isEmpty()) {
                throw new Exception();
            }
            logger.info("Received '" + logFile.getOriginalFilename() + "' file for compression");
            logService.compressLogFile(logFile.getInputStream());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error while trying to compress file!");
            return ResponseEntity.badRequest().build();
        } finally {
            logService.clearFormatType();
        }
    }
}
