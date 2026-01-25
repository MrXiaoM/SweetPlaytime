package top.mrxiaom.sweet.playtime.func;

import org.bukkit.configuration.MemoryConfiguration;
import top.mrxiaom.pluginbase.api.IRunTask;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.playtime.SweetPlaytime;
import top.mrxiaom.sweet.playtime.config.Query;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

@AutoRegister
public class CleanupManager extends AbstractModule {
    private long keepTimeSeconds;
    private IRunTask task;
    public CleanupManager(SweetPlaytime plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (config.getBoolean("cleanup.enable")) {
            Long keepTime = Query.parseSeconds(config.getString("cleanup.keep-time"));
            if (keepTime == null) {
                warn("cleanup.keep-time 的值无效");
                return;
            }
            Long checkPeriodSeconds = Query.parseSeconds(config.getString("cleanup.check-period"));
            if (checkPeriodSeconds == null) {
                warn("cleanup.check-period 的值无效");
                return;
            }
            this.keepTimeSeconds = keepTime;
            long period = checkPeriodSeconds * 20L;
            task = plugin.getScheduler().runTaskTimerAsync(this::cleanup, period, period);
        }
    }

    public void cleanup() {
        if (keepTimeSeconds == 0L) return;
        LocalDateTime beforeThat = LocalDateTime.now().minusSeconds(keepTimeSeconds);
        try (Connection conn = plugin.getConnection()) {
            plugin.getPlaytimeDatabase().cleanup(conn, beforeThat);
            plugin.getRewardStatusDatabase().cleanup(conn);
        } catch (SQLException e) {
            warn(e);
        }
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public static CleanupManager inst() {
        return instanceOf(CleanupManager.class);
    }
}
