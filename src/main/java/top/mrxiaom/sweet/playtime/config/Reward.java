package top.mrxiaom.sweet.playtime.config;

import org.bukkit.configuration.ConfigurationSection;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;

import java.util.List;

public class Reward {
    private final RewardSets rewardSets;
    private final long durationSeconds;
    private final String display;
    private final List<String> description;
    private final List<IAction> rewardActions;

    public Reward(RewardSets rewardSets, ConfigurationSection config) {
        this.rewardSets = rewardSets;
        String duration = config.getString("duration");
        Long durationSeconds = Query.parseSeconds(duration);
        if (durationSeconds == null) {
            throw new IllegalArgumentException("duration 的值 " + duration + " 无效");
        }
        this.durationSeconds = durationSeconds;
        this.display = config.getString("display", "");
        this.description = config.getStringList("description");
        this.rewardActions = ActionProviders.loadActions(config, "reward-actions");
    }

    public RewardSets getRewardSets() {
        return rewardSets;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public String getDuration(TimeFormat format) {
        return format.formatSeconds(durationSeconds);
    }

    public String getDisplay() {
        return display;
    }

    public List<String> getDescription() {
        return description;
    }

    public List<IAction> getRewardActions() {
        return rewardActions;
    }
}
