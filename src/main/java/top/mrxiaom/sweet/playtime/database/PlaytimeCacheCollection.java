package top.mrxiaom.sweet.playtime.database;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApiStatus.Internal
public class PlaytimeCacheCollection {
    private final UUID uuid;
    private final Map<String, Long> cacheMap = new HashMap<>();
    public PlaytimeCacheCollection(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Nullable
    public Long getCache(@Nullable String tag, @Nullable LocalDate startDate, @Nullable LocalDate endDate) {
        LocalDateTime startTime = startDate == null ? null : startDate.atTime(0, 0);
        LocalDateTime endTime = endDate == null ? null : endDate.atTime(0, 0);
        return getCache(tag, startTime, endTime);
    }

    @Nullable
    public Long getCache(@Nullable String tag, @Nullable LocalDateTime startTime, @Nullable LocalDateTime endTime) {
        String key = key(tag, startTime, endTime);
        return cacheMap.get(key);
    }

    public void putCache(@Nullable String tag, @Nullable LocalDateTime startTime, @Nullable LocalDateTime endTime, long seconds) {
        String key = key(tag, startTime, endTime);
        putCache(key, seconds);
    }

    public void putCache(String key, long seconds) {
        cacheMap.put(key, seconds);
    }

    public static String key(@Nullable String tag, @Nullable LocalDateTime startTime, @Nullable LocalDateTime endTime) {
        StringBuilder sb = new StringBuilder();
        if (tag != null) sb.append(tag);
        sb.append(";");
        if (startTime != null) sb.append(startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        sb.append(";");
        if (endTime != null) sb.append(endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        sb.append(";");
        return sb.toString();
    }
}
