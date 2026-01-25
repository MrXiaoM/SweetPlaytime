package top.mrxiaom.sweet.playtime.config;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface OutdatePeriod {
    @NotNull
    LocalDate getNextOutdateDate();

    @NotNull
    default LocalDateTime getNextOutdateDateTime() {
        return getNextOutdateDate().atTime(0, 0);
    }
}
