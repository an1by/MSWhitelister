package net.aniby.mswhitelister;

import org.bukkit.configuration.ConfigurationSection;

public class MSMessages {
    public static String alreadyWritten;
    public static String formSent;
    public static String nicknameError;
    public static String accepted;
    public static String declined;
    public static String dmMessage;
    public static String notInGuild;
    public static String userNotExists;

    public static void init(MSWhitelister plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("messages");

        alreadyWritten = section.getString("already_written");
        formSent = section.getString("form_sent");
        nicknameError = section.getString("nickname_error");
        accepted = section.getString("accepted");
        declined = section.getString("declined");
        dmMessage = section.getString("dm_message");
        notInGuild = section.getString("not_in_guild");
        userNotExists = section.getString("user_not_exists");
    }
}
