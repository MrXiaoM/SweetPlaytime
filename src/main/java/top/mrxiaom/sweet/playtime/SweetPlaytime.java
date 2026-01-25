package top.mrxiaom.sweet.playtime;

import org.bukkit.configuration.file.FileConfiguration;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.paper.PaperFactory;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.pluginbase.utils.inventory.InventoryFactory;
import top.mrxiaom.pluginbase.utils.item.ItemEditor;
import top.mrxiaom.pluginbase.utils.scheduler.FoliaLibScheduler;
import top.mrxiaom.pluginbase.utils.ClassLoaderWrapper;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.resolver.DefaultLibraryResolver;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import top.mrxiaom.sweet.playtime.database.PlaytimeDatabase;
import top.mrxiaom.sweet.playtime.database.RewardStatusDatabase;

public class SweetPlaytime extends BukkitPlugin {
    public static SweetPlaytime getInstance() {
        return (SweetPlaytime) BukkitPlugin.getInstance();
    }
    public SweetPlaytime() throws Exception {
        super(options()
                .bungee(false)
                .adventure(true)
                .database(true)
                .reconnectDatabaseWhenReloadConfig(false)
                .scanIgnore("top.mrxiaom.sweet.playtime.libs")
        );
        this.scheduler = new FoliaLibScheduler(this);

        info("正在检查依赖库状态");
        File librariesDir = ClassLoaderWrapper.isSupportLibraryLoader
                ? new File("libraries")
                : new File(this.getDataFolder(), "libraries");
        DefaultLibraryResolver resolver = new DefaultLibraryResolver(getLogger(), librariesDir);

        YamlConfiguration overrideLibraries = ConfigUtils.load(resolve("./.override-libraries.yml"));
        for (String key : overrideLibraries.getKeys(false)) {
            resolver.getStartsReplacer().put(key, overrideLibraries.getString(key));
        }
        resolver.addResolvedLibrary(BuildConstants.RESOLVED_LIBRARIES);

        List<URL> libraries = resolver.doResolve();
        info("正在添加 " + libraries.size() + " 个依赖库到类加载器");
        for (URL library : libraries) {
            this.classLoader.addURL(library);
        }
    }

    @Override
    public @NotNull ItemEditor initItemEditor() {
        return PaperFactory.createItemEditor();
    }

    @Override
    public @NotNull InventoryFactory initInventoryFactory() {
        return PaperFactory.createInventoryFactory();
    }

    private PlaytimeDatabase playtimeDatabase;
    private RewardStatusDatabase rewardStatusDatabase;
    public PlaytimeDatabase getPlaytimeDatabase() {
        return playtimeDatabase;
    }
    public RewardStatusDatabase getRewardStatusDatabase() {
        return rewardStatusDatabase;
    }

    private String tag = "default";
    public String tag() {
        return tag;
    }

    @Override
    protected void beforeEnable() {
        options.registerDatabase(
                this.playtimeDatabase = new PlaytimeDatabase(this),
                this.rewardStatusDatabase = new RewardStatusDatabase(this)
        );
    }

    @Override
    protected void beforeReloadConfig(FileConfiguration config) {
        this.tag = config.getString("tag", "default");
    }

    @Override
    protected void afterEnable() {
        if (PAPI.isEnabled()) {
            new Placeholders(this).register();
        }
        getLogger().info("SweetPlaytime 加载完毕");
    }
}
