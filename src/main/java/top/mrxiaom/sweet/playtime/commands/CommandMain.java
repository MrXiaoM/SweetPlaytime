package top.mrxiaom.sweet.playtime.commands;

import com.google.common.collect.Lists;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.playtime.Messages;
import top.mrxiaom.sweet.playtime.SweetPlaytime;
import top.mrxiaom.sweet.playtime.config.Query;
import top.mrxiaom.sweet.playtime.config.RewardSets;
import top.mrxiaom.sweet.playtime.func.AbstractModule;
import top.mrxiaom.sweet.playtime.func.CleanupManager;
import top.mrxiaom.sweet.playtime.func.RewardManager;

import java.util.*;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    public CommandMain(SweetPlaytime plugin) {
        super(plugin);
        registerCommand("sweetplaytime", this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 3 && "claim".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player)) {
                return Messages.player__only.tm(sender);
            }
            Player player = (Player) sender;
            RewardSets rewardSets = RewardManager.inst().get(args[1]);
            if ("all".equalsIgnoreCase(args[2])) {
                plugin.getScheduler().runTaskAsync(() -> {
                    boolean result = rewardSets.doClaimAndSubmit(player);
                    List<IAction> list = result ? rewardSets.getClaimAllSuccess() : rewardSets.getClaimAllFailed();
                    if (!list.isEmpty()) {
                        plugin.getScheduler().runTask(() -> ActionProviders.run(plugin, player, list));
                    }
                });
                return true;
            }
            Long targetDuration = Query.parseSeconds(args[2]);
            if (targetDuration == null) {
                return Messages.Command.claim__wrong_duration.tm(player);
            }
            if (!rewardSets.containsDuration(targetDuration)) {
                return Messages.Command.claim__no_duration.tm(player);
            }
            plugin.getScheduler().runTaskAsync(() -> {
                boolean result = rewardSets.doClaimAndSubmit(player, targetDuration);
                List<IAction> list = result ? rewardSets.getClaimSingleSuccess() : rewardSets.getClaimSingleFailed();
                if (!list.isEmpty()) {
                    plugin.getScheduler().runTask(() -> ActionProviders.run(plugin, player, list));
                }
            });
            return true;
        }
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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            if (sender instanceof Player) {
                list.add("claim");
            }
            if (sender.isOp()) {
                list.add("cleanup");
                list.add("reload");
            }
            return startsWith(list, args[0]);
        }
        if (args.length == 2) {
            if (sender instanceof Player) {
                if ("claim".equalsIgnoreCase(args[0])) {
                    return startsWith(RewardManager.inst().keys(sender), args[1]);
                }
            }
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
