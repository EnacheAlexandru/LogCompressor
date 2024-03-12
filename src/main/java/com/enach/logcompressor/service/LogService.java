package com.enach.logcompressor.service;

import com.enach.logcompressor.model.LogFormat;
import com.enach.logcompressor.model.LogFormatType;
import com.enach.logcompressor.model.LogRepetitiveFormatType;
import com.enach.logcompressor.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
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
                for (String formatType : logFormat.getFormatTypeList()) {
                    if (LogFormatType.REP.getFormatType().equals(formatType)) {
                        handleLogRepetitiveFormatType(repGroup, matcher.group(group));
                        repGroup++;
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
        List<SortedMap<String, Long>> list = logRepository.getLogRepetitiveFormatTypeList();
        if (repGroup == list.size()) {
            SortedMap<String, Long> map = new TreeMap<>();
            map.put(key, 1L);
            list.add(map);
        } else {
            SortedMap<String, Long> map = list.get(repGroup);
            if (map.containsKey(key)) {
                map.put(key, map.get(key) + 1);
            } else {
                map.put(key, 1L);
            }
        }
    }
}
