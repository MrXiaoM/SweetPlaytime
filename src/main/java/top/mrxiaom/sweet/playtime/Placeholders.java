package top.mrxiaom.sweet.playtime;

import com.google.common.collect.Lists;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.depend.PlaceholdersExpansion;
import top.mrxiaom.sweet.playtime.config.Query;
import top.mrxiaom.sweet.playtime.database.PlaytimeDatabase;
import top.mrxiaom.sweet.playtime.func.AbstractPluginHolder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@AutoRegister(requirePlugins = "PlaceholderAPI")
public class Placeholders extends AbstractPluginHolder {
    private Extension extension;
    private String formatSecond, formatSeconds, formatMinute, formatMinutes;
    private String formatHour, formatHours, formatDay, formatDays;
    public Placeholders(SweetPlaytime plugin) {
        super(plugin, true);
    }

    @Override
    protected void register() {
        super.register();
        if (extension == null) {
            extension = new Extension(plugin);
            extension.register();
        }
    }

    @Override
    public void onDisable() {
        if (extension != null) {
            extension.unregister();
            extension = null;
        }
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        this.formatSecond = config.getString("time-format.second", "秒");
        this.formatSeconds = config.getString("time-format.seconds", "秒");
        this.formatMinute = config.getString("time-format.minute", "分");
        this.formatMinutes = config.getString("time-format.minutes", "分");
        this.formatHour = config.getString("time-format.hour", "时");
        this.formatHours = config.getString("time-format.hours", "时");
        this.formatDay = config.getString("time-format.day", "天");
        this.formatDays = config.getString("time-format.days", "天");
    }

    public String formatSeconds(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds / 3600) % 24;
        long minutes = (totalSeconds / 60) % 60;
        long seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days != 0) {
            sb.append(days);
            sb.append(days == 1 ? formatDay : formatDays);
        }
        if (hours != 0) {
            sb.append(hours);
            sb.append(hours == 1 ? formatHour : formatHours);
        }
        if (minutes != 0) {
            sb.append(minutes);
            sb.append(minutes == 1 ? formatMinute : formatMinutes);
        }
        sb.append(seconds);
        sb.append(seconds == 1 ? formatSecond : formatSeconds);
        return sb.toString();
    }

    public class Extension extends PlaceholdersExpansion<SweetPlaytime> {
        protected Extension(SweetPlaytime plugin) {
            super(plugin);
        }

        @Override
        public boolean persist() {
            return true;
        }

        private Long parseAndExecuteQuery(String params, int prefixLength, UUID uuid, AtomicReference<String> error) {
            Query query;
            if (params.length() > prefixLength) {
                String[] args = params.substring(prefixLength).split(",");
                query = Query.parse(Lists.newArrayList(args), error);
            } else {
                query = Query.ALL;
            }
            if (query == null) {
                return null;
            }
            PlaytimeDatabase db = plugin.getPlaytimeDatabase();
            Long fromDb = query.collectPlaytimeWithCache(db, uuid);
            if (fromDb == null) {
                error.set("DATABASE_ERROR");
                return null;
            }
            return fromDb + db.getCurrentOnlineSeconds(uuid);
        }

        @Override
        public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
            AtomicReference<String> error = new AtomicReference<>("UNKNOWN_ERROR");
            if (params.equalsIgnoreCase("query") || params.startsWith("query_")) {
                Long totalSeconds = parseAndExecuteQuery(params, 6, player.getUniqueId(), error);
                if (totalSeconds == null) {
                    return error.get();
                }
                return String.valueOf(totalSeconds);
            }
            if (params.equalsIgnoreCase("format_query") || params.startsWith("format_query_")) {
                Long totalSeconds = parseAndExecuteQuery(params, 13, player.getUniqueId(), error);
                if (totalSeconds == null) {
                    return error.get();
                }
                return formatSeconds(totalSeconds);
            }
            return super.onPlaceholderRequest(player, params);
        }
    }
}
