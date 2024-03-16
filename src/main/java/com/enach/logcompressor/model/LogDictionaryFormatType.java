package com.enach.logcompressor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class LogDictionaryFormatType {

    private Map<String, Long> keyMap;

    private List<Long> orderList;

}
