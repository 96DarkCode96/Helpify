package eu.darkcode.helpify.discord;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.List;

public class GuildManager {

    public static List<SlashCommandData> generateCommands(long guildId) {
        List<SlashCommandData> list = new ArrayList<>();
        //  /modules
        list.add(Commands.slash("modules", "Shows statuses of all modules!")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));
        return list;
    }
}