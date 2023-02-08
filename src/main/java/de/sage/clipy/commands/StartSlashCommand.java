package de.sage.clipy.commands;

import de.sage.clipy.audio.AudioHandler;
import de.sage.clipy.sql.LiteSQL;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class StartSlashCommand extends ListenerAdapter {

    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("start")) {
            if(event.getMember().getVoiceState() == null) event.deferReply().queue();

            ResultSet rs = LiteSQL.onQuery("SELECT * FROM userData WHERE userID = ?", event.getUser().getIdLong());
            try {
                if(rs.next()){
                    if(rs.getBoolean("tos")){
                        String changed = null;

                        if(event.getOption("start") != null){
                            LiteSQL.onUpdate("UPDATE userData SET autostart = ? WHERE userID = ?", event.getOption("start").getAsBoolean(), event.getUser().getIdLong());
                            changed = " Your autostart setting was changed to " + event.getOption("start").getAsBoolean() + "!";
                        }

                        if(!AudioHandler.isRecording(event.getUser())){
                            HashMap<VoiceChannel, Integer> channels = new HashMap<>();
                            //Check where more people are
                            for (Member member : event.getGuild().getMembers()) {
                                if (member.getVoiceState() != null && member.getVoiceState().getChannel() != null) {
                                    if (AudioHandler.isRecording(member.getUser()) && member.getVoiceState().getChannel().getType().equals(ChannelType.VOICE)) {
                                        channels.put(member.getVoiceState().getChannel().asVoiceChannel(), channels.getOrDefault(member.getVoiceState().getChannel().asVoiceChannel(), 0) + 1);
                                    }
                                }
                            }



                            if (channels.containsKey(event.getMember().getVoiceState().getChannel().asVoiceChannel())) {
                                channels.values().stream().max(Integer::compareTo).ifPresentOrElse(integer -> {
                                    if (integer >= channels.get(event.getMember().getVoiceState().getChannel().asVoiceChannel())) {
                                        event.getGuild().getAudioManager().openAudioConnection(event.getMember().getVoiceState().getChannel().asVoiceChannel());
                                    }else{
                                        event.getMember().getUser().openPrivateChannel().queue(dm -> {
                                            dm.sendMessage("You can't start recording because there are more people using the bot in another channel! I am connected to: " + event.getGuild().getAudioManager().getConnectedChannel().getAsMention()).queue();
                                            return;
                                        });
                                    }
                                }, () -> {
                                    event.getGuild().getAudioManager().openAudioConnection(event.getMember().getVoiceState().getChannel().asVoiceChannel());

                                    AudioHandler handler = new AudioHandler();

                                    event.getGuild().getAudioManager().setReceivingHandler(handler);
                                    event.getGuild().getAudioManager().openAudioConnection(event.getMember().getVoiceState().getChannel().asVoiceChannel());
                                });

                                //Failsafe method
                            } else {
                                event.getGuild().getAudioManager().openAudioConnection(event.getMember().getVoiceState().getChannel().asVoiceChannel());

                                AudioHandler handler = new AudioHandler();

                                event.getGuild().getAudioManager().setReceivingHandler(handler);
                                event.getGuild().getAudioManager().openAudioConnection(event.getMember().getVoiceState().getChannel().asVoiceChannel());
                            }

                            AudioHandler.startRecordingUser(event.getUser());
                            
                            event.reply("Recording started! Use /get to get your audio file or /stop to stop recording!"+ changed).queue();
                        }else
                            event.reply("You are already recording! If you still don't get an audio file, report this to the developer!").setEphemeral(true).queue();
                    }else
                        event.reply("You have not accepted the ToS. Its required to use this bot! Use /register to accept the ToS again.").setEphemeral(true).queue();
                }else
                    event.reply("You are not registered! Please use /register to register!").queue();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}