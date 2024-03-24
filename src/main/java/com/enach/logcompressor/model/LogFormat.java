package com.enach.logcompressor.model;

import lombok.Getter;

import java.util.List;

@Getter
public class LogFormat {

    private final String regex;

    private final String format;

    private List<String> formatTypeList;

    private final String lineSeparatorsRegex;

    public LogFormat(String regex, String format, String lineSeparatorsRegex) {
        this.regex = regex;
        this.format = format;
        this.lineSeparatorsRegex = lineSeparatorsRegex;
        if (this.format != null) {
            List<String> logformatTypeList = List.of(format.split(lineSeparatorsRegex));
            this.formatTypeList = logformatTypeList.stream().filter(o -> !o.isBlank()).toList();
        }
    }

}
