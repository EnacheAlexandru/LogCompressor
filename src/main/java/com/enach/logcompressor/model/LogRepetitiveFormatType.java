package com.enach.logcompressor.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class LogRepetitiveFormatType {

    private String key;

    private Long times;

}
