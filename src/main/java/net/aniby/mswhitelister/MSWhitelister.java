package net.aniby.mswhitelister;

import com.cocahonka.comfywhitelist.api.ComfyWhitelistAPI;
import com.cocahonka.comfywhitelist.api.Storage;
import com.cocahonka.comfywhitelist.api.WhitelistManager;
import net.aniby.mswhitelister.discord.DiscordBot;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MSWhitelister extends JavaPlugin {
    private static MSWhitelister instance;

    public static MSWhitelister getInstance() {
        return instance;
    }


    private static DiscordBot bot;

    public static DiscordBot getBot() {
        return bot;
    }

    private static ComfyWhitelistAPI whitelistApi;
    private static WhitelistManager whitelistManager;
    private static Storage whitelistStorage;

    public static Storage getWhitelistStorage() {
        return whitelistStorage;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        extractWhitelistApi();

        instance = this;

        MSMessages.init(instance);
        bot = new DiscordBot(instance);
    }

    private void extractWhitelistApi() {
        ServicesManager servicesManager = getServer().getServicesManager();
        RegisteredServiceProvider<ComfyWhitelistAPI> provider = servicesManager.getRegistration(ComfyWhitelistAPI.class);

        whitelistApi = provider.getProvider();
        whitelistManager = whitelistApi.getStateManager();
        whitelistStorage = whitelistApi.getStorage();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
