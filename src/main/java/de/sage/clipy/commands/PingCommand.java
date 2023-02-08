package de.sage.clipy.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class PingCommand extends ListenerAdapter {

    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        long start = System.currentTimeMillis();
        if (event.getName().equals("ping")) {
            event.reply("Pong!").queue((message) -> {
                long end = System.currentTimeMillis();
                message.editOriginal("Pong! " + (end - start) + "ms").queue();
            });
        }
    }
}
