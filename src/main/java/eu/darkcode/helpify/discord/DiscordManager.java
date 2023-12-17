package eu.darkcode.helpify.discord;

import eu.darkcode.helpify.objects.Tokens;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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
                .setActivity(Activity.playing("/helpify | matejtomecek.eu/helpify"))
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