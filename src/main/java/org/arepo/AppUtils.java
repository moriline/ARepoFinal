package org.arepo;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class AppUtils {
    public static Long getCurrentTimeInMillis() {
        Instant instant = Instant.now();
        Timestamp timestamp = Timestamp.from(instant);
        return timestamp.getTime();
    }

    public static String convertUTC(Long timestamp) {
        var instant = Instant.ofEpochMilli(timestamp);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        formatter = formatter.withZone(TimeZone.getTimeZone("UTC").toZoneId());
        return formatter.format(instant);
    }
    public static String convertLocal(Long timestamp){
        var instant = Instant.ofEpochMilli(timestamp);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        formatter = formatter.withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }
}
