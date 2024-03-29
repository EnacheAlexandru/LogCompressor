package com.enach.logcompressor.model;

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

    private List<Long> deltaList;

    private Boolean isFixedLength;

    private String numSeparatorsRegex;

    public String formatCurrentLikeKey() {
        String[] sepList = key.split(numSeparatorsRegex);
        int keySize = key.replaceAll(numSeparatorsRegex, "").length();
        String sepStr = key.replaceAll("\\d", "");
        StringBuilder currStr = new StringBuilder(String.valueOf(current));
        int currStrSize = currStr.length();
        if (isFixedLength || (!sepStr.isEmpty() && currStrSize < keySize)) {
            for (int i = 0; i < keySize - currStrSize; i++) {
                currStr.insert(0, "0");
            }
        }

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
