package top.mrxiaom.sweet.playtime;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.CollectionUtils;
import top.mrxiaom.pluginbase.utils.depend.PlaceholdersExpansion;
import top.mrxiaom.sweet.playtime.config.Query;
import top.mrxiaom.sweet.playtime.config.RewardSets;
import top.mrxiaom.sweet.playtime.config.TimeFormat;
import top.mrxiaom.sweet.playtime.database.PlaytimeDatabase;
import top.mrxiaom.sweet.playtime.func.AbstractPluginHolder;
import top.mrxiaom.sweet.playtime.func.RewardManager;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@AutoRegister(requirePlugins = "PlaceholderAPI")
public class Placeholders extends AbstractPluginHolder {
    private Extension extension;
    private TimeFormat timeFormat;
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
        timeFormat = new TimeFormat(config);
    }

    public class Extension extends PlaceholdersExpansion<SweetPlaytime> {
        protected Extension(SweetPlaytime plugin) {
            super(plugin);
        }

        private Long parseAndExecuteQuery(String params, int prefixLength, UUID uuid, AtomicReference<String> error) {
            Query query;
            if (params.length() > prefixLength) {
                List<String> args = CollectionUtils.split(params.substring(prefixLength), ',');
                query = Query.parse(args, error);
            } else {
                query = Query.ALL;
            }
            if (query == null) {
                return null;
            }
            PlaytimeDatabase db = plugin.getPlaytimeDatabase();
            Long seconds = query.collectCurrentPlaytimeWithCache(db, uuid, null);
            if (seconds == null) {
                error.set("DATABASE_ERROR");
                return null;
            }
            return seconds;
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
                return timeFormat.formatSeconds(totalSeconds);
            }
            if (params.startsWith("status_")) {
                List<String> split = CollectionUtils.split(params.substring(7), ':', 2);
                if (split.size() == 2) {
                    RewardSets rewardSets = RewardManager.inst().get(split.get(0));
                    if (rewardSets == null) {
                        return "NOT_FOUND";
                    }
                    String duration = split.get(1);
                    if ("all".equalsIgnoreCase(duration)) {
                        return String.valueOf(rewardSets.checkClaimStatus(player, null));
                    }
                    Long targetDuration = Query.parseSeconds(duration);
                    if (targetDuration == null) {
                        return "WRONG_DURATION_FORMAT";
                    }
                    if (!rewardSets.containsDuration(targetDuration)) {
                        return "NO_DURATION_FOUND";
                    }
                    return String.valueOf(rewardSets.checkClaimStatus(player, targetDuration));
                }
            }
            return super.onPlaceholderRequest(player, params);
        }
    }
}
