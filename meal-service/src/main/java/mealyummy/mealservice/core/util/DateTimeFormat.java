package mealyummy.mealservice.core.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateTimeFormat {
    public static String formatInstantCustom(Instant instant) {
        if (instant == null) return null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, HH:mm:ss dd-MM-yyyy")
                .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .withLocale(Locale.forLanguageTag("vi-VN"));

        return formatter.format(instant);
    }
}
