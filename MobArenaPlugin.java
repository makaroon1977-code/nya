import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class MobArenaPlugin extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger(MobArenaPlugin.class.getName());

    @Override
    public void onEnable() {
        LOGGER.info("MobArenaPlugin is starting...");
        try {
            // Initialize your plugin (load configurations, register commands/events)
            initialize();
        } catch (Exception e) {
            LOGGER.severe("Error enabling MobArenaPlugin: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void initialize() {
        // Load configuration, set up commands and events
        LOGGER.info("MobArenaPlugin initialized.");
        // Example: getConfig().options().copyDefaults(true);
        // saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        LOGGER.info("MobArenaPlugin is stopping...");
        // Clean up resources
    }

    // Additional functionality with proper error handling
    private void exampleAction() {
        try {
            // Perform an action
        } catch (SpecificException e) {
            LOGGER.warning("Specific error occurred: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.severe("An unexpected error occurred: " + e.getMessage());
        }
    }
}