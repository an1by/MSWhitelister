package net.aniby.mswhitelister;

import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DiscordForm {
    private static List<ActionRow> getSubjects(ConfigurationSection section) {
        List<ActionRow> subjects = new ArrayList<>();

        Set<String> keys = section.getKeys(false);
        for (String id : keys) {
            ConfigurationSection subject = section.getConfigurationSection(id);
            TextInputStyle style = TextInputStyle.valueOf(subject.getString("style", "SHORT"));
            String placeholder = subject.getString("placeholder", null);
            String label = subject.getString("label");
            TextInput input = TextInput.create(id, label, style)
                    .setMinLength(1)
                    .setMaxLength(style == TextInputStyle.SHORT ? 100 : 1000)
                    .setPlaceholder(placeholder)
                    .build();
            subjects.add(ActionRow.of(input));
        }

        return subjects;
    }

    public static Modal getModal() {
        ConfigurationSection modalSection = MSWhitelister.getInstance().getConfig().getConfigurationSection("discord.form.modal");
        List<ActionRow> rows = getSubjects(modalSection.getConfigurationSection("questions"));

        return Modal.create("mswl:form", modalSection.getString("label", "Анкета"))
                .addComponents(rows)
                .build();
    }
}
