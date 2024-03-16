package com.enach.logcompressor.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class LogNumericFormatType {

    private String key;

    private Long start;

    private Long current;

    @Setter(AccessLevel.NONE)
    private List<Long> deltaList;

}
