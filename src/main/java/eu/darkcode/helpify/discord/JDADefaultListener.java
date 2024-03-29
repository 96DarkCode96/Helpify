package eu.darkcode.helpify.discord;

import eu.darkcode.helpify.discord.annotations.ButtonListener;
import eu.darkcode.helpify.discord.annotations.ModalListener;
import eu.darkcode.helpify.discord.annotations.SlashListener;
import eu.darkcode.helpify.discord.annotations.StringSelectListener;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class JDADefaultListener extends ListenerAdapter {

    private static final HashMap<String, List<Method>> slashListeners = new HashMap<>();
    private static final HashMap<String, List<Method>> buttonListeners = new HashMap<>();
    private static final HashMap<String, List<Method>> modalListeners = new HashMap<>();
    private static final HashMap<String, List<Method>> stringSelectListeners = new HashMap<>();

    static{
        try {
            ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
            provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
            Set<BeanDefinition> classes = provider.findCandidateComponents("eu.darkcode.helpify");
            for (BeanDefinition bean : classes) {
                Class<?> clazz = Class.forName(bean.getBeanClassName());
                for (Method method : clazz.getMethods()) {
                    for (ButtonListener listener : method.getAnnotationsByType(ButtonListener.class))
                        buttonListeners.computeIfAbsent(listener.buttonId(), s -> new ArrayList<>()).add(method);
                    for (SlashListener listener : method.getAnnotationsByType(SlashListener.class))
                        slashListeners.computeIfAbsent(listener.command(), s -> new ArrayList<>()).add(method);
                    for (ModalListener listener : method.getAnnotationsByType(ModalListener.class))
                        modalListeners.computeIfAbsent(listener.modalId(), s -> new ArrayList<>()).add(method);
                    for (StringSelectListener listener : method.getAnnotationsByType(StringSelectListener.class))
                        stringSelectListeners.computeIfAbsent(listener.componentId(), s -> new ArrayList<>()).add(method);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        Guild guild = event.getGuild();
        guild.updateCommands().addCommands(GuildManager.generateCommands(guild.getIdLong())).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        List<Method> methodList = slashListeners.get(event.getName());
        if(methodList == null)
            return;
        methodList.forEach(method -> execute(method, event));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        List<Method> methodList = buttonListeners.get(event.getButton().getId());
        if(methodList == null)
            return;
        methodList.forEach(method -> execute(method, event));
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        List<Method> methodList = modalListeners.get(event.getModalId());
        if(methodList == null)
            return;
        methodList.forEach(method -> execute(method, event));
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        List<Method> methodList = stringSelectListeners.get(event.getComponentId());
        if(methodList == null)
            return;
        methodList.forEach(method -> execute(method, event));
    }

    private void execute(Method method, Object event) {
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