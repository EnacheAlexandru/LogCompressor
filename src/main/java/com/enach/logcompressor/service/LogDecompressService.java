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

    @Value("${logcompressor.debug.print.line.multiple}")
    private int DEBUG_LINE_MULTIPLE;

    private final LogRepository logRepository;

    private static final Log logger = LogFactory.getLog(LogDecompressService.class);


    public void decompress(InputStream inputStream) throws IOException {
        logger.info("Starting processing decompression...");

        LogFormat logFormat;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();

            logFormat = logRepository.getLogFormatMap().get(line); // the first line is the name
            logger.info("Compressed file matched with '" + line + "' format!");

            for (String formatType : logFormat.getFormatTypeList()) {
                if (LogFormatType.REP.getName().equals(formatType)) {
                    List<LogRepetitiveFormatType> listGroup = new ArrayList<>();
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        Long times = Long.valueOf(reader.readLine());
                        LogRepetitiveFormatType repFormatType = new LogRepetitiveFormatType(line, times);
                        listGroup.add(repFormatType);
                    }
                    logRepository.getLogRepetitiveFormatTypeList().add(listGroup);
                } else if (LogFormatType.NUM.getName().equals(formatType) || LogFormatType.NUMF.getName().equals(formatType)) {
                    String key = reader.readLine();
                    String strNumKey = key.replaceAll(NUM_SEPARATORS, "");
                    Long numKey = Long.parseLong(strNumKey);
                    boolean isFixedLength = LogFormatType.NUMF.getName().equals(formatType);
                    LogNumericFormatType numFormatType = new LogNumericFormatType(key, numKey, numKey, new ArrayList<>(), isFixedLength, NUM_SEPARATORS);
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        numFormatType.getDeltaList().add(Long.valueOf(line));
                    }
                    logRepository.getLogNumericFormatTypeList().add(numFormatType);
                } else if (LogFormatType.DICT.getName().equals(formatType)) {
                    LogDictionaryFormatType dictFormatType = new LogDictionaryFormatType(null, new ArrayList<>(), new ArrayList<>());
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        dictFormatType.getKeyList().add(line);
                    }
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        dictFormatType.getOrderList().add(Long.valueOf(line));
                    }
                    logRepository.getLogDictionaryFormatTypeList().add(dictFormatType);
                } else if (LogFormatType.MSG.getName().equals(formatType)) {
                    List<String> listGroup = new ArrayList<>();
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        listGroup.add(line);
                    }
                    logRepository.getLogMessageFormatTypeList().add(listGroup);
                }
            }

            reader.close();
        } catch (Exception e) {
            if (reader != null) {
                reader.close();
            }
            logger.error(logRepository.printStackTrace(e));
            throw new IOException();
        }

        logger.info("Decompression processed successfully!");

        logger.info("Starting exporting decompressed log...");
        exportDecompressedLog(logFormat);
        logger.info("Decompressed log exported successfully!");
    }

    private void exportDecompressedLog(LogFormat logFormat) throws IOException {
        String path = "src/main/resources/" + DECOMPRESSED_LOG_FILENAME;

        int totalLines = logRepository.getLogNumericFormatTypeList().get(0).getDeltaList().size();
        logger.debug("Total lines in compressed file: " + totalLines);
        int currentLine = 0;
        int lastMsgCurrentLine = 0;

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(path));

            while (currentLine < totalLines) {
                String line = String.valueOf(logFormat.getFormat()); // need a copy
                line = line.replaceAll("\\b(rep|num|numf|dict|msg)\\b", "%s");
                List<String> replaceList = new ArrayList<>();
                int repGroup = 0;
                int numGroup = 0;
                int dictGroup = 0;
                int msgGroup = 0;
                boolean containsNewline = false; // used for last msg group
                for (String formatType : logFormat.getFormatTypeList()) {
                    if (LogFormatType.REP.getName().equals(formatType)) {
                        List<LogRepetitiveFormatType> repObjList = logRepository.getLogRepetitiveFormatTypeList().get(repGroup++);
                        LogRepetitiveFormatType repObj = repObjList.get(0);
                        replaceList.add(repObj.getKey());
                        repObj.setTimes(repObj.getTimes() - 1);
                        if (repObjList.get(0).getTimes() == 0L) {
                            repObjList.remove(0);
                        }
                    } else if (LogFormatType.NUM.getName().equals(formatType) || LogFormatType.NUMF.getName().equals(formatType)) {
                        LogNumericFormatType numObj = logRepository.getLogNumericFormatTypeList().get(numGroup++);
                        numObj.setCurrent(numObj.getCurrent() + numObj.getDeltaList().get(currentLine));
                        replaceList.add(numObj.formatCurrentLikeKey());
                    } else if (LogFormatType.DICT.getName().equals(formatType)) {
                        LogDictionaryFormatType dictObj = logRepository.getLogDictionaryFormatTypeList().get(dictGroup++);
                        long currentOrder = dictObj.getOrderList().get(currentLine);
                        replaceList.add(dictObj.getKeyList().get((int) currentOrder));
                    } else if (LogFormatType.MSG.getName().equals(formatType)) {
                        String msgObj;
                        if (msgGroup == logRepository.getLogMessageFormatTypeList().size() - 1) { // last msg group
                            msgObj = logRepository.getLogMessageFormatTypeList().get(msgGroup++).get(lastMsgCurrentLine++);
                            if (msgObj.contains(NEWLINE_MARKER)) {
                                containsNewline = true;
                                msgObj = msgObj.replace(NEWLINE_MARKER, System.lineSeparator());
                            }
                        } else {
                            msgObj = logRepository.getLogMessageFormatTypeList().get(msgGroup++).get(currentLine);
                        }

                        replaceList.add(msgObj);
                    }
                }

                line = String.format(line, replaceList.toArray());

                writer.write(line);

                if (!containsNewline) {
                    writer.newLine();
                }

                while (containsNewline && lastMsgCurrentLine < logRepository.getLogMessageFormatTypeList().get(msgGroup - 1).size()) {
                    String msgObj = logRepository.getLogMessageFormatTypeList().get(msgGroup - 1).get(lastMsgCurrentLine++);
                    if (msgObj.contains(NEWLINE_MARKER)) {
                        msgObj = msgObj.replace(NEWLINE_MARKER, System.lineSeparator());
                        writer.write(msgObj);
                    } else {
                        containsNewline = false;
                        writer.write(msgObj);
                        writer.newLine();
                    }
                }

                currentLine++;

                if (currentLine % DEBUG_LINE_MULTIPLE == 0) {
                    logger.debug("Current line read: " + currentLine);
                }
            }

            writer.close();
        } catch (Exception e) {
            if (writer != null) {
                writer.close();
            }
            logger.error(logRepository.printStackTrace(e));
            throw new IOException();
        }
    }
}
