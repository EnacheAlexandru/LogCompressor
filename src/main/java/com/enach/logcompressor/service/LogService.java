package com.enach.logcompressor.service;

import com.enach.logcompressor.model.*;
import com.enach.logcompressor.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LogService {

    @Value("${logcompressor.compressed.log.filename}")
    private String COMPRESSED_LOG_FILENAME;

    @Value("${logcompressor.newline.marker}")
    private String NEWLINE_MARKER;

    @Value("${logcompressor.num.separators}")
    private String NUM_SEPARATORS;

    @Value("${logcompressor.error.stacktrace.size}")
    private int STACKTRACE_SIZE;

    private final LogRepository logRepository;

    private static final Log logger = LogFactory.getLog(LogService.class);

    public void compress(InputStream inputStream) throws IOException {
        logger.info("Starting processing compression...");

        LogFormat logFormat;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();
            long currentLine = 0;

            // first line has to match one of the formats in order to select the appropriate format
            logFormat = matchLogFormat(line);
            if (logFormat == null) {
                throw new IOException();
            }

            Pattern pattern = Pattern.compile(logFormat.getRegex());
            boolean isFirstLineProcessed = false;
            while (true) {
                if (isFirstLineProcessed) {
                    line = reader.readLine();
                    currentLine++;
                }
                if (line == null) {
                    break;
                }

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int group = 1;
                    int repGroup = 0;
                    int numGroup = 0;
                    int dictGroup = 0;
                    int msgGroup = 0;
                    for (String formatType : logFormat.getFormatTypeList()) {
                        if (LogFormatType.REP.getName().equals(formatType)) {
                            handleLogRepetitiveFormatType(repGroup++, matcher.group(group));
                        } else if (LogFormatType.NUM.getName().equals(formatType) || LogFormatType.NUMF.getName().equals(formatType)) {
                            handleLogNumericFormatType(numGroup++, matcher.group(group));
                        } else if (LogFormatType.DICT.getName().equals(formatType)) {
                            handleLogDictionaryFormatType(dictGroup++, matcher.group(group));
                        } else if (LogFormatType.MSG.getName().equals(formatType)) {
                            handleLogMessageFormatType(msgGroup++, matcher.group(group));
                        }
                        group++;
                    }
                } else {
                    handleLogNoMatchFormatType(currentLine, line);
                }

                if (!isFirstLineProcessed) {
                    isFirstLineProcessed = true;
                }
            }

            reader.close();
        } catch (Exception e) {
            if (reader != null) {
                reader.close();
            }
            logger.error(e);
            for (int i = 0; i < STACKTRACE_SIZE; i++) {
                logger.error(e.getStackTrace()[i]);
            }
            throw new IOException();
        }

        logger.info("Compression processed successfully!");

        logger.info("Starting exporting compressed log...");
        exportCompressedLog(logFormat);
        logger.info("Compressed log exported successfully!");
    }

    private LogFormat matchLogFormat(String line) {
        for (String name : logRepository.getLogFormatMap().keySet()) {
            LogFormat logFormat = logRepository.getLogFormatMap().get(name);
            Pattern pattern = Pattern.compile(logFormat.getRegex());
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                logger.info("Matched with '" + name + "' format!");
                return logFormat;
            }
        }

        logger.warn("Did not match with any format!");
        return null;
    }

    private void handleLogRepetitiveFormatType(int repGroup, String key) {
        List<List<LogRepetitiveFormatType>> list = logRepository.getLogRepetitiveFormatTypeList();

        LogRepetitiveFormatType repFormatType;
        List<LogRepetitiveFormatType> groupList;
        if (repGroup == list.size()) {
            groupList = new ArrayList<>();
            repFormatType = new LogRepetitiveFormatType(key, 1L);
            groupList.add(repFormatType);
            list.add(groupList);
        } else {
            groupList = list.get(repGroup);
            repFormatType = groupList.get(groupList.size() - 1);
            if (key.equals(repFormatType.getKey())) {
                repFormatType.setTimes(repFormatType.getTimes() + 1);
            } else {
                LogRepetitiveFormatType newRepFormatType = new LogRepetitiveFormatType(key, 1L);
                groupList.add(newRepFormatType);
            }
        }
    }

    private void handleLogNumericFormatType(int numGroup, String key) {
        List<LogNumericFormatType> list = logRepository.getLogNumericFormatTypeList();

        String strNumber = key.replaceAll(NUM_SEPARATORS, "");
        Long number = Long.parseLong(strNumber);

        LogNumericFormatType numFormatType;
        if (numGroup == list.size()) {
            numFormatType = new LogNumericFormatType(key, number, number, new ArrayList<>(List.of(0L)), null, NUM_SEPARATORS);
            list.add(numFormatType);
        } else {
            numFormatType = list.get(numGroup);
            numFormatType.getDeltaList().add(number - numFormatType.getCurrent());
            numFormatType.setCurrent(number);
        }
    }

    private void handleLogDictionaryFormatType(int dictGroup, String key) {
        List<LogDictionaryFormatType> list = logRepository.getLogDictionaryFormatTypeList();

        LogDictionaryFormatType dictFormatType;
        if (dictGroup == list.size()) {
            Map<String, Long> keyMap = new LinkedHashMap<>();
            keyMap.put(key, 0L);
            dictFormatType = new LogDictionaryFormatType(keyMap, null, new ArrayList<>(List.of(0L)));
            list.add(dictFormatType);
        } else {
            dictFormatType = list.get(dictGroup);
            if (!dictFormatType.getKeyMap().containsKey(key)) {
                dictFormatType.getKeyMap().put(key, (long) dictFormatType.getKeyMap().size());
            }
            dictFormatType.getOrderList().add(dictFormatType.getKeyMap().get(key));
        }
    }

    private void handleLogMessageFormatType(int msgGroup, String key) {
        List<List<String>> list = logRepository.getLogMessageFormatTypeList();

        List<String> groupList;
        if (msgGroup == list.size()) {
            groupList = new ArrayList<>(List.of(key));
            list.add(groupList);
        } else {
            groupList = list.get(msgGroup);
            groupList.add(key);
        }
    }

    private void handleLogNoMatchFormatType(long currentLine, String line) {
        Map<Long, String> map = logRepository.getLogNoMatchFormatTypeMap();
        if ("".equals(line)) {
            map.put(currentLine, NEWLINE_MARKER);
        } else {
            map.put(currentLine, line);
        }
    }

    private void exportCompressedLog(LogFormat logFormat) throws IOException {
        String path = "src/main/resources/" + COMPRESSED_LOG_FILENAME;

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(path));

            writer.write(logFormat.getFormat());
            writer.newLine();

            int index = 0;
            int repGroup = 0;
            int numGroup = 0;
            int dictGroup = 0;
            int msgGroup = 0;
            for (String formatType : logFormat.getFormatTypeList()) {
                if (LogFormatType.REP.getName().equals(formatType)) {
                    List<LogRepetitiveFormatType> groupList = logRepository.getLogRepetitiveFormatTypeList().get(repGroup++);
                    for (LogRepetitiveFormatType repFormatType : groupList) {
                        writer.write(repFormatType.getKey());
                        writer.newLine();
                        writer.write(String.valueOf(repFormatType.getTimes()));
                        writer.newLine();
                    }
                } else if (LogFormatType.NUM.getName().equals(formatType) || LogFormatType.NUMF.getName().equals(formatType)) {
                    LogNumericFormatType group = logRepository.getLogNumericFormatTypeList().get(numGroup++);
                    writer.write(group.getKey());
                    writer.newLine();
                    for (long delta : group.getDeltaList()) {
                        writer.write(String.valueOf(delta));
                        writer.newLine();
                    }
                } else if (LogFormatType.DICT.getName().equals(formatType)) {
                    LogDictionaryFormatType group = logRepository.getLogDictionaryFormatTypeList().get(dictGroup++);
                    for (String key : group.getKeyMap().keySet()) {
                        writer.write(key);
                        writer.newLine();
                    }
                    writer.newLine();
                    for (long order : group.getOrderList()) {
                        writer.write(String.valueOf(order));
                        writer.newLine();
                    }

                } else if (LogFormatType.MSG.getName().equals(formatType)) {
                    List<String> groupList = logRepository.getLogMessageFormatTypeList().get(msgGroup++);
                    for (String msg : groupList) {
                        writer.write(msg);
                        writer.newLine();
                    }
                }

                if (index < logFormat.getFormatTypeList().size() - 1) {
                    writer.newLine();
                }

                index++;
            }

            Map<Long, String> noMatchMap = logRepository.getLogNoMatchFormatTypeMap();

            if (!noMatchMap.isEmpty()) {
                writer.newLine();
            }

            for (long line : noMatchMap.keySet()) {
                writer.write(String.valueOf(line));
                writer.newLine();
                writer.write(noMatchMap.get(line));
                writer.newLine();
            }

            writer.close();
        } catch (Exception e) {
            if (writer != null) {
                writer.close();
            }
            logger.error("Error while trying to export compressed file!");
            logger.error(e);
            for (int i = 0; i < STACKTRACE_SIZE; i++) {
                logger.error(e.getStackTrace()[i]);
            }
            throw new IOException();
        }
    }

    public void clearFormatType() {
        logRepository.clearFormatType();
    }
}
