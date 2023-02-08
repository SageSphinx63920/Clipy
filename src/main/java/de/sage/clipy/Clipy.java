package de.sage.clipy;

import club.minnced.opus.util.OpusLibrary;
import de.sage.clipy.commands.*;
import de.sage.clipy.events.ToSMenuEvent;
import de.sage.clipy.events.UserVoiceEvent;
import de.sage.clipy.sql.LiteSQL;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author SageSphinx63920
 * <p>
 * Copyright (c) 2019 - 2023 by SageSphinx63920 to present. All rights reserved
 */
public class Clipy {

    private static @Getter Clipy instance;
    private static JDABuilder builder;
    private static @Getter JDA jda;

    public static void main(String[] args) throws IOException {
        instance = new Clipy();

        LiteSQL.connect();

        //We need this because Opus isn't included by default on macOS. Use `bre install opus` to install it.
       // if (System.getProperty("os.name").contains("Mac OS")) {
            System.setProperty("jna.tmpdir", "./tmp");
            System.setProperty("java.io.tmpdir", "./tmp");
            OpusLibrary.loadFrom("/opt/homebrew/lib/libopus.dylib");
      //  }

        builder = JDABuilder.createDefault(Token.token);

        //Caching and stuff
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.enableIntents(Arrays.asList(GatewayIntent.values()));
        builder.enableCache(Arrays.asList(CacheFlag.values()));

        //Set Bot Start Status
        builder.setActivity(Activity.listening("your voice <3"));
        builder.setStatus(OnlineStatus.ONLINE);

        //Intern JDA Settings
        builder.setAutoReconnect(true);

        // Event Listener
        builder.addEventListeners(new UserVoiceEvent());
        builder.addEventListeners(new ToSMenuEvent());

        //Command
        builder.addEventListeners(new RegisterCommand());
        builder.addEventListeners(new PingCommand());
        builder.addEventListeners(new StartSlashCommand());
        //builder.addEventListeners(new GetCommand());
        builder.addEventListeners(new UnregisterCommand());
        builder.addEventListeners(new StopCommand());

        //Start JDA with Shards
        try {
            jda = builder.build();
            System.out.println("Clipy started!\n");

            registerSlashCommands();
        } catch (Exception e) {
            System.out.println("Clipy failed to start! Ex: " + e.getMessage());
        }

    }

    public static void registerSlashCommands() {
        CommandListUpdateAction clua = jda.updateCommands();

        // User Menu
        clua.addCommands(Commands.user("Get Clip"));

        //Slash Commands
        clua.addCommands(Commands.slash("ping", "Pong!"));
        clua.addCommands(Commands.slash("register", "Enable the bot to record your voice")
                .addOption(OptionType.BOOLEAN, "auto", "Automatically record your voice when you join a channel", false));
        clua.addCommands(Commands.slash("unregister", "Disable the bot to record your voice"));
        clua.addCommands(Commands.slash("start", "Start recording your voice").addOption(OptionType.BOOLEAN, "auto", "Automatically record your voice when you join a channel", false));
        clua.addCommands(Commands.slash("stop", "Stop recording your voice").addOption(OptionType.BOOLEAN, "auto", "Automatically record your voice when you join a channel", false));
        clua.addCommands(Commands.slash("delete", "Delete your recorded voice. Doesn't stop recording"));
        clua.addCommands(Commands.slash("get", "Get your recorded voice").addOptions(
                new OptionData(OptionType.USER, "user", "The user you want to get the voice from", false),
                new OptionData(OptionType.INTEGER, "offset", "Seconds of offset from the start if the file", false)
        ));

        clua.queue(null, null);
    }
}