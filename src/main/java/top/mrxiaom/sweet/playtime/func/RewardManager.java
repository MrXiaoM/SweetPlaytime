package top.mrxiaom.sweet.playtime.func;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playtime.SweetPlaytime;
import top.mrxiaom.sweet.playtime.config.RewardSets;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@AutoRegister
public class RewardManager extends AbstractModule {
    private final Map<String, RewardSets> rewards = new HashMap<>();
    public RewardManager(SweetPlaytime plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration pluginConfig) {
        File folder = plugin.resolve(pluginConfig.getString("rewards-folder", "./rewards"));
        if (!folder.exists()) {
            Util.mkdirs(folder);
            plugin.saveResource("rewards/example.yml", new File(folder, "example.yml"));
        }
        rewards.clear();
        Util.reloadFolder(folder, false, (id, file) -> {
            YamlConfiguration config = ConfigUtils.load(file);
            try {
                rewards.put(id, new RewardSets(id, config));
            } catch (Throwable t) {
                warn("[rewards] 加载配置 " + id + " 时出现错误: " + t.getMessage());
            }
        });
        info("[rewards] 加载了 " + rewards.size() + " 个奖励配置");
    }

    public static RewardManager inst() {
        return instanceOf(RewardManager.class);
    }
}
