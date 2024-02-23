package net.aniby.mswhitelister.discord;

import net.aniby.mswhitelister.MSWhitelister;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.configuration.ConfigurationSection;

public class DiscordBot {
    private final JDA jda;

    public JDA getJda() {
        return jda;
    }

    public TextChannel logChannel;
    public TextChannel formChannel;

    public String playerRole;
    public String declinedRole;

    public DiscordBot(MSWhitelister plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("discord");

        playerRole = section.getString("roles.player");
        declinedRole = section.getString("roles.declined");

        JDABuilder builder = JDABuilder
                .createDefault(
                        section.getString("token")
                )
                .addEventListeners(new DiscordListener())
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .disableCache(
                        CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.ACTIVITY,
                        CacheFlag.CLIENT_STATUS, CacheFlag.CLIENT_STATUS, CacheFlag.STICKER,
                        CacheFlag.FORUM_TAGS, CacheFlag.ONLINE_STATUS
                )
                .setBulkDeleteSplittingEnabled(false)
                .setActivity(Activity.watching(
                        section.getString("rpc")
                ));
        this.jda = builder.build();
    }
}
