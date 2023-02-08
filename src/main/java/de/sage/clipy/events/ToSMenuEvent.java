package de.sage.clipy.events;

import de.sage.clipy.sql.LiteSQL;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ToSMenuEvent extends ListenerAdapter {

    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().startsWith("tos:")) {
            String[] args = event.getComponentId().split(":");
            long userID = Long.parseLong(args[1]);
            if(userID == event.getUser().getIdLong()){
                boolean autoStart = Boolean.parseBoolean(args[2]);
                if (event.getSelectedOptions().get(0).getValue().equals("agree")) {
                    LiteSQL.onUpdate("INSERT INTO userData (userID, autoStart, tos) VALUES (?, ?, ?)", userID, autoStart, true);
                    event.reply("You are now registered! Autorecord on join status: **" + (autoStart?"on":"off") + "**").queue();

                    event.getInteraction().editSelectMenu(event.getSelectMenu().asDisabled()).queue();
                } else {
                    event.reply("You declined the ToS! **We do not record you!** Use /register again to may agree again.").setEphemeral(true).queue();
                }
            }else
                event.reply("You are not allowed to use this menu!").setEphemeral(true).queue();

        }
    }
}