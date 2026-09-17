package com.erp.carmusic;

import java.util.*;
import java.util.regex.*;

public final class LrcParser {
    public static class Line {
        public final long ms;
        public final String text;
        public Line(long ms, String text) { this.ms = ms; this.text = text; }
    }

    private static final Pattern TAG = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:\\.(\\d{1,3}))?\\](.*)");

    public static List<Line> parse(String lrc) {
        List<Line> out = new ArrayList<>();
        if (lrc == null) return out;
        for (String raw : lrc.split("\\r?\\n")) {
            Matcher m = TAG.matcher(raw.trim());
            if (!m.matches()) continue;
            long min = Long.parseLong(m.group(1));
            long sec = Long.parseLong(m.group(2));
            String frac = m.group(3);
            long ms = (min * 60 + sec) * 1000L;
            if (frac != null) {
                if (frac.length() == 1) ms += Long.parseLong(frac) * 100;
                else if (frac.length() == 2) ms += Long.parseLong(frac) * 10;
                else ms += Long.parseLong(frac.substring(0, 3));
            }
            out.add(new Line(ms, m.group(4).trim()));
        }
        Collections.sort(out, Comparator.comparingLong(x -> x.ms));
        return out;
    }

    public static int indexAt(List<Line> lines, long pos) {
        int idx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).ms <= pos) idx = i;
            else break;
        }
        return idx;
    }
}
