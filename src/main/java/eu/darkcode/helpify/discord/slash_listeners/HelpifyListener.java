package eu.darkcode.helpify.discord.slash_listeners;

import eu.darkcode.helpify.discord.ButtonListener;
import eu.darkcode.helpify.discord.DiscordManager;
import eu.darkcode.helpify.discord.SlashListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HelpifyListener {

    private static final HashMap<Integer, String> helpLists = new HashMap<>();

    static{
        helpLists.put(1, "**HELPIFY**\nCreated by ***96DarkCode96***\n\n**/helpify**\n> Basic info about bot and available commands!\n\n**/modules**\n> Show all modules available at your guild!\n> *GUILD ONLY*");
        helpLists.put(2, "**Module - Voting** | *GUILD ONLY*\n\n**/voting <type>**\n> Enter voting creation mode!\n> *Enters persistent creation mode for voting of your type!*\n> *If you want to start another, first you have to publish or delete current voting that is being created!*\n\n**/resend-voting**\n> Resends deleted voting message!");
    }

    @SlashListener(command="helpify")
    public static void command(SlashCommandInteractionEvent event){
        event.replyEmbeds(generateMessage(1))
                .setEphemeral(true)
                .setActionRow(generateHelpButtons(1, helpLists.size()))
                .queue();
    }

    @ButtonListener(buttonId="helpifyButton1")
    public static void button1(ButtonInteractionEvent event){
        event.deferEdit().queue();
        event.getHook().editOriginalEmbeds(generateMessage(1))
                .setActionRow(generateHelpButtons(1, helpLists.size()))
                .queue();
    }

    @ButtonListener(buttonId="helpifyButton2")
    public static void button2(ButtonInteractionEvent event){
        event.deferEdit().queue();
        int page = Integer.parseInt(event.getMessage().getButtonById("helpifyButton3").getLabel().split(" / ")[0]) - 1;
        event.getHook().editOriginalEmbeds(generateMessage(page))
                .setActionRow(generateHelpButtons(page, helpLists.size()))
                .queue();
    }

    @ButtonListener(buttonId="helpifyButton3")
    public static void button3(ButtonInteractionEvent event){
        event.deferEdit().queue();
        event.getMessage().delete().queue();
    }

    @ButtonListener(buttonId="helpifyButton4")
    public static void button4(ButtonInteractionEvent event){
        event.deferEdit().queue();
        int page = Integer.parseInt(event.getMessage().getButtonById("helpifyButton3").getLabel().split(" / ")[0]) + 1;
        event.getHook().editOriginalEmbeds(generateMessage(page))
                .setActionRow(generateHelpButtons(page, helpLists.size()))
                .queue();
    }

    @ButtonListener(buttonId="helpifyButton5")
    public static void button5(ButtonInteractionEvent event){
        event.deferEdit().queue();
        int page = helpLists.size();
        event.getHook().editOriginalEmbeds(generateMessage(page))
                .setActionRow(generateHelpButtons(page, page))
                .queue();
    }

    private static MessageEmbed generateMessage(int page) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setThumbnail(DiscordManager.getJDA().getSelfUser().getEffectiveAvatarUrl());
        builder.setColor(Color.decode("#FFFFFF"));
        builder.setTitle("Page " + page, "https://helpify.matejtomecek.eu/?ref=helpifyCommand");
        builder.setDescription(helpLists.get(page));
        return builder.build();
    }

    private static List<Button> generateHelpButtons(int page, int maxPages){
        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.secondary("helpifyButton1", "First").withDisabled(page < 3));
        buttons.add(Button.secondary("helpifyButton2", "Previous").withDisabled(page == 1));
        buttons.add(Button.secondary("helpifyButton3", page + " / " + maxPages).asDisabled());
        buttons.add(Button.secondary("helpifyButton4", "Next").withDisabled(page == maxPages));
        buttons.add(Button.secondary("helpifyButton5", "Last").withDisabled(page >= maxPages-1));
        return buttons;
    }

}
