package top.mrxiaom.sweet.playtime.config;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IRunTask;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playtime.SweetPlaytime;
import top.mrxiaom.sweet.playtime.database.PlaytimeDatabase;
import top.mrxiaom.sweet.playtime.database.RewardStatusDatabase;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class RewardSets {
    private final @NotNull SweetPlaytime plugin;
    private final @NotNull String id;
    private final @Nullable String permission;
    private final @NotNull List<String> query;
    private final @NotNull OutdatePeriod statusOutdatePeriod;
    private final @Nullable Long autoClaimPeriod;
    private final @NotNull List<Reward> rewards;
    private IRunTask checkPeriodTask;
    public RewardSets(@NotNull SweetPlaytime plugin, @NotNull String id, @NotNull ConfigurationSection config) {
        this.plugin = plugin;
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
        this.statusOutdatePeriod = parsePeriod(config.getString("status-outdate-period", "auto"));
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
            this.rewards.add(new Reward(this, section));
        }
        this.rewards.sort(Comparator.comparingLong(Reward::getDurationSeconds));

        if (this.autoClaimPeriod != null) {
            IRunTask task = plugin.getScheduler().runTaskTimerAsync(this::checkAutoClaim, this.autoClaimPeriod, this.autoClaimPeriod);
            setCheckPeriodTask(task);
        }
    }

    public void checkAutoClaim() {
        LocalDateTime outdateTime = statusOutdatePeriod.getNextOutdateDateTime();
        if (outdateTime.isBefore(LocalDateTime.now())) return;
        Map<Player, List<Long>> durationMap = new HashMap<>();
        // 执行自动领取检查任务
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!hasPermission(player)) continue;
            List<Long> durationList = doAutoClaim(player);
            if (durationList != null && !durationList.isEmpty()) {
                durationMap.put(player, durationList);
            }
        }
        // 提交领取成功的记录到数据库
        if (!durationMap.isEmpty()) plugin.getScheduler().runTaskAsync(() -> {
            RewardStatusDatabase db = plugin.getRewardStatusDatabase();
            try (Connection conn = plugin.getConnection()) {
                for (Map.Entry<Player, List<Long>> entry : durationMap.entrySet()) {
                    Player player = entry.getKey();
                    db.markClaimed(conn, player, id, entry.getValue(), outdateTime);
                }
            } catch (SQLException e) {
                db.warn(e);
            }
        });
    }

    /**
     * 执行自动领取操作，如果领取成功，提交到数据库
     * @param player 待领取奖励的玩家
     * @return 是否有领取到奖励
     */
    public boolean doClaimAndSubmit(Player player) {
        List<Long> durationList = doAutoClaim(player);
        if (durationList != null && !durationList.isEmpty()) {
            plugin.getScheduler().runTaskAsync(() -> {
                LocalDateTime outdateTime = statusOutdatePeriod.getNextOutdateDateTime();
                plugin.getRewardStatusDatabase().markClaimed(player, id, durationList, outdateTime);
            });
            return true;
        }
        return false;
    }

    /**
     * 执行自动领取奖励操作
     * @param player 待领取奖励的玩家
     * @return 领取成功的累计在线时长，如果数据库调用失败，返回 <code>null</code>
     */
    public List<Long> doAutoClaim(Player player) {
        PlaytimeDatabase pdb = plugin.getPlaytimeDatabase();
        RewardStatusDatabase rdb = plugin.getRewardStatusDatabase();
        UUID uuid = player.getUniqueId();
        // 获取玩家当前在线时间
        Long fromDb = createQuery().collectPlaytimeWithCache(pdb, uuid);
        if (fromDb == null) return null;
        long seconds = fromDb + pdb.getCurrentOnlineSeconds(uuid);
        // 获取玩家已经领取过的奖励列表
        List<Long> claimed = rdb.getClaimedWithCache(uuid, id);
        List<Long> durationList = new ArrayList<>();
        for (Reward reward : rewards) {
            long duration = reward.getDurationSeconds();
            if (claimed.contains(duration)) continue;
            if (seconds >= duration) { // 累计在线时间到了，执行奖励命令
                durationList.add(duration);
                ActionProviders.run(plugin, player, reward.getRewardActions());
            }
        }
        return durationList;
    }

    public @NotNull SweetPlaytime getPlugin() {
        return plugin;
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

    public @NotNull OutdatePeriod getStatusOutdatePeriod() {
        return statusOutdatePeriod;
    }

    public @Nullable Long getAutoClaimPeriod() {
        return autoClaimPeriod;
    }

    public @NotNull List<Reward> getRewards() {
        return rewards;
    }

    public void cancelCheckPeriodTask() {
        if (checkPeriodTask != null) {
            checkPeriodTask.cancel();
            checkPeriodTask = null;
        }
    }

    public IRunTask getCheckPeriodTask() {
        return checkPeriodTask;
    }

    public void setCheckPeriodTask(IRunTask checkPeriodTask) {
        this.checkPeriodTask = checkPeriodTask;
    }

    private OutdatePeriod parsePeriod(String str) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("status-outdate-period 的数值无效");
        }
        if (str.equalsIgnoreCase("auto")) {
            return new AutoOutdatePeriod(this::createQuery);
        }
        if (str.toLowerCase().startsWith("fixed ")) {
            try {
                LocalDate endDate = LocalDate.parse(str.substring(6), DateTimeFormatter.ISO_LOCAL_DATE);
                return new FixedOutdatePeriod(endDate);
            } catch (Throwable ignored) {
            }
            throw new IllegalArgumentException("status-outdate-period 的数值无效");
        }
        EnumOutdatePeriod byEnum = Util.valueOrNull(EnumOutdatePeriod.class, str);
        if (byEnum == null) {
            throw new IllegalArgumentException("status-outdate-period 的数值无效");
        }
        return byEnum;
    }
}
