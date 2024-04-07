package com.enach.logcompressor.repository;

import com.enach.logcompressor.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

@Getter
@Setter
@Repository
@RequiredArgsConstructor
public class LogRepository implements ApplicationRunner {

    private static final Log logger = LogFactory.getLog(LogRepository.class);

	@Value("${logcompressor.log.format.filename}")
    private String LOG_FORMAT_FILENAME;

	@Value("${logcompressor.line.separators}")
	private String LINE_SEPARATORS;

	@Value("${logcompressor.error.stacktrace.size}")
	private int STACKTRACE_SIZE;

	private boolean isProcessing = false;

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
				LogFormat logFormat = new LogFormat(name, regex, format, LINE_SEPARATORS);
				if (!LogFormatType.MSG.getName().equals(logFormat.getFormatTypeList().get(logFormat.getFormatTypeList().size() - 1))) {
					logger.error("All line formats have to finish with 'msg'!");
					throw new Exception();
				}
                logFormatMap.put(name, logFormat);
			}
			reader.close();
		} catch (Exception e1) {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (Exception e2) {
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
	}

	public String printStackTrace(Exception e) {
		StringBuilder stacktrace = new StringBuilder(String.valueOf(e));
		stacktrace.append(System.lineSeparator());
		for (int i = 0; i < STACKTRACE_SIZE; i++) {
			stacktrace.append("...");
			stacktrace.append(e.getStackTrace()[i]);
			if (i < STACKTRACE_SIZE - 1) {
				stacktrace.append(System.lineSeparator());
			}
		}
		return String.valueOf(stacktrace);
	}

}
