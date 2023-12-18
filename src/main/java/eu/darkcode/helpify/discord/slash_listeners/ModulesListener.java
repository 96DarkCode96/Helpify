package eu.darkcode.helpify.discord.slash_listeners;

import eu.darkcode.helpify.database.Database;
import eu.darkcode.helpify.discord.SlashListener;
import eu.darkcode.helpify.discord.modules.Module;
import eu.darkcode.helpify.discord.modules.ModuleStatus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.HashMap;

public class ModulesListener {

    @SlashListener(command = "modules")
    public static void listen(SlashCommandInteractionEvent event){
        if(!event.isFromGuild())
            return;
        EmbedBuilder b = new EmbedBuilder();
        b.setTitle("Modules");
        b.setColor(Color.white);
        assert event.getGuild() != null;
        HashMap<Module, ModuleStatus> modules = Database.fetchModules(event.getGuild().getIdLong());
        modules.forEach((module, moduleStatus) -> b.addField(module.getMessage(), "> " + moduleStatus.getMessage(), false));
        event.replyEmbeds(b.build())
                .setEphemeral(true)
                .setActionRow(Button.link("https://helpify.matejtomecek.eu/dashboard/", "DASHBOARD"))
                .queue();
    }

}