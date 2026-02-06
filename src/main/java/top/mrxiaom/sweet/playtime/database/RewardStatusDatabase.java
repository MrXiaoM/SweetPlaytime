package top.mrxiaom.sweet.playtime.database;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.sweet.playtime.SweetPlaytime;
import top.mrxiaom.sweet.playtime.func.AbstractPluginHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RewardStatusDatabase extends AbstractPluginHolder implements IDatabase, Listener {
    private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private String TABLE_NAME;
    private final Map<UUID, RewardStatusCacheCollection> cacheMap = new HashMap<>();

    public RewardStatusDatabase(SweetPlaytime plugin) {
        super(plugin);
        register();
        registerEvents();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        cacheMap.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cacheMap.remove(e.getPlayer().getUniqueId());
    }

    @Override
    public void reload(Connection conn, String tablePrefix) throws SQLException {
        TABLE_NAME = tablePrefix + "reward_status";
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_NAME + "`(" +
                        "`uuid` VARCHAR(48)," +
                        "`name` VARCHAR(48)," +
                        "`reward_sets_id` VARCHAR(48)," +
                        "`duration` INT," +
                        "`outdate_time` DATETIME" +
                ");"
        )) {
            ps.execute();
        }
    }

    @NotNull
    private RewardStatusCacheCollection getOrCreateCache(UUID playerUUID) {
        RewardStatusCacheCollection exists = cacheMap.get(playerUUID);
        if (exists != null) return exists;
        RewardStatusCacheCollection newOne = new RewardStatusCacheCollection(playerUUID);
        cacheMap.put(playerUUID, newOne);
        return newOne;
    }

    /**
     * 获取已领取的累计时间列表
     * @see RewardStatusDatabase#getClaimed(UUID, String)
     */
    public List<Long> getClaimedWithCache(UUID playerUUID, String rewardSetsId) {
        List<Long> exists = getOrCreateCache(playerUUID).getCache(rewardSetsId);
        if (exists != null) return exists;
        return getClaimed(playerUUID, rewardSetsId);
    }

    /**
     * 获取已领取的累计时间列表
     * @param playerUUID 玩家 UUID
     * @param rewardSetsId 奖励集合 ID，即奖励配置的文件名
     * @return 如果数据库调用失败，返回 <code>null</code>
     */
    @Nullable
    public List<Long> getClaimed(UUID playerUUID, String rewardSetsId) {
        String now = LocalDateTime.now().format(timeFormat);
        try (Connection conn = plugin.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM `" + TABLE_NAME + "` WHERE `uuid`=? AND `reward_sets_id`=? AND `outdate_time` > '" + now + "';"
        )) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, rewardSetsId);
            List<Long> durationList = new ArrayList<>();
            try (ResultSet result = ps.executeQuery()) {
                while (result.next()) {
                    durationList.add(result.getLong("duration"));
                }
            }
            getOrCreateCache(playerUUID).putCache(rewardSetsId, durationList);
            return durationList;
        } catch (SQLException e) {
            warn(e);
            return null;
        }
    }

    public void markClaimed(
            String rewardSetsId,
            Map<Player, List<Long>> durationMap,
            LocalDateTime outdateTime
    ) {
        for (Map.Entry<Player, List<Long>> entry : durationMap.entrySet()) {
            RewardStatusCacheCollection cache = getOrCreateCache(entry.getKey().getUniqueId());
            List<Long> exists = cache.getCache(rewardSetsId);
            if (exists != null) {
                exists.addAll(entry.getValue());
                cache.putCache(rewardSetsId, exists);
            } else {
                cache.putCache(rewardSetsId, entry.getValue());
            }
        }
        plugin.getScheduler().runTaskAsync(() -> {
            RewardStatusDatabase db = plugin.getRewardStatusDatabase();
            try (Connection conn = plugin.getConnection()) {
                for (Map.Entry<Player, List<Long>> entry : durationMap.entrySet()) {
                    Player player = entry.getKey();
                    db.markClaimed(conn, player, rewardSetsId, entry.getValue(), outdateTime);
                }
            } catch (SQLException e) {
                db.warn(e);
            }
        });
    }

    public void markClaimed(
            Player player,
            String rewardSetsId,
            List<Long> durationList,
            LocalDateTime outdateTime
    ) {
        try (Connection conn = plugin.getConnection()) {
            markClaimed(conn, player, rewardSetsId, durationList, outdateTime);
        } catch (SQLException e) {
            warn(e);
        }
    }

    public void markClaimed(
            Connection conn,
            Player player,
            String rewardSetsId,
            List<Long> durationList,
            LocalDateTime outdateTime
    ) throws SQLException {
        if (durationList.isEmpty()) return;
        String outdate = outdateTime.format(timeFormat);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO `" + TABLE_NAME + "`" +
                        "(`uuid`,`name`,`reward_sets_id`,`duration`,`outdate_time`) " +
                        "VALUES(?,?,?,?,'" + outdate + "');"
        )) {
            UUID uuid = player.getUniqueId();
            String name = player.getName();
            for (long duration : durationList) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setString(3, rewardSetsId);
                ps.setLong(4, duration);
                ps.addBatch();
            }
            ps.executeBatch();
            getOrCreateCache(uuid).removeCache(rewardSetsId);
        }
    }

    public void cleanup() {
        try (Connection conn = plugin.getConnection()) {
            cleanup(conn);
        } catch (SQLException e) {
            warn(e);
        }
    }

    public void cleanup(Connection conn) throws SQLException {
        String now = LocalDateTime.now().format(timeFormat);
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM `" + TABLE_NAME + "` WHERE `outdate_time` < '" + now + "';"
        )) {
            ps.execute();
        }
    }
}
