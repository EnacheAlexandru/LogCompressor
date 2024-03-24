package com.enach.logcompressor.repository;

import com.enach.logcompressor.model.LogFormat;
import com.enach.logcompressor.model.LogDictionaryFormatType;
import com.enach.logcompressor.model.LogNumericFormatType;
import com.enach.logcompressor.model.LogRepetitiveFormatType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Getter
@Repository
@RequiredArgsConstructor
public class LogRepository implements ApplicationRunner {

    private static final Log logger = LogFactory.getLog(LogRepository.class);

	@Value("${logcompressor.log.format.filename}")
    private String LOG_FORMAT_FILENAME;

	@Value("${logcompressor.line.separators}")
	private String LINE_SEPARATORS;

    private final Map<String, LogFormat> logFormatMap = new HashMap<>();

	// === \/ FORMAT TYPES \/ ===

	// wrapped in lists because of groups

	// rep
	private final List<List<LogRepetitiveFormatType>> logRepetitiveFormatTypeList = new ArrayList<>();

	// num
	private final List<LogNumericFormatType> logNumericFormatTypeList = new ArrayList<>();

	// dict
	private final List<LogDictionaryFormatType> logDictionaryFormatTypeList = new ArrayList<>();

	// msg
	private final List<List<String>> logMessageFormatTypeList = new ArrayList<>();

	// lines that do not match any format
	private final Map<Long, String> logNoMatchFormatTypeMap = new LinkedHashMap<>();

	// === /\ FORMAT TYPES /\ ===

    @Override
    public void run(ApplicationArguments args) {
		BufferedReader reader = null;
        try {
			String path = "src/main/resources/" + LOG_FORMAT_FILENAME;
			reader = new BufferedReader(new FileReader(path));
			String line;
			while ((line = reader.readLine()) != null) {
				String name = line;
				line = reader.readLine();
				String regex = line;
				line = reader.readLine();
				String format = line;
				LogFormat logFormat = new LogFormat(regex, format, LINE_SEPARATORS);
                logFormatMap.put(name, logFormat);
			}
			reader.close();
		} catch (IOException e1) {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e2) {
				// ignored
			}
			logger.error("Error reading log formats file");
            System.exit(0);
		}
    }

	public void clearFormatType() {
		logRepetitiveFormatTypeList.clear();
		logNumericFormatTypeList.clear();
		logDictionaryFormatTypeList.clear();
		logMessageFormatTypeList.clear();
		logNoMatchFormatTypeMap.clear();
	}
}
