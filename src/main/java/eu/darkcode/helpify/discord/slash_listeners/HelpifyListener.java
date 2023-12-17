package eu.darkcode.helpify.discord.slash_listeners;

import eu.darkcode.helpify.discord.SlashListener;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class HelpifyListener {

    @SlashListener(command="helpify")
    public static void listen(SlashCommandInteractionEvent event){
        event.deferReply(true).queue();
        event.getHook().editOriginal("Working...!").queue();
        System.out.println("Funguje #1");
    }

    @SlashListener(command="helpify")
    public static void listen(){
        System.out.println("Funguje #2");
    }

}
