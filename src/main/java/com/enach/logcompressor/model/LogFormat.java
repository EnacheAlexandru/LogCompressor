package com.enach.logcompressor.model;

import lombok.Getter;

import java.util.List;

@Getter
public class LogFormat {

    private String regex;

    private final String format;

    private List<String> formatTypeList;

    public LogFormat(String regex, String format) {
        this.regex = regex;
        this.format = format;
        if (this.format != null) {
            List<String> logformatTypeList = List.of(format.split("[\\p{Punct}\\p{Blank}]"));
            this.formatTypeList = logformatTypeList.stream().filter(o -> !o.isBlank()).toList();
        }
    }

    public LogFormat(String format) {
        this.format = format;
        if (this.format != null) {
            List<String> logformatTypeList = List.of(format.split("[\\p{Punct}\\p{Blank}]"));
            this.formatTypeList = logformatTypeList.stream().filter(o -> !o.isBlank()).toList();
        }
    }
}
