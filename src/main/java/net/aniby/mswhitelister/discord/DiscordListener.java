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
import java.util.Set;

public class DiscordListener extends ListenerAdapter {
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        ConfigurationSection section = MSWhitelister.getInstance().getConfig().getConfigurationSection("discord");
        ConfigurationSection channels = section.getConfigurationSection("channels");

        DiscordBot bot = MSWhitelister.getBot();
        JDA jda = bot.getJda();
        bot.formChannel = jda.getTextChannelById(channels.getString("form"));


        MessageEmbed embed = EmbedBuilder.fromData(
                DataObject.fromJson(
                        section.getString("form.embed")
                                .replace("\\n", "\n")
                )
        ).build();


        bot.formChannel.sendMessageEmbeds(embed).addActionRow(
                Button.primary("mswl:create_form", section.getString("form.button_label"))
        ).queue();

        bot.logChannel = jda.getTextChannelById(channels.getString("log"));
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
                event.reply(MSMessages.alreadyWritten).queue();
                return;
            }

            event.replyModal(DiscordForm.getModal()).queue();
        } else if (customId.startsWith("mcwl:log:")) {
            String[] args = customId.split(":");

            boolean accepted = args[2].equals("accept");
            String nickname = args[3];
            String discordId = args[4];

            Guild guild = event.getGuild();
            Member target = guild.getMemberById(discordId);
            if (target == null) {
                event.reply(MSMessages.notInGuild).queue();
                return;
            }

            String messages = accepted ? MSMessages.accepted : MSMessages.declined;
            messages = messages.replace("\\n", "\n")
                    .replace("%user_id", discordId)
                    .replace("%user_nickname", nickname)
                    .replace("%admin_id", member.getId());

            Role addRole = guild.getRoleById(accepted ? MSWhitelister.getBot().playerRole : MSWhitelister.getBot().declinedRole);
            target.getRoles().add(addRole);

            if (accepted) {
                MSWhitelister.getWhitelistStorage().addPlayer(nickname);
                try {
                    target.getUser().openPrivateChannel().queue(privateChannel -> {
                        privateChannel.sendMessage(MSMessages.dmMessage).queue();
                    });
                } catch (Exception ignored) {}
            }

            event.editMessage(messages).setComponents(new ArrayList<>()).queue();
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
                event.reply(MSMessages.nicknameError).queue();
                return;
            }

            MessageEmbed embed = EmbedBuilder.fromData(
                    DataObject.fromJson(logStringEmbed)
            ).build();

            MSWhitelister.getBot().logChannel.sendMessageEmbeds(embed).setActionRow(
                    Button.success("mcwl:log:accept:" + nickname + ":" + user.getId(), "Принять"),
                    Button.danger("mcwl:log:decline:" + nickname + ":" + user.getId(), "Отклонить")
            ).queue();

            event.reply(MSMessages.formSent).queue();
        }
    }
}
