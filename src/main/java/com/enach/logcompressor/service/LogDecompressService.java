package com.enach.logcompressor.service;

import com.enach.logcompressor.model.*;
import com.enach.logcompressor.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LogDecompressService {

    @Value("${logcompressor.decompressed.log.filename}")
    private String DECOMPRESSED_LOG_FILENAME;

    @Value("${logcompressor.newline.marker}")
    private String NEWLINE_MARKER;

    @Value("${logcompressor.num.separators}")
    private String NUM_SEPARATORS;

    @Value("${logcompressor.line.separators}")
    private String LINE_SEPARATORS;

    private final LogRepository logRepository;

    private static final Log logger = LogFactory.getLog(LogDecompressService.class);


    public void decompress(InputStream inputStream) throws IOException {
        logger.info("Starting processing decompression...");

        LogFormat logFormat;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();

            logFormat = new LogFormat(null, line, LINE_SEPARATORS);

            for (String formatType : logFormat.getFormatTypeList()) {
                if (LogFormatType.REP.getName().equals(formatType)) {
                    List<LogRepetitiveFormatType> listGroup = new ArrayList<>();
                    while (!(line = reader.readLine()).isEmpty()) {
                        Long times = Long.valueOf(reader.readLine());
                        LogRepetitiveFormatType repFormatType = new LogRepetitiveFormatType(line, times);
                        listGroup.add(repFormatType);
                    }
                    logRepository.getLogRepetitiveFormatTypeList().add(listGroup);
                } else if (LogFormatType.NUM.getName().equals(formatType)) {
                    String key = reader.readLine();
                    String strNumKey = key.replaceAll(NUM_SEPARATORS, "");
                    Long numKey = Long.parseLong(strNumKey);
                    LogNumericFormatType numFormatType = new LogNumericFormatType(key, numKey, numKey, new ArrayList<>(), NUM_SEPARATORS);
                    while (!(line = reader.readLine()).isEmpty()) {
                        numFormatType.getDeltaList().add(Long.valueOf(line));
                    }
                    logRepository.getLogNumericFormatTypeList().add(numFormatType);
                } else if (LogFormatType.DICT.getName().equals(formatType)) {
                    LogDictionaryFormatType dictFormatType = new LogDictionaryFormatType(null, new ArrayList<>(), new ArrayList<>());
                    while (!(line = reader.readLine()).isEmpty()) {
                        dictFormatType.getKeyList().add(line);
                    }
                    while (!(line = reader.readLine()).isEmpty()) {
                        dictFormatType.getOrderList().add(Long.valueOf(line));
                    }
                    logRepository.getLogDictionaryFormatTypeList().add(dictFormatType);
                } else if (LogFormatType.MSG.getName().equals(formatType)) {
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
        exportDecompressedLog(logFormat);
        logger.info("Decompressed log exported successfully!");
    }

    private void exportDecompressedLog(LogFormat logFormat) throws IOException {
        String path = "src/main/resources/" + DECOMPRESSED_LOG_FILENAME;

        int totalLines = logRepository.getLogMessageFormatTypeList().get(0).size() + logRepository.getLogNoMatchFormatTypeMap().size();
        int index = 0;
        int currentLine = 0;

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(path));

            while (currentLine < totalLines) {
                String noMatchObj = logRepository.getLogNoMatchFormatTypeMap().get((long) currentLine);
                if (noMatchObj != null) {
                    if (!NEWLINE_MARKER.equals(noMatchObj)) {
                        writer.write(noMatchObj);
                    }
                    writer.newLine();
                    currentLine++;
                    continue;
                }
                String line = String.valueOf(logFormat.getFormat()); // need a copy
                line = line.replaceAll("rep|num|dict|msg", "%s");
                List<String> replaceList = new ArrayList<>();
                int repGroup = 0;
                int numGroup = 0;
                int dictGroup = 0;
                int msgGroup = 0;
                for (String formatType : logFormat.getFormatTypeList()) {
                    if (LogFormatType.REP.getName().equals(formatType)) {
                        List<LogRepetitiveFormatType> repObjList = logRepository.getLogRepetitiveFormatTypeList().get(repGroup++);
                        LogRepetitiveFormatType repObj = repObjList.get(0);
                        replaceList.add(repObj.getKey());
                        repObj.setTimes(repObj.getTimes() - 1);
                        if (repObjList.get(0).getTimes() == 0L) {
                            repObjList.remove(0);
                        }
                    } else if (LogFormatType.NUM.getName().equals(formatType)) {
                        LogNumericFormatType numObj = logRepository.getLogNumericFormatTypeList().get(numGroup++);
                        numObj.setCurrent(numObj.getCurrent() + numObj.getDeltaList().get(index));
                        replaceList.add(numObj.formatCurrentLikeKey());
                    } else if (LogFormatType.DICT.getName().equals(formatType)) {
                        LogDictionaryFormatType dictObj = logRepository.getLogDictionaryFormatTypeList().get(dictGroup++);
                        long currentOrder = dictObj.getOrderList().get(index);
                        replaceList.add(dictObj.getKeyList().get((int) currentOrder));
                    } else if (LogFormatType.MSG.getName().equals(formatType)) {
                        String msgObj = logRepository.getLogMessageFormatTypeList().get(msgGroup++).get(index);
                        replaceList.add(msgObj);
                    }
                }

                line = String.format(line, replaceList.toArray());

                writer.write(line);
                writer.newLine();

                currentLine++;
                index++;
            }

            writer.close();
        } catch (Exception e) {
            if (writer != null) {
                writer.close();
            }
            logger.error("Error while trying to export decompressed file!");
            throw new IOException();
        }
    }
}
