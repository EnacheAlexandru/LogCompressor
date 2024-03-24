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

    public String formatCurrentLikeKey() {
        String[] sepList = key.split("[:,.]");
        String sepStr = key.replaceAll("\\d", "");
        String currStr = String.valueOf(current);

        StringBuilder format = new StringBuilder();
        int start = 0;
        for (int i = 0; i < sepList.length - 1; i++) {
            int len = sepList[i].length();
            String sub = currStr.substring(start, start + len);
            format.append(sub);
            format.append(sepStr.charAt(i));
            start += len;
        }
        String last = currStr.substring(start);
        format.append(last);

        return String.valueOf(format);
    }

}
