package de.sage.clipy.commands;

import de.sage.clipy.audio.AudioHandler;
import de.sage.clipy.sql.LiteSQL;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class StopCommand extends ListenerAdapter {

    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("stop")) {
            if (AudioHandler.isRecording(event.getUser())) {
                String changed = null;

                if(event.getOption("start") != null){
                    LiteSQL.onUpdate("UPDATE userData SET autostart = ? WHERE userID = ?", event.getOption("start").getAsBoolean(), event.getUser().getIdLong());
                    changed = " Your autostart setting was changed to " + event.getOption("start").getAsBoolean() + "!";
                }

                AudioHandler.stopRecordingUser(event.getUser());
                event.reply("You stopped recording! All your current stored audi data was deleted. Bot will not record you anymore!" + changed).queue();
            } else
                event.reply("You are not recording! Use /start to start recording!").setEphemeral(true).queue();
        }
    }
}
