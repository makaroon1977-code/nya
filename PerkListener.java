import java.util.HashMap;
import java.util.Map;

public class PerkListener {

    private Map<String, Long> cooldowns = new HashMap<>();

    public void activatePerk(String playerId) {
        if (isOnCooldown(playerId)) {
            System.out.println("Player " + playerId + " is on cooldown!");
            return;
        }

        // Activate perk logic here
        System.out.println("Activating perk for player " + playerId);

        // Set cooldown for the player
        setCooldown(playerId);
    }

    private boolean isOnCooldown(String playerId) {
        return cooldowns.containsKey(playerId) && 
               (System.currentTimeMillis() - cooldowns.get(playerId) < getCooldownTime());
    }

    private void setCooldown(String playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    private long getCooldownTime() {
        // TODO: Define the cooldown duration in milliseconds
        return 60000; // Example: 60 seconds
    }

    // TODO: Implement additional features such as user notifications or perk enhancements
}