import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

public class PlayerRepository {
    // Concurrent hashmap for thread-safe caching
    private final Map<String, String> cache = Collections.synchronizedMap(new HashMap<>());

    public String getPlayer(String playerId) {
        // Synchronize accessing the cache
        synchronized (cache) {
            return cache.get(playerId);
        }
    }

    public void addPlayer(String playerId, String playerData) {
        // Synchronize adding to the cache
        synchronized (cache) {
            cache.put(playerId, playerData);
        }
    }

    public void removePlayer(String playerId) {
        // Synchronize removing from the cache
        synchronized (cache) {
            cache.remove(playerId);
        }
    }
}