package eu.darkcode.helpify.discord;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class JDADefaultListener extends ListenerAdapter {

    private static final HashMap<String, List<Method>> methods = new HashMap<>();

    static{
        try {
            ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
            provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
            Set<BeanDefinition> classes = provider.findCandidateComponents("eu.darkcode.helpify");
            for (BeanDefinition bean : classes) {
                Class<?> clazz = Class.forName(bean.getBeanClassName());
                for (Method method : clazz.getMethods()) {
                    SlashListener annotation = AnnotationUtils.getAnnotation(method, SlashListener.class);
                    if(annotation == null)
                        continue;
                    methods.computeIfAbsent(annotation.command(), s -> new ArrayList<>()).add(method);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGenericEvent(GenericEvent event) {
        super.onGenericEvent(event);
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        Guild guild = event.getGuild();
        guild.updateCommands().addCommands(GuildManager.generateCommands(guild.getIdLong())).queue();
        super.onGuildReady(event);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        List<Method> methodList = methods.get(event.getName());
        if(methodList == null)
            return;
        methodList.forEach(method -> execute(method, event));
    }

    private void execute(Method method, SlashCommandInteractionEvent event) {
        try {
            method.setAccessible(true);
            if(method.getParameterCount() == 0)
                method.invoke(null);
            else
                method.invoke(null, event);
            method.setAccessible(false);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}