package top.mrxiaom.sweet.playtime.config;

import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playtime.database.PlaytimeDatabase;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class Query {
    public static final Query ALL = new Query(null, null, null);
    private final @Nullable String tag;
    private final @Nullable LocalDate startDate;
    private final @Nullable LocalDate endDate;
    public Query(@Nullable String tag, @Nullable LocalDate startDate, @Nullable LocalDate endDate) {
        this.tag = tag;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public @Nullable String getTag() {
        return tag;
    }

    public @Nullable LocalDate getStartDate() {
        return startDate;
    }

    public @Nullable LocalDate getEndDate() {
        return endDate;
    }

    /**
     * @see PlaytimeDatabase#collectPlaytimeWithCache(UUID, String, LocalDate, LocalDate)
     */
    public @Nullable Long collectPlaytimeWithCache(PlaytimeDatabase db, UUID playerUUID) {
        return db.collectPlaytimeWithCache(playerUUID, tag, startDate, endDate);
    }

    @Nullable
    public static Query parse(List<String> args, @Nullable AtomicReference<String> error) {
        String tag = null;
        LocalDate startDate = null, endDate = null;
        for (String arg : args) {
            String str = arg.trim();
            if (str.equalsIgnoreCase("today")) {
                startDate = LocalDate.now();
                continue;
            }
            DayOfWeek dayOfWeek = Util.valueOrNull(DayOfWeek.class, str);
            if (dayOfWeek != null) {
                LocalDate now = LocalDate.now();
                LocalDate monday = now.minusDays(now.getDayOfWeek().getValue() - 1);
                startDate = monday.plusDays(dayOfWeek.getValue() - 1);
            }
            if (!str.contains("=")) continue;
            int i = str.indexOf('=');
            String key = str.substring(0, i);
            String value = str.substring(i + 1);
            if (key.equals("tag")) {
                tag = value;
                continue;
            }
            if (key.equals("start")) {
                try {
                    startDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (DateTimeParseException e) {
                    if (error != null) error.set("WRONG_START_DATE_FORMAT");
                    return null;
                }
            }
            if (key.equals("end")) {
                try {
                    endDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (DateTimeParseException e) {
                    if (error != null) error.set("WRONG_END_DATE_FORMAT");
                    return null;
                }
            }
        }
        return new Query(tag, startDate, endDate);
    }

    public static Long parseSeconds(String str) {
        if (str == null || str.isEmpty()) return null;
        char[] array = str.toCharArray();
        long seconds = 0L;
        Integer value = null;
        for (char ch : array) {
            if (ch >= '0' && ch <= '9') {
                int num = ch - '0';
                if (value == null) {
                    value = num;
                } else {
                    value = (value * 10) + num;
                }
                continue;
            }
            if (value == null) {
                return null;
            }
            if (ch == 'd') {
                seconds += value * 86400L;
                continue;
            }
            if (ch == 'h') {
                seconds += value * 3600L;
                continue;
            }
            if (ch == 'm') {
                seconds += value * 60L;
                continue;
            }
            if (ch == 's') {
                seconds += value;
                continue;
            }
            return null;
        }
        return seconds;
    }
}
