package top.mrxiaom.sweet.playtime.config;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.function.Supplier;

public class AutoOutdatePeriod implements OutdatePeriod {
    private final Supplier<Query> createQuery;
    public AutoOutdatePeriod(Supplier<Query> createQuery) {
        this.createQuery = createQuery;
        if (createQuery.get().getEndDate() == null) {
            throw new IllegalArgumentException("query 的 endDate 无效");
        }
    }

    @Override
    public @NotNull LocalDate getNextOutdateDate() {
        LocalDate endDate = createQuery.get().getEndDate();
        if (endDate == null) {
            throw new IllegalStateException("query 的 endDate 无效");
        }
        return endDate;
    }
}
