package top.mrxiaom.sweet.playtime.func;

import top.mrxiaom.sweet.playtime.SweetPlaytime;

@SuppressWarnings({"unused"})
public abstract class AbstractPluginHolder extends top.mrxiaom.pluginbase.func.AbstractPluginHolder<SweetPlaytime> {
    public AbstractPluginHolder(SweetPlaytime plugin) {
        super(plugin);
    }

    public AbstractPluginHolder(SweetPlaytime plugin, boolean register) {
        super(plugin, register);
    }
}
