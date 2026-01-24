package top.mrxiaom.sweet.playtime;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.pluginbase.utils.depend.PlaceholdersExpansion;
import top.mrxiaom.sweet.playtime.database.PlaytimeDatabase;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

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
            String tag = null;
            LocalDate startDate = null, endDate = null;
            if (beginIndex > 2) {
                String[] args = params.substring(beginIndex + 1).split(",");
                for (String arg : args) {
                    String str = arg.trim();
                    if (str.equalsIgnoreCase("today")) {
                        startDate = LocalDate.now();
                        continue;
                    }
                    DayOfWeek dayOfWeek = Util.valueOrNull(DayOfWeek.class, str);
                    if (dayOfWeek != null) {
                        LocalDate now = LocalDate.now();
                        LocalDate monday = now.minusDays(now.getDayOfWeek().getValue() - 1);
                        startDate = monday.plusDays(dayOfWeek.getValue() - 1);
                    }
                    if (!str.contains("=")) continue;
                    int i = str.indexOf('=');
                    String key = str.substring(0, i);
                    String value = str.substring(i + 1);
                    if (key.equals("tag")) {
                        tag = value;
                        continue;
                    }
                    if (key.equals("start")) {
                        try {
                            startDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                        } catch (DateTimeParseException e) {
                            return "WRONG_START_DATE_FORMAT";
                        }
                    }
                    if (key.equals("end")) {
                        try {
                            endDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                        } catch (DateTimeParseException e) {
                            return "WRONG_END_DATE_FORMAT";
                        }
                    }
                }
            }
            PlaytimeDatabase db = plugin.getPlaytimeDatabase();
            UUID uuid = player.getUniqueId();
            Long fromDb = db.collectPlaytimeWithCache(uuid, tag, startDate, endDate);
            if (fromDb == null) {
                return "DATABASE_ERROR";
            }
            return String.valueOf(fromDb + db.getCurrentOnlineSeconds(uuid));
        }
        return super.onPlaceholderRequest(player, params);
    }
}
