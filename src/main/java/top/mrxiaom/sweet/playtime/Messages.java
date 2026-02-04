package top.mrxiaom.sweet.playtime;

import top.mrxiaom.pluginbase.func.language.Language;
import top.mrxiaom.pluginbase.func.language.Message;

import static top.mrxiaom.pluginbase.func.language.LanguageFieldAutoHolder.field;

@Language(prefix="messages.")
public class Messages {
    public static final Message player__only = field("&e该操作只能由玩家执行");
    public static final Message no_permission = field("&c你没有执行该操作的权限");

    @Language(prefix="messages.command.")
    public static class Command {
        public static final Message claim__wrong_duration = field("&e输入的时间格式错误");
        public static final Message claim__no_duration = field("&e这个奖励集合不存在指定的在线奖励");
        public static final Message cleanup__success = field("&c数据清理执行完成");
        public static final Message reload__success = field("&a配置文件已重载");
        public static final Message reload__database = field("&a已重载并重新连接到数据库");
    }
}
