package de.sage.clipy.commands;

import de.sage.clipy.sql.LiteSQL;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RegisterCommand extends ListenerAdapter {

    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("register")) {
            ResultSet rs = LiteSQL.onQuery("SELECT * FROM userData WHERE userID = ?", event.getUser().getIdLong());

            try {
                if (!rs.next()) {

                    boolean autoStart = false;
                    if (event.getOption("autostart") != null)
                        autoStart = event.getOption("autostart").getAsBoolean();

                    StringSelectMenu menu = StringSelectMenu.create("tos:" + event.getMember().getIdLong() + ":" + autoStart).addOptions(
                            SelectOption.of("I agree with the ToS", "agree").withDescription("I read the ToS and agree with it").withEmoji(Emoji.fromFormatted("<:yes:964887955145646141>")),
                            SelectOption.of("I don't agree with the ToS. I dont allow recordings", "disagree").withDescription("I read the ToS and don't agree with it. I dont want to be recorded").withEmoji(Emoji.fromFormatted("<:danger:964658510841409538>"))
                            ).build();

                    event.reply("Please read the ToS and agree with it to use the bot. You can read the ToS here:\n https://sageee.xyz").setActionRow(menu).queue();
                } else
                    event.reply("You are already registered! Use /unregister to stop the functionality of the bot. Use /start or /stop <autostart> to change your autostart setting.").setEphemeral(true).queue();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
