package top.mrxiaom.sweet.playtime.func;

import org.bukkit.configuration.MemoryConfiguration;
import top.mrxiaom.pluginbase.api.IRunTask;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.playtime.SweetPlaytime;

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
            Long keepTime = parseSeconds(config.getString("cleanup.keep-time"));
            if (keepTime == null) {
                warn("cleanup.keep-time 的值无效");
                return;
            }
            Long checkPeriodSeconds = parseSeconds(config.getString("cleanup.check-period"));
            if (checkPeriodSeconds == null) {
                warn("cleanup.check-period 的值无效");
                return;
            }
            this.keepTimeSeconds = keepTime;
            long period = checkPeriodSeconds * 20L;
            task = plugin.getScheduler().runTaskTimerAsync(() -> {
                LocalDateTime beforeThat = LocalDateTime.now().minusSeconds(keepTimeSeconds);
                plugin.getPlaytimeDatabase().cleanup(beforeThat);
            }, period, period);
        }
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private static Long parseSeconds(String str) {
        if (str == null || str.isEmpty()) return null;
        char[] array = str.toCharArray();
        long seconds = 0L;
        Integer value = null;
        for (char ch : array) {
            if (ch >= '0' && ch <= '9') {
                int num = ch - '0';
                if (value == null) {
                    value = num;
                } else {
                    value = (value * 10) + num;
                }
                continue;
            }
            if (value == null) {
                return null;
            }
            if (ch == 'd') {
                seconds += value * 86400L;
                continue;
            }
            if (ch == 'h') {
                seconds += value * 3600L;
                continue;
            }
            if (ch == 'm') {
                seconds += value * 60L;
                continue;
            }
            if (ch == 's') {
                seconds += value;
                continue;
            }
            return null;
        }
        return seconds;
    }
}
