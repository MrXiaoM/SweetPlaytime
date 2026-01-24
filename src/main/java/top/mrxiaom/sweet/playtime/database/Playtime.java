package top.mrxiaom.sweet.playtime.database;

import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 在线玩家的当前在线时间缓存
 */
public class Playtime {
    private final Player player;
    private LocalDateTime startRecordTime;

    public Playtime(Player player) {
        this.player = player;
        this.startRecordTime = LocalDateTime.now();
    }

    public long getPlayedSeconds() {
        return getPlayedSeconds(LocalDateTime.now());
    }

    public long getPlayedSeconds(LocalDateTime now) {
        long duration = now.toEpochSecond(ZoneOffset.UTC) - startRecordTime.toEpochSecond(ZoneOffset.UTC);
        return Math.max(0, duration);
    }

    public Player getPlayer() {
        return player;
    }

    public LocalDateTime getStartRecordTime() {
        return startRecordTime;
    }

    public void setStartRecordTime(LocalDateTime startRecordTime) {
        this.startRecordTime = startRecordTime;
    }
}
