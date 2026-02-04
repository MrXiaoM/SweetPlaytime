package top.mrxiaom.sweet.playtime.commands;

import com.google.common.collect.Lists;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.playtime.Messages;
import top.mrxiaom.sweet.playtime.SweetPlaytime;
import top.mrxiaom.sweet.playtime.func.AbstractModule;
import top.mrxiaom.sweet.playtime.func.CleanupManager;

import java.util.*;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    public CommandMain(SweetPlaytime plugin) {
        super(plugin);
        registerCommand("sweetplaytime", this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (sender.isOp()) {
            if (args.length == 1 && "cleanup".equalsIgnoreCase(args[0]) && sender.isOp()) {
                plugin.getScheduler().runTaskAsync(() -> {
                    CleanupManager.inst().cleanup();
                    Messages.Command.cleanup__success.tm(sender);
                });
                return true;
            }
            if (args.length > 0 && "reload".equalsIgnoreCase(args[0]) && sender.isOp()) {
                if (args.length > 1 && "database".equalsIgnoreCase(args[1])) {
                    plugin.options.database().reloadConfig();
                    plugin.options.database().reconnect();
                    return Messages.Command.reload__database.tm(sender);
                }
                plugin.reloadConfig();
                return Messages.Command.reload__success.tm(sender);
            }
            return true;
        }
        return Messages.no_permission.tm(sender);
    }

    private static final List<String> listArg0 = Lists.newArrayList();
    private static final List<String> listOpArg0 = Lists.newArrayList("reload");
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return startsWith(sender.isOp() ? listOpArg0 : listArg0, args[0]);
        }
        return Collections.emptyList();
    }

    public List<String> startsWith(Collection<String> list, String s) {
        return startsWith(null, list, s);
    }
    public List<String> startsWith(String[] addition, Collection<String> list, String s) {
        String s1 = s.toLowerCase();
        List<String> stringList = new ArrayList<>(list);
        if (addition != null) stringList.addAll(0, Lists.newArrayList(addition));
        stringList.removeIf(it -> !it.toLowerCase().startsWith(s1));
        return stringList;
    }
}
