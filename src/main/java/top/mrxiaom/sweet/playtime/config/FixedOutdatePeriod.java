package top.mrxiaom.sweet.playtime.config;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;

public class FixedOutdatePeriod implements OutdatePeriod {
    private final LocalDate endDate;
    public FixedOutdatePeriod(@NotNull LocalDate endDate) {
        this.endDate = endDate;
    }

    @Override
    public @NotNull LocalDate getNextOutdateDate() {
        return endDate;
    }
}
