package net.aniby.mswhitelister.discord;

import net.aniby.mswhitelister.DiscordForm;
import net.aniby.mswhitelister.MSMessages;
import net.aniby.mswhitelister.MSWhitelister;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DiscordListener extends ListenerAdapter {
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        ConfigurationSection section = MSWhitelister.getInstance().getConfig().getConfigurationSection("discord");
        ConfigurationSection channels = section.getConfigurationSection("channels");

        DiscordBot bot = MSWhitelister.getBot();
        JDA jda = bot.getJda();
        bot.guild = jda.getGuildById(section.getString("guild"));
        bot.formChannel = bot.guild.getTextChannelById(channels.getString("form"));


        MessageEmbed embed = EmbedBuilder.fromData(
                DataObject.fromJson(
                        section.getString("form.embed")
                )
        ).build();


        // Clearing form channel
        try {
            MessageHistory history = new MessageHistory(bot.formChannel);
            List<Message> msgs = history.retrievePast(100).complete();
            if (msgs.size() > 1)
                bot.formChannel.deleteMessages(msgs).queue();
        } catch (Exception exception) {
            MSWhitelister.getInstance().getLogger().info(
                    "\u001B[31mMessages in form channel can't be deleted! Delete it yourself!\u001B[37m"
            );
        }

        // Form channel
        bot.formChannel.sendMessageEmbeds(embed).addActionRow(
                Button.primary("mswl:create_form", section.getString("form.button_label"))
        ).queue();

        // Log channel (form callback for admins)
        bot.logChannel = bot.guild.getTextChannelById(channels.getString("log"));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        Member member = event.getMember();
        if (member == null)
            return;
        DiscordBot bot = MSWhitelister.getBot();

        String customId = event.getComponentId();
        if (customId.equals("mswl:create_form")) {
            Role role = member.getRoles().stream()
                    .filter(r -> r.getId().equals(bot.declinedRole) || r.getId().equals(bot.playerRole))
                    .findFirst().orElse(null);
            if (role != null) {
                event.reply(MSMessages.alreadyWritten).setEphemeral(true).queue();
                return;
            }

            event.replyModal(DiscordForm.getModal()).queue();
        } else if (customId.startsWith("mcwl:log:")) {
            String[] args = customId.split(":");

            boolean accepted = args[2].equals("accept");
            String nickname = args[3];
            String discordId = args[4];

            Member target = bot.guild.getMemberById(discordId);
            if (target == null) {
                event.editMessage(MSMessages.notInGuild).setEmbeds(new ArrayList<>()).setComponents(new ArrayList<>()).queue();
                return;
            }

            String messages = accepted ? MSMessages.accepted : MSMessages.declined;
            messages = messages.replace("\\n", "\n")
                    .replace("%user_id", discordId)
                    .replace("%user_nickname", nickname)
                    .replace("%admin_id", member.getId());
            // Logging
            MSWhitelister.getInstance().getLogger().info(
                    accepted
                            ? "\u001B[36m" + nickname + "\u001B[37m was \u001B[32madded \u001B[37mto server by \u001B[33m(" + member.getEffectiveName() + "/" + member.getId() + ")\u001B[37m"
                            : "\u001B[36m" + nickname + "\u001B[37m form was \u001B[31mdeclined \u001B[37mby \u001B[33m(" + member.getEffectiveName() + "/" + member.getId() + ")\u001B[37m"
            );

            // Add role
            String roleId = accepted ? MSWhitelister.getBot().playerRole : MSWhitelister.getBot().declinedRole;
            Role addRole = bot.guild.getRoleById(roleId);
            bot.guild.addRoleToMember(target, addRole).queue();

            if (accepted) {
                MSWhitelister.getWhitelistStorage().addPlayer(nickname);
                try {
                    target.getUser().openPrivateChannel().queue(privateChannel -> {
                        privateChannel.sendMessage(MSMessages.dmMessage).queue();
                    });
                } catch (Exception ignored) {
                }
            }

            event.editMessage(messages).setEmbeds(new ArrayList<>()).setComponents(new ArrayList<>()).queue();
        }
    }

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {
        if (event.getModalId().equals("mswl:form")) {
            User user = event.getUser();

            FileConfiguration configuration = MSWhitelister.getInstance().getConfig();
            ConfigurationSection section = configuration
                    .getConfigurationSection("discord.form.modal.questions");
            Set<String> ids = section.getKeys(false);

            String logStringEmbed = configuration.getString("discord.log.embed")
                    .replace("%discord_id", user.getId())
                    .replace("\\n", "\n");
            String nickname = null;
            for (String id : ids) {
                String value = event.getValue(id).getAsString();
                if (id.equals("nickname"))
                    nickname = value;

                String label = section.getConfigurationSection(id).getString("label");

                logStringEmbed = logStringEmbed.replace(
                        "%field_" + id + "_label", label
                ).replace(
                        "%field_" + id + "_value", value
                );
            }

            if (nickname == null) {
                event.reply(MSMessages.nicknameError).setEphemeral(true).queue();
                return;
            }

            MessageEmbed embed = EmbedBuilder.fromData(
                    DataObject.fromJson(logStringEmbed)
            ).build();

            MSWhitelister.getBot().logChannel.sendMessageEmbeds(embed).setActionRow(
                    Button.success("mcwl:log:accept:" + nickname + ":" + user.getId(), "Принять"),
                    Button.danger("mcwl:log:decline:" + nickname + ":" + user.getId(), "Отклонить")
            ).queue();

            event.reply(MSMessages.formSent).setEphemeral(true).queue();
        }
    }
}
