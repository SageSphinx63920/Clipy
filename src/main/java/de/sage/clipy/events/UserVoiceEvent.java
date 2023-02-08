package de.sage.clipy.events;

import de.sage.clipy.audio.AudioHandler;
import de.sage.clipy.sql.LiteSQL;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class UserVoiceEvent extends ListenerAdapter {

    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelLeft() == null && event.getChannelJoined() != null && !event.getMember().getUser().isBot()) {
            ResultSet rs = LiteSQL.onQuery("SELECT * FROM userData WHERE userID = " + event.getMember().getIdLong());

            try {
                if (rs.next()) {
                    if (rs.getBoolean("autoStart")) {

                        HashMap<VoiceChannel, Integer> channels = new HashMap<>();
                        //Check where more people are
                        for (Member member : event.getGuild().getMembers()) {
                            if (member.getVoiceState() != null && member.getVoiceState().getChannel() != null) {
                                if (AudioHandler.isRecording(member.getUser()) && member.getVoiceState().getChannel().getType().equals(ChannelType.VOICE)) {
                                    channels.put(member.getVoiceState().getChannel().asVoiceChannel(), channels.getOrDefault(member.getVoiceState().getChannel().asVoiceChannel(), 0) + 1);
                                }
                            }
                        }

                        if (channels.containsKey(event.getChannelJoined().asVoiceChannel())) {
                            channels.values().stream().max(Integer::compareTo).ifPresentOrElse(integer -> {
                                if (integer >= channels.get(event.getChannelJoined().asVoiceChannel())) {
                                    event.getGuild().getAudioManager().openAudioConnection(event.getChannelJoined().asVoiceChannel());
                                }else{
                                    event.getMember().getUser().openPrivateChannel().queue(dm -> {
                                        dm.sendMessage("You can't start recording because there are more people using the bot in another channel! I am connected to: " + event.getGuild().getAudioManager().getConnectedChannel().getAsMention()).queue();
                                        return;
                                    });
                                }
                            }, () -> {
                                event.getGuild().getAudioManager().openAudioConnection(event.getChannelJoined().asVoiceChannel());

                                AudioHandler handler = new AudioHandler();

                                event.getGuild().getAudioManager().setReceivingHandler(handler);
                                event.getGuild().getAudioManager().openAudioConnection(event.getChannelJoined().asVoiceChannel());
                            });

                            //Failsafe method
                        } else {
                            event.getGuild().getAudioManager().openAudioConnection(event.getChannelJoined().asVoiceChannel());

                            AudioHandler handler = new AudioHandler();

                            event.getGuild().getAudioManager().setReceivingHandler(handler);
                            event.getGuild().getAudioManager().openAudioConnection(event.getChannelJoined().asVoiceChannel());
                        }

                        AudioHandler.startRecordingUser(event.getMember().getUser());
                    }
                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

          /*  event.getGuild().getAudioManager().openAudioConnection(event.getChannelJoined());

            Member member = event.getMember();

            AudioHandler handler = new AudioHandler();
            event.getGuild().getAudioManager().setReceivingHandler(handler);

            new ScheduledThreadPoolExecutor(1).schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        File dataDir =  Path.of("data", event.getGuild().getId(), "audio").toFile();
                        dataDir.mkdirs();

                        File clip = handler.getWavFile(new File(dataDir, member.getId()), event.getMember().getUser(), 0);
                        if(clip != null){
                            //event.getGuild().getTextChannelsByName("clips", true).get(0).sendFile(clip).queue();
                        }



                        event.getGuild().getAudioManager().setReceivingHandler(null);
                        event.getGuild().getAudioManager().closeAudioConnection();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 15, TimeUnit.SECONDS);*/
        }
    }
}
