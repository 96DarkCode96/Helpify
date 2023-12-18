package eu.darkcode.helpify.discord;

import eu.darkcode.helpify.objects.Tokens;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.ArrayList;
import java.util.List;

public class DiscordManager {

    private static JDA jda;

    public static void init(){
        jda = JDABuilder.createDefault(Tokens.DISCORD_TOKEN)
                .enableIntents(GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS)
                .enableCache(CacheFlag.VOICE_STATE)
                .setAutoReconnect(true)
                .addEventListeners(new JDADefaultListener())
                .setActivity(Activity.playing("/helpify | helpify.matejtomecek.eu"))
                .build();
        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        jda.updateCommands().addCommands(generateGlobalCommands()).queue();

    }

    private static List<SlashCommandData> generateGlobalCommands() {
        List<SlashCommandData> list = new ArrayList<>();
        list.add(Commands.slash("helpify", "Displays list of all available commands!"));
        return list;
    }

    public static JDA getJDA() {
        if(jda == null)
            throw new NullPointerException("JDA is not initialized!");
        return jda;
    }
}