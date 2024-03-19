package com.enach.logcompressor.service;

import com.enach.logcompressor.model.*;
import com.enach.logcompressor.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LogDecompressService {

    @Value("${logcompressor.decompressed.log.filename}")
    private String DECOMPRESSED_LOG_FILENAME;

    @Value("${logcompressor.newline.marker}")
    private String NEWLINE_MARKER;

    private final LogRepository logRepository;

    private static final Log logger = LogFactory.getLog(LogDecompressService.class);


    public void decompress(InputStream inputStream) throws IOException {
        logger.info("Starting processing decompression...");

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();

            LogFormat logFormat = new LogFormat(line);

            for (String formatType : logFormat.getFormatTypeList()) {
                if (LogFormatType.REP.getFormatType().equals(formatType)) {
                    List<LogRepetitiveFormatType> listGroup = new ArrayList<>();
                    while (!(line = reader.readLine()).isEmpty()) {
                        Long times = Long.valueOf(reader.readLine());
                        LogRepetitiveFormatType repFormatType = new LogRepetitiveFormatType(line, times);
                        listGroup.add(repFormatType);
                    }
                    logRepository.getLogRepetitiveFormatTypeList().add(listGroup);
                } else if (LogFormatType.NUM.getFormatType().equals(formatType)) {
                    String key = reader.readLine();
                    String strNumKey = key.replaceAll("[:,.]", "");
                    Long numKey = Long.parseLong(strNumKey);
                    LogNumericFormatType numFormatType = new LogNumericFormatType(key, numKey, numKey, new ArrayList<>());
                    while (!(line = reader.readLine()).isEmpty()) {
                        numFormatType.getDeltaList().add(Long.valueOf(line));
                    }
                    logRepository.getLogNumericFormatTypeList().add(numFormatType);
                } else if (LogFormatType.DICT.getFormatType().equals(formatType)) {
                    LogDictionaryFormatType dictFormatType = new LogDictionaryFormatType(null, new ArrayList<>(), new ArrayList<>());
                    while (!(line = reader.readLine()).isEmpty()) {
                        dictFormatType.getKeyList().add(line);
                    }
                    while (!(line = reader.readLine()).isEmpty()) {
                        dictFormatType.getOrderList().add(Long.valueOf(line));
                    }
                    logRepository.getLogDictionaryFormatTypeList().add(dictFormatType);
                } else if (LogFormatType.MSG.getFormatType().equals(formatType)) {
                    List<String> listGroup = new ArrayList<>();
                    while (!(line = reader.readLine()).isEmpty()) {
                        listGroup.add(line);
                    }
                    logRepository.getLogMessageFormatTypeList().add(listGroup);
                }
            }

            while ((line = reader.readLine()) != null) {
                String value = reader.readLine();
                logRepository.getLogNoMatchFormatTypeMap().put(Long.valueOf(line), value);
            }

            reader.close();
        } catch (Exception e) {
            if (reader != null) {
                reader.close();
            }
            throw new IOException();
        }

        logger.info("Decompression processed successfully!");

        logger.info("Starting exporting decompressed log...");
//        exportDecompressedLog(logFormat);
        logger.info("Decompressed log exported successfully!");
    }
}
