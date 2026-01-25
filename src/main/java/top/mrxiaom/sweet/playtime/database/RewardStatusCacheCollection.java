package top.mrxiaom.sweet.playtime.database;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RewardStatusCacheCollection {
    private final UUID uuid;
    private final Map<String, List<Long>> cacheMap = new HashMap<>();
    public RewardStatusCacheCollection(UUID uuid) {
        this.uuid = uuid;
    }
    public UUID getUuid() {
        return uuid;
    }

    @Nullable
    public List<Long> getCache(String rewardSetsId) {
        return cacheMap.get(rewardSetsId);
    }

    public void removeCache(String rewardSetsId) {
        cacheMap.remove(rewardSetsId);
    }

    public void putCache(String rewardSetsId, List<Long> durationList) {
        cacheMap.put(rewardSetsId, durationList);
    }
}
