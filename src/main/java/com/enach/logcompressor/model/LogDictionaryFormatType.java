package com.enach.logcompressor.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class LogDictionaryFormatType {

    private final Map<String, Long> mappings = new HashMap<>();

    private final List<Long> order = new ArrayList<>();

}
