package top.mrxiaom.sweet.playtime.config;

import org.jetbrains.annotations.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.function.Supplier;

public enum EnumOutdatePeriod implements OutdatePeriod {
    DAILY(() -> LocalDate.now().plusDays(1)),
    WEEKLY(() -> {
        LocalDate nowDate = LocalDate.now();
        DayOfWeek dayOfWeek = nowDate.getDayOfWeek();
        return nowDate.plusWeeks(1).minusDays(dayOfWeek.getValue() - 1);
    }),
    MONTHLY(() -> LocalDate.now().withDayOfMonth(1).plusMonths(1)),
    YEARLY(() -> LocalDate.now().withDayOfYear(1).plusYears(1)),
    FOREVER(() -> LocalDate.now().plusYears(100).withDayOfYear(1)),

    ;
    private final Supplier<LocalDate> impl;
    EnumOutdatePeriod(Supplier<LocalDate> impl) {
        this.impl = impl;
    }

    @NotNull
    public LocalDate getNextOutdateDate() {
        return impl.get();
    }
}
