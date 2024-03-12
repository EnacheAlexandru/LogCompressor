package com.enach.logcompressor.model;

import lombok.Getter;

@Getter
public enum LogFormatType {
    REP("rep"),
    NUM("num"),
    DICT("dict"),
    MSG("msg");

    private final String formatType;

    LogFormatType(String formatType) {
        this.formatType = formatType;
    }
}
