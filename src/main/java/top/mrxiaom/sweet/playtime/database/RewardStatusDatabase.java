package top.mrxiaom.sweet.playtime.database;

import org.bukkit.entity.Player;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RewardStatusDatabase extends AbstractPluginHolder implements IDatabase {
    private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private String TABLE_NAME;

    public RewardStatusDatabase(SweetPlaytime plugin) {
        super(plugin);
        register();
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

    /**
     * 获取已领取的累计时间列表
     * @see RewardStatusDatabase#getClaimed(UUID, String)
     */
    public List<Long> getClaimedWithCache(UUID playerUUID, String rewardSetsId) {
        // TODO: 实现缓存功能
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
            return durationList;
        } catch (SQLException e) {
            warn(e);
            return null;
        }
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
            for (long duration : durationList) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setString(2, player.getName());
                ps.setString(3, rewardSetsId);
                ps.setLong(4, duration);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void cleanup() {
        String now = LocalDateTime.now().format(timeFormat);
        try (Connection conn = plugin.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM `" + TABLE_NAME + "` WHERE `outdate_time` < '" + now + "';"
        )) {
            ps.execute();
        } catch (SQLException e) {
            warn(e);
        }
    }
}
