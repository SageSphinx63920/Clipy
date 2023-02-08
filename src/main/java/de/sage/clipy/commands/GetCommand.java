package de.sage.clipy.commands;

import de.sage.clipy.audio.AudioHandler;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;

public class GetCommand extends ListenerAdapter {

    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("get")) {
            User user = event.getUser();
            int offset = 0;

            if(event.getOption("user") != null)
                user = event.getOption("user").getAsUser();

            if(event.getOption("offset") != null)
                offset = event.getOption("offset").getAsInt();


            if (AudioHandler.isRecording(user)) {
                File file = AudioHandler.getWavFile(AudioHandler.getDataDir(event.getGuild(), user), user, offset);
                event.reply("Here is the audio file form " + user.getAsTag() + " with an offset of " + offset + " seconds!").addFiles(FileUpload.fromData(file)).queue(success -> {
                    file.delete();
                });
            }else
                event.reply("The user your are trying to get the audio form is not recorded! Use /start to start recording!").setEphemeral(true).queue();
        }
    }
}
