package plugin.cache;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PacketLayerCache {
    private static final ConcurrentHashMap<UUID, CacheData> CACHE = new ConcurrentHashMap<>();

    private PacketLayerCache() {
        // Enforce Singleton, cannot instantiate
    }

    public static CacheData get(UUID playerId) {
        return CACHE.get(playerId);
    }

    public static void put(UUID playerId, String worldName) {
        CACHE.put(playerId, new CacheData(playerId, worldName));
    }

    public static CacheData remove(UUID playerId) {
        return CACHE.remove(playerId);
    }
}
