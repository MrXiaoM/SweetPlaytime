package top.mrxiaom.sweet.playtime.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RewardSets {
    private final @NotNull String id;
    private final @Nullable String permission;
    private final @NotNull List<String> query;
    private final @NotNull EnumOutdatePeriod statusOutdatePeriod;
    private final @Nullable Long autoClaimPeriod;
    private final @NotNull List<Reward> rewards;
    public RewardSets(@NotNull String id, @NotNull ConfigurationSection config) {
        this.id = id;
        String permission = config.getString("permission", "none");
        if (permission.equalsIgnoreCase("none")) {
            this.permission = null;
        } else {
            this.permission = permission;
        }
        this.query = config.getStringList("query");
        AtomicReference<String> error = new AtomicReference<>("UNKNOWN_ERROR");
        if (Query.parse(query, error) == null) {
            throw new IllegalArgumentException("查询参数 query 解析错误: " + error.get());
        }
        EnumOutdatePeriod statusOutdatePeriod = Util.valueOrNull(EnumOutdatePeriod.class, config.getString("status-outdate-period"));
        if (statusOutdatePeriod == null) {
            throw new IllegalArgumentException("status-outdate-period 的数值无效");
        }
        this.statusOutdatePeriod = statusOutdatePeriod;
        String autoClaimPeriod = config.getString("auto-claim-period", "none");
        if (autoClaimPeriod.equalsIgnoreCase("none")) {
            this.autoClaimPeriod = null;
        } else {
            Long autoClaimPeriodSeconds = Query.parseSeconds(autoClaimPeriod);
            if (autoClaimPeriodSeconds == null) {
                throw new IllegalArgumentException("auto-claim-period 的数值无效");
            }
            this.autoClaimPeriod = autoClaimPeriodSeconds;
        }
        this.rewards = new ArrayList<>();
        for (ConfigurationSection section : ConfigUtils.getSectionList(config, "rewards")) {
            this.rewards.add(new Reward(section));
        }
    }

    public @NotNull String getId() {
        return id;
    }

    public @Nullable String getPermission() {
        return permission;
    }

    public boolean hasPermission(@NotNull Permissible p) {
        return permission == null || p.hasPermission(permission);
    }

    public @NotNull Query createQuery() {
        Query query = Query.parse(this.query, null);
        if (query == null) {
            throw new IllegalStateException("预料中的错误: 奖励配置 " + id + " 的查询参数 query 解析错误");
        }
        return query;
    }

    public @NotNull EnumOutdatePeriod getStatusOutdatePeriod() {
        return statusOutdatePeriod;
    }

    public @Nullable Long getAutoClaimPeriod() {
        return autoClaimPeriod;
    }

    public @NotNull List<Reward> getRewards() {
        return rewards;
    }
}
