package top.mrxiaom.sweet.playtime.config;

import org.bukkit.configuration.ConfigurationSection;

public class TimeFormat {
    private final String formatSecond, formatSeconds, formatMinute, formatMinutes;
    private final String formatHour, formatHours, formatDay, formatDays;
    public TimeFormat(ConfigurationSection config) {
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
}
