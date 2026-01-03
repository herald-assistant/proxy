package com.acme.herald.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JqlUtils {
    private static final Pattern CUSTOMFIELD = Pattern.compile("^customfield_(\\d+)$");

    public static String toJqlField(String fieldIdOrName) {
        String f = fieldIdOrName == null ? "" : fieldIdOrName.trim();
        Matcher m = CUSTOMFIELD.matcher(f);
        if (m.matches()) return "cf[" + m.group(1) + "]";
        return f;
    }

    public static String escapeJql(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
