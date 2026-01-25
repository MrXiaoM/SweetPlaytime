package top.mrxiaom.sweet.playtime;

import com.google.common.collect.Lists;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.depend.PlaceholdersExpansion;
import top.mrxiaom.sweet.playtime.config.Query;
import top.mrxiaom.sweet.playtime.database.PlaytimeDatabase;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class Placeholders extends PlaceholdersExpansion<SweetPlaytime> {
    protected Placeholders(SweetPlaytime plugin) {
        super(plugin);
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("query") || params.startsWith("query_")) {
            int beginIndex = params.indexOf('_');
            Query query;
            if (beginIndex > 2) {
                String[] args = params.substring(beginIndex + 1).split(",");
                AtomicReference<String> error = new AtomicReference<>("UNKNOWN_ERROR");
                query = Query.parse(Lists.newArrayList(args), error);
                if (query == null) {
                    return error.get();
                }
            } else {
                query = Query.ALL;
            }
            PlaytimeDatabase db = plugin.getPlaytimeDatabase();
            UUID uuid = player.getUniqueId();
            Long fromDb = query.collectPlaytimeWithCache(db, uuid);
            if (fromDb == null) {
                return "DATABASE_ERROR";
            }
            return String.valueOf(fromDb + db.getCurrentOnlineSeconds(uuid));
        }
        return super.onPlaceholderRequest(player, params);
    }
}
