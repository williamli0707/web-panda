package com.github.williamli0707.webpanda.records;

import java.sql.Date;
import java.text.SimpleDateFormat;

public record Attempt(long timestamp, String code, int index) implements Comparable<Attempt> {
    private static SimpleDateFormat sdf = new SimpleDateFormat("E MMMM d h:m:s a z y");
    @Override
    public int compareTo(Attempt o) {
        return Long.compare(timestamp, o.timestamp);
    }

    @Override
    public String toString() {
        return "Attempt " + index + " (" + sdf.format(new Date(timestamp)) + "): \n" + code + "\n";
    }
}
