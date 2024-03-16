package com.enach.logcompressor.service;

import com.enach.logcompressor.model.*;
import com.enach.logcompressor.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;

    private static final Log logger = LogFactory.getLog(LogService.class);

    public String compressLogFile(InputStream inputStream) throws IOException {
        logger.info("Starting compressing file...");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = reader.readLine();

        // first line has to match one of the formats in order to select the appropriate format
        LogFormat logFormat = matchLogFormat(line);
        if (logFormat == null) {
            return null;
        }

        Pattern pattern = Pattern.compile(logFormat.getRegex());
        boolean isFirstLineProcessed = false;
        while (true) {
            if (isFirstLineProcessed) {
                line = reader.readLine();
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
                for (String formatType : logFormat.getFormatTypeList()) {
                    if (LogFormatType.REP.getFormatType().equals(formatType)) {
                        handleLogRepetitiveFormatType(repGroup++, matcher.group(group));
                    } else if (LogFormatType.NUM.getFormatType().equals(formatType)) {
                        handleLogNumericFormatType(numGroup++, matcher.group(group));
                    } else if (LogFormatType.DICT.getFormatType().equals(formatType)) {
                        handleLogDictionaryFormatType(dictGroup++, matcher.group(group));
                    }
                    group++;
                }
            }

            if (!isFirstLineProcessed) {
                isFirstLineProcessed = true;
            }
        }

        logger.info("File compressed successfully!");
        return "lol";
    }

    private LogFormat matchLogFormat(String line) {
        for (String name : logRepository.getLogFormatMap().keySet()) {
            LogFormat logFormat = logRepository.getLogFormatMap().get(name);
            Pattern pattern = Pattern.compile(logFormat.getRegex());
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                logger.info("Matched with '" + name + "' format");
                return logFormat;
            }
        }

        logger.warn("Did not match with any format");
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

        String strNumber = key.replaceAll("[:,.]", "");
        Long number = Long.parseLong(strNumber);

        LogNumericFormatType numFormatType;
        if (numGroup == list.size()) {
            numFormatType = new LogNumericFormatType(key, number, number, new ArrayList<>(List.of(0L)));
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
            Map<String, Long> keyMap = new HashMap<>();
            keyMap.put(key, 0L);
            dictFormatType = new LogDictionaryFormatType(keyMap, new ArrayList<>(List.of(0L)));
            list.add(dictFormatType);
        } else {
            dictFormatType = list.get(dictGroup);
            if (!dictFormatType.getKeyMap().containsKey(key)) {
                dictFormatType.getKeyMap().put(key, (long) dictFormatType.getKeyMap().size());
            }
            dictFormatType.getOrderList().add(dictFormatType.getKeyMap().get(key));
        }
    }
}
