package top.mrxiaom.sweet.playtime.database;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.sweet.playtime.SweetPlaytime;
import top.mrxiaom.sweet.playtime.func.AbstractPluginHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PlaytimeDatabase extends AbstractPluginHolder implements IDatabase, Listener {
    private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private String TABLE_NAME;
    private final Map<UUID, Playtime> players = new HashMap<>();
    private LocalDate lastDate = LocalDate.now();
    public PlaytimeDatabase(SweetPlaytime plugin) {
        super(plugin);
        register();
        registerEvents();
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.put(player.getUniqueId(), new Playtime(player));
        }
        plugin.getScheduler().runTaskTimerAsync(() -> {
            // 每天0点至少要保存一次在线时间数据
            LocalDate nowDate = LocalDate.now();
            if (isEqual(nowDate, lastDate)) return;
            lastDate = nowDate;

            LocalDateTime now = nowDate.atTime(0, 0);
            submit(players.values(), now);
            // 提交后重设记录的开始时间
            for (Playtime playtime : players.values()) {
                playtime.setStartRecordTime(now);
            }
        }, 20L, 20L);
    }
    private boolean isEqual(LocalDate a, LocalDate b) {
        return a.getYear() == b.getYear() && a.getDayOfYear() == b.getDayOfYear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        players.put(player.getUniqueId(), new Playtime(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Playtime playtime = players.remove(e.getPlayer().getUniqueId());
        if (playtime != null) {
            // TODO: 需要验证是否需要在 onDisable 时提交数据
            plugin.getScheduler().runTaskAsync(() -> submit(playtime, LocalDateTime.now()));
        }
    }

    @Override
    public void reload(Connection conn, String tablePrefix) throws SQLException {
        TABLE_NAME = tablePrefix + "playtime";
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_NAME + "`(" +
                        "`uuid` VARCHAR(48)," +
                        "`name` VARCHAR(48)," +
                        "`tag` VARCHAR(48)," +
                        "`record_start_time` DATETIME," +
                        "`record_save_time` DATETIME," +
                        "`played_seconds` INT" +
                ");"
        )) {
            ps.execute();
        }
    }

    /**
     * 获取玩家游玩时间 (秒)
     * @param playerUUID 玩家 UUID
     * @param tag 要求搜索标签
     * @param startDate 要求起始时间
     * @param endDate 要求结束时间
     * @return 返回玩家游玩时间秒数，如果数据库调用异常，返回 <code>null</code>
     */
    @Nullable
    public Long collectPlaytimeSeconds(UUID playerUUID, @Nullable String tag, @Nullable LocalDate startDate, @Nullable LocalDate endDate) {
        LocalDateTime startTime = startDate == null ? null : startDate.atTime(0, 0);
        LocalDateTime endTime = endDate == null ? null : endDate.atTime(0, 0);
        return collectPlaytimeSeconds(playerUUID, tag, startTime, endTime);
    }

    /**
     * 获取玩家游玩时间 (秒)
     * @param playerUUID 玩家 UUID
     * @param tag 要求搜索标签
     * @param startTime 要求起始时间
     * @param endTime 要求结束时间
     * @return 返回玩家游玩时间秒数，如果数据库调用异常，返回 <code>null</code>
     */
    @Nullable
    public Long collectPlaytimeSeconds(UUID playerUUID, @Nullable String tag, @Nullable LocalDateTime startTime, @Nullable LocalDateTime endTime) {
        try (Connection conn = plugin.getConnection()) {
            return collectPlaytimeSeconds(conn, playerUUID, tag, startTime, endTime);
        } catch (SQLException e) {
            warn(e);
            return null;
        }
    }

    public long collectPlaytimeSeconds(Connection conn, UUID playerUUID, @Nullable String tag, @Nullable LocalDateTime startTime, @Nullable LocalDateTime endTime) throws SQLException {
        List<String> conditions = new ArrayList<>();
        conditions.add("`uuid`=?");
        if (tag != null) {
            conditions.add("`tag`=?");
        }
        if (startTime != null) {
            String value = startTime.format(timeFormat);
            conditions.add("`record_start_time` >= '" + value + "'");
        }
        if (endTime != null) {
            String value = endTime.format(timeFormat);
            conditions.add("`record_start_time` < '" + value + "'");
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM `" + TABLE_NAME + "` WHERE " + String.join(" AND ", conditions) + ";"
        )) {
            ps.setString(1, playerUUID.toString());
            if (tag != null) {
                ps.setString(2, tag);
            }
            long seconds = 0L;
            // 收集数据库记录的在线时间
            try (ResultSet result = ps.executeQuery()) {
                while (result.next()) {
                    seconds += result.getLong("played_seconds");
                }
            }
            // 如果玩家在线，把玩家当前在线时间也加进去
            Playtime playtime = players.get(playerUUID);
            if (playtime != null) {
                seconds += playtime.getPlayedSeconds();
            }
            return seconds;
        }
    }

    /**
     * 提交游玩时间数据
     */
    public void submit(Collection<Playtime> collection, LocalDateTime now) {
        try (Connection conn = plugin.getConnection()) {
            for (Playtime playtime : collection) {
                submit(conn, playtime, now);
            }
        } catch (SQLException e) {
            warn(e);
        }
    }

    /**
     * 提交游玩时间数据
     */
    public void submit(Playtime playtime, LocalDateTime now) {
        try (Connection conn = plugin.getConnection()) {
            submit(conn, playtime, now);
        } catch (SQLException e) {
            warn(e);
        }
    }

    public void submit(Connection conn, Playtime playtime, LocalDateTime now) throws SQLException {
        String startTime = playtime.getStartRecordTime().format(timeFormat);
        String nowTime = now.format(timeFormat);
        long playedSeconds = playtime.getPlayedSeconds(now);
        if (playedSeconds < 1) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO `" + TABLE_NAME + "`" +
                        "(`uuid`,`name`,`tag`,`record_start_time`,`record_save_time`,`played_seconds`) " +
                        "VALUES(?,?,?,'" + startTime + "','" + nowTime + "',?);"
        )) {
            ps.setString(1, playtime.getPlayer().getUniqueId().toString());
            ps.setString(2, playtime.getPlayer().getName());
            ps.setString(3, plugin.tag());
            ps.setLong(4, playedSeconds);
            ps.executeUpdate();
        }
    }

    /**
     * 清理在线时间数据
     * @param beforeThat 要清理早于什么时候记录的数据
     */
    public void cleanup(LocalDateTime beforeThat) {
        String before = beforeThat.format(timeFormat);
        try (Connection conn = plugin.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM `" + TABLE_NAME + "` WHERE `record_start_time` < '" + before + "';"
        )) {
            ps.execute();
        } catch (SQLException e) {
            warn(e);
        }
    }
}
